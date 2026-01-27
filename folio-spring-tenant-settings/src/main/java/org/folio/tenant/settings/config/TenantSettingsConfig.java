package org.folio.tenant.settings.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration class for the tenant settings module.
 * Enables JPA repositories, entity scanning, component scanning, and configuration properties.
 */
@EnableJpaRepositories(basePackages = "org.folio.tenant.settings.repository")
@EntityScan(basePackages = "org.folio")
@ComponentScan("org.folio.tenant.settings")
@EnableConfigurationProperties(TenantSettingsProperties.class)
public class TenantSettingsConfig {
}
