package org.folio.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.AbstractStringAssert;
import org.folio.spring.scope.EmptyFolioExecutionContextHolder;
import org.folio.spring.scope.FolioExecutionScopeConfig;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class FolioExecutionContextTest {

  private final FolioExecutionContext context = new FolioExecutionContext() {};

  private final FolioExecutionScopeConfig folioExecutionScopeConfig =
      new FolioExecutionScopeConfig(new EmptyFolioExecutionContextHolder(null));

  @Test
  void shouldReturnNullsForDefaultImplementation() {
    assertThat(context).satisfies(ctx -> {
      assertNull(ctx.getTenantId());
      assertNull(ctx.getOkapiUrl());
      assertNull(ctx.getToken());
      assertNull(ctx.getUserId());
      assertNull(ctx.getRequestId());
      assertNull(ctx.getAllHeaders());
      assertNull(ctx.getOkapiHeaders());
      assertNull(ctx.getFolioModuleMetadata());
    });
  }

  @Test
  void executeCanThrow() {
    assertTenantId().isNull();
    var fooContext = folioExecutionContext("foo");
    var e = assertThrows(RuntimeException.class, () -> {
      fooContext.execute(() -> {
        assertTenantId().isEqualTo("foo");
        throw new RuntimeException("catch me if you can");
      });
    });
    assertTenantId().isNull();
    assertThat(e.getMessage()).isEqualTo("catch me if you can");
  }

  @Test
  void executeCanReturn() {
    assertTenantId().isNull();
    var result = folioExecutionContext("calc").execute(() -> {
      assertTenantId().isEqualTo("calc");
      return 42;
    });
    assertTenantId().isNull();
    assertThat(result).isEqualTo(42);
  }

  AbstractStringAssert<?> assertTenantId() {
    return assertThat(folioExecutionScopeConfig.folioExecutionContext().getTenantId());
  }

  FolioExecutionContext folioExecutionContext(String tenantId) {
    Map<String, Collection<String>> headers = Map.of(TENANT, List.of(tenantId));
    return new DefaultFolioExecutionContext(null, headers);
  }
}
