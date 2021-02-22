package org.folio.spring.config;

import lombok.RequiredArgsConstructor;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.repository.SystemUserRepository;
import org.folio.spring.repository.impl.DbSystemUserRepository;
import org.folio.spring.service.SystemUserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(prefix = "application.system-user",
  name = {"username", "password", "permissions-file-path"})
@EnableConfigurationProperties(FolioSystemUserProperties.class)
@EnableFeignClients(clients = {AuthnClient.class, PermissionsClient.class, UsersClient.class})
@RequiredArgsConstructor
public class FolioSystemUserConfig {
  private final FolioSystemUserProperties systemUserConf;

  @Bean
  public SystemUserRepository systemUserRepository(
    JdbcTemplate jdbcTemplate, FolioModuleMetadata moduleMetadata) {

    return new DbSystemUserRepository(jdbcTemplate, moduleMetadata);
  }

  @Bean
  public SystemUserService systemUserService(
    PermissionsClient permissionsClient, UsersClient usersClient, AuthnClient authnClient,
    SystemUserRepository repository, FolioExecutionContext context) {

    return new SystemUserService(permissionsClient, usersClient,
      authnClient, repository, systemUserConf, context);
  }
}
