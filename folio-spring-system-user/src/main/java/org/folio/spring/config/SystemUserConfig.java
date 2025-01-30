package org.folio.spring.config;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Log4j2
@Configuration
@ComponentScan(basePackages = {
  "org.folio.spring.client",
  "org.folio.spring.context",
  "org.folio.spring.service",
  "org.folio.spring.config.properties",
})
@EnableConfigurationProperties({SystemUserProperties.class})
public class SystemUserConfig {

  @Bean
  @Lazy
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public PrepareSystemUserService fallbackSystemUserService(FolioExecutionContext executionContext,
      SystemUserProperties systemUserProperties) {
    log.warn("Fallback system user service is being initialized; the system user is disabled.");
    return new PrepareSystemUserService(executionContext, systemUserProperties, null);
  }
}
