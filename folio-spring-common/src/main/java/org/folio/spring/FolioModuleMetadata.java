package org.folio.spring;

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
   * Provides the database schema name associated with the given tenant ID.
   *
   * @param tenantId The ID of the tenant for which the database schema name is requested.
   * @return The database schema name corresponding to the given tenant ID.
   */
  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  String getDBSchemaName(String tenantId);
}
