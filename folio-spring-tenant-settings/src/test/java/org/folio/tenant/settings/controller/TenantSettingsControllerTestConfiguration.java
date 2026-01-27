package org.folio.tenant.settings.controller;

import org.folio.tenant.settings.config.TenantSettingsProperties;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@ComponentScan(basePackages = "org.folio.tenant.settings")
@EnableConfigurationProperties(TenantSettingsProperties.class)
public class TenantSettingsControllerTestConfiguration {
}
