package org.folio.tenant.settings.config;

import org.folio.tenant.settings.nativex.TenantSettingsRuntimeHints;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration class for the tenant settings module.
 * Enables JPA repositories, entity scanning, component scanning, and configuration properties.
 */
@EnableJpaRepositories(basePackages = "org.folio.tenant.settings.repository")
@EntityScan(basePackages = "org.folio")
@ComponentScan("org.folio.tenant.settings")
@EnableConfigurationProperties(TenantSettingsProperties.class)
@ImportRuntimeHints(TenantSettingsRuntimeHints.class)
@RegisterReflectionForBinding({
  org.folio.tenant.domain.dto.Setting.class,
  org.folio.tenant.domain.dto.SettingCollection.class,
  org.folio.tenant.domain.dto.SettingGroup.class,
  org.folio.tenant.domain.dto.SettingGroupCollection.class,
  org.folio.tenant.domain.dto.SettingUpdateRequest.class,
  org.folio.tenant.domain.dto.Metadata.class})
public class TenantSettingsConfig {
}
