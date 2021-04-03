package org.folio.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class FolioExecutionContextTest {

  private FolioExecutionContext context = new FolioExecutionContext() {};

  @Test
  void shouldReturnNullsForDefaultImplementation() {
    assertThat(context).satisfies(ctx -> {
      assertNull(ctx.getTenantId());
      assertNull(ctx.getOkapiUrl());
      assertNull(ctx.getToken());
      assertNull(ctx.getUserId());
      assertNull(ctx.getUserName());
      assertNull(ctx.getAllHeaders());
      assertNull(ctx.getOkapiHeaders());
      assertNull(ctx.getFolioModuleMetadata());
    });
  }
}