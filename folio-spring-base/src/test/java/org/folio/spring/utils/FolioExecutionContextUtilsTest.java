package org.folio.spring.utils;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.Test;

class FolioExecutionContextUtilsTest {
  private final FolioModuleMetadata moduleMetadata = new FolioModuleMetadata() {
    @Override public String getModuleName() {
      return "test";
    }

    @Override public String getDBSchemaName(String tenantId) {
      return "tenant_test";
    }
  };

  @Test
  void testPrepareContextForTenant() {
    var tenant = "tenant";
    var newTenant = "new_tenant";
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(TENANT, singleton(tenant));
    var folioContext = new DefaultFolioExecutionContext(moduleMetadata, headers);

    var actual = FolioExecutionContextUtils.prepareContextForTenant(newTenant, moduleMetadata, folioContext);

    Consumer<FolioExecutionContext> contextRequirements = context -> {
      assertThat(context.getFolioModuleMetadata()).isEqualTo(moduleMetadata);
      assertThat(context.getOkapiHeaders()).containsOnlyKeys(TENANT);
      assertThat(context.getOkapiHeaders().get(TENANT)).containsOnly(newTenant);
    };

    assertThat(actual).satisfies(contextRequirements);
  }

  @Test
  void testPrepareContextForEmptyHeaders() {
    var headers = new HashMap<String, Collection<String>>();
    var folioContext = new DefaultFolioExecutionContext(moduleMetadata, headers);
    var newTenant = "new_tenant";

    assertThrows(IllegalStateException.class,
      () -> FolioExecutionContextUtils.prepareContextForTenant(newTenant, moduleMetadata, folioContext));
  }
}
