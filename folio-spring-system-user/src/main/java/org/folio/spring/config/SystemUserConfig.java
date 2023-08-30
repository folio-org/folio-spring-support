package org.folio.spring.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.folio.spring.model.SystemUser;
import org.folio.spring.service.SystemUserProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.spring.client",
  "org.folio.spring.context",
  "org.folio.spring.service",
  "org.folio.spring.config.properties",
})
@EnableFeignClients(basePackages = "org.folio.spring.client")
@ConditionalOnProperty(prefix = "folio.system-user", name = "username")
@EnableConfigurationProperties({SystemUserProperties.class})
public class SystemUserConfig {

  @Bean
  @ConditionalOnClass({Caffeine.class, CaffeineCacheManager.class})
  public Cache<String, SystemUser> systemUserCache() {
    return Caffeine.from("maximumSize=500,expireAfterWrite=3600s").build();
  }
}
