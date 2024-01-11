package org.folio.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FolioModuleMetadataTest {

  @Test
  void stubTest() {
    String moduleName = "module-name";
    String schemaName = "schema-name";
    FolioModuleMetadata folioModuleMetadata = new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return moduleName;
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return schemaName;
      }
    };

    assertEquals(moduleName, folioModuleMetadata.getModuleName());
    assertEquals(schemaName, folioModuleMetadata.getDBSchemaName("!"));
  }
}
