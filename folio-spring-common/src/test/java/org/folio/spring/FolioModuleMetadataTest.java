package org.folio.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class FolioModuleMetadataTest {

  @Test
  void getModuleId_positive_returnsModuleNameWhenVersionIsMissing() {
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
    assertTrue(folioModuleMetadata.getModuleVersion().isEmpty());
    assertEquals(moduleName, folioModuleMetadata.getModuleId());
    assertEquals(schemaName, folioModuleMetadata.getDBSchemaName("!"));
  }

  @Test
  void getModuleId_positive_includesVersionWhenPresent() {
    String moduleName = "mod-foo";
    String moduleVersion = "1.2.3";
    FolioModuleMetadata folioModuleMetadata = new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return moduleName;
      }

      @Override
      public Optional<String> getModuleVersion() {
        return Optional.of(moduleVersion);
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return "schema-name";
      }
    };

    assertEquals(moduleName + "-" + moduleVersion, folioModuleMetadata.getModuleId());
  }
}
