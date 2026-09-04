package org.folio.spring;

import java.util.Optional;

/**
 * Provides module metadata.
 */
public interface FolioModuleMetadata {

  /**
   * Retrieves the name of the module.
   *
   * @return The name of the module.
   */
  String getModuleName();

  /**
   * Retrieves the version of the module.
   *
   * @return The version of the module, or empty when it is not available.
   */
  default Optional<String> getModuleVersion() {
    return Optional.empty();
  }

  /**
   * Retrieves the module identifier.
   *
   * @return The module name with version suffix when the version is available, otherwise the module name.
   */
  default String getModuleId() {
    return getModuleVersion()
      .filter(version -> !version.isBlank())
      .map(version -> getModuleName() + "-" + version)
      .orElse(getModuleName());
  }

  /**
   * Provides the database schema name associated with the given tenant ID.
   *
   * @param tenantId The ID of the tenant for which the database schema name is requested.
   * @return The database schema name corresponding to the given tenant ID.
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  String getDBSchemaName(String tenantId);
}
