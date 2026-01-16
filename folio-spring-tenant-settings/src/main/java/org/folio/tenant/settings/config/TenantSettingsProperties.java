package org.folio.tenant.settings.config;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the tenant settings module.
 * Binds properties with the prefix "folio.tenant.settings".
 */
@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ConfigurationProperties(prefix = "folio.tenant.settings")
public class TenantSettingsProperties {

  /**
   * Defines tenant settings domain.
   */
  @NonNull
  @Pattern(regexp = "[a-z][a-z-]+[a-z]", message = "Domain must follow the pattern: '[a-z][a-z-]+[a-z]'")
  private String domain;

  /**
   * Defines if the permission check is enabled.
   */
  private boolean permissionCheckEnabled = true;
}
