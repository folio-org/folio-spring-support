package org.folio.spring.config;

import org.folio.spring.service.SystemUserProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
  "org.folio.spring.client",
  "org.folio.spring.context",
  "org.folio.spring.service",
  "org.folio.spring.config.properties",
})
@EnableConfigurationProperties({SystemUserProperties.class})
public class SystemUserConfig {}
