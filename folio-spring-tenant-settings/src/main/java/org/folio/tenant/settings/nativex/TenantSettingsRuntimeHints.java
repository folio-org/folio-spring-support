package org.folio.tenant.settings.nativex;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native-image reachability hints for {@code folio-spring-tenant-settings}.
 *
 * <p>Wired into the context via {@code @ImportRuntimeHints} on
 * {@link org.folio.tenant.settings.config.TenantSettingsConfig} (an auto-configuration entry point).</p>
 */
public class TenantSettingsRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    // Liquibase loads this changelog from the classpath (consumers include it via
    // <include file="db/tenant-settings/changelog.xml"/>); native-image does not enumerate the
    // classpath at runtime, so it must be declared. The file is self-contained (no nested <include>),
    // so registering the single path is sufficient.
    hints.resources().registerPattern("db/tenant-settings/changelog.xml");

    // Entity reflection (SettingEntity, its nested SettingType enum, SettingGroupEntity) is contributed
    // automatically by Hibernate's AOT processing when the consuming application builds its persistence
    // unit (entities are picked up via @EntityScan). Per the migration plan we rely on that and add an
    // explicit entity registrar here only if a native smoke test reports the metadata as missing.
  }
}
