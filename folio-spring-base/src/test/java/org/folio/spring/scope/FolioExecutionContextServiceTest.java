package org.folio.spring.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.exception.FolioContextExecutionException;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class FolioExecutionContextServiceTest {

  private FolioModuleMetadata moduleMetadata;
  private FolioExecutionContextService service;

  @BeforeEach
  void setUp() {
    moduleMetadata = new FolioModuleMetadata() {

      @Override
      public String getModuleName() {
        return "test-module";
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return "public";
      }
    };
    service = new FolioExecutionContextService(moduleMetadata);
  }

  @Test
  void execute_callable_success() {
    String tenantId = "tenantA";
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("header1", List.of("value1"));

    String result = service.execute(tenantId, headers, () -> {
      assertContext(tenantId, "header1", "value1");
      return "success";
    });
    assertEquals("success", result);
  }

  @Test
  void execute_callable_exception() {
    String tenantId = "tenantB";
    Map<String, Collection<String>> headers = new HashMap<>();

    Exception ex = assertThrows(FolioContextExecutionException.class, () ->
      service.execute(tenantId, headers, () -> {
        throw new RuntimeException("fail");
      })
    );
    assertTrue(ex.getMessage().contains("tenant = tenantB"));
    assertInstanceOf(RuntimeException.class, ex.getCause());
  }

  @Test
  void execute_runnable_success() {
    String tenantId = "tenantC";
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("header2", List.of("value2"));
    final boolean[] called = {false};

    service.execute(tenantId, headers, () -> {
      assertContext(tenantId, "header2", "value2");
      called[0] = true;
    });
    assertTrue(called[0]);
  }

  @Test
  void execute_runnable_exception() {
    String tenantId = "tenantD";
    Map<String, Collection<String>> headers = new HashMap<>();

    Exception ex = assertThrows(FolioContextExecutionException.class, () ->
      service.execute(tenantId, headers, (Runnable) () -> {
        throw new RuntimeException("fail");
      })
    );
    assertTrue(ex.getMessage().contains("tenant = tenantD"));
    assertInstanceOf(RuntimeException.class, ex.getCause());
  }

  @Test
  void execute_with_context_success() {
    String tenantId = "tenantE";
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("header2", List.of("value2"));
    FolioExecutionContext context = new FolioExecutionContext() {
      @Override
      public Map<String, Collection<String>> getAllHeaders() {
        return headers;
      }
    };

    String result = service.execute(tenantId, context, () -> {
      assertContext(tenantId, "header2", "value2");
      return "context-success";
    });
    assertEquals("context-success", result);
  }

  @Test
  void execute_with_context_runnable_success() {
    String tenantId = "tenantF";
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put("header2", List.of("value2"));
    FolioExecutionContext context = new FolioExecutionContext() {
      @Override
      public Map<String, Collection<String>> getAllHeaders() {
        return headers;
      }
    };
    final boolean[] called = {false};

    service.execute(tenantId, context, () -> {
      assertContext(tenantId, XOkapiHeaders.TENANT, tenantId);
      called[0] = true;
    });
    assertTrue(called[0]);
  }

  private void assertContext(String tenantId, String additionalHeader, String additionalHeaderValue) {
    var context = FolioExecutionScopeExecutionContextManager.getFolioExecutionContext();
    assertThat(context).isNotNull();
    assertThat(context.getTenantId()).isEqualTo(tenantId);
    assertThat(context.getAllHeaders())
      .extracting(map -> map.get(additionalHeader).iterator().next(),
        map -> map.get(XOkapiHeaders.TENANT).iterator().next())
      .containsExactly(additionalHeaderValue, tenantId);
    assertEquals(moduleMetadata, context.getFolioModuleMetadata());
  }
}
