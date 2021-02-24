package org.folio.spring.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FolioModuleMetadataTest {
  @Test
  void shouldReturnSchemaNameIfTenantValid() {
    assertEquals("valid_tenant_name_folio_spring_base",
      getDBSchemaName("valid_tenant_name"));
  }

  @Test
  void shouldThrowExceptionIfTenantInvalid() {
    assertThrows(IllegalArgumentException.class,
      () -> getDBSchemaName("drop schema"));
  }

  private String getDBSchemaName(String tenant) {
    return new FolioSpringConfiguration()
      .folioModuleMetadata("folio-spring-base").getDBSchemaName(tenant);
  }
}
