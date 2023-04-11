package org.folio.spring;

public interface FolioModuleMetadata {
  String getModuleName();

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  String getDBSchemaName(String tenantId);
}
