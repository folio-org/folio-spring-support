package org.folio.spring;

public interface FolioModuleMetadata {
  String getModuleName();

  String getDBSchemaName(String tenantId);
}
