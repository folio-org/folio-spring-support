package org.folio.spring.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.model.SystemUser;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserProperties;
import org.folio.spring.service.SystemUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@ConditionalOnProperty(name = "folio.system-user.enabled", havingValue = "true", matchIfMissing = true)
public class OptionalSystemUserConfig {

  @Bean
  @ConditionalOnClass({Caffeine.class, CaffeineCacheManager.class})
  public Cache<String, SystemUser> systemUserCache() {
    return Caffeine.from("maximumSize=500,expireAfterWrite=3600s").build();
  }

  @Bean
  public AuthnClient authnClient(HttpServiceProxyFactory factory) {
    return factory.createClient(AuthnClient.class);
  }

  @Bean
  public UsersClient usersClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UsersClient.class);
  }

  @Bean
  public PermissionsClient permissionsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(PermissionsClient.class);
  }

  @Bean
  public SystemUserService systemUserService(ExecutionContextBuilder executionContextBuilder,
      SystemUserProperties systemUserProperties, FolioEnvironment folioEnvironment,
      AuthnClient authnClient, UsersClient usersClient) {
    return new SystemUserService(executionContextBuilder, systemUserProperties, folioEnvironment,
      authnClient, usersClient);
  }

  @Bean
  public PrepareSystemUserService prepareSystemUserService(FolioExecutionContext executionContext,
      SystemUserProperties systemUserProperties, SystemUserService systemUserService) {
    return new PrepareSystemUserService(executionContext, systemUserProperties, systemUserService);
  }
}
