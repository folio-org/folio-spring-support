package org.folio.spring.scope;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.junit.jupiter.api.Test;

class FolioExecutionContextSetterTest {

  @Test
  void noException() {
    var folioModuleMetadata = mock(FolioModuleMetadata.class);
    var httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getHeaderNames()).thenReturn(enumeration("X-okapi-TENANT"));
    when(httpServletRequest.getHeaders("X-okapi-TENANT")).thenReturn(enumeration("honolulu"));
    try (var x = new FolioExecutionContextSetter(folioModuleMetadata, httpServletRequest)) {
      assertThat(getTenantId(), is("honolulu"));
    }
    assertThat(getTenantId(), is(nullValue()));
  }

  @Test
  void exception() {
    var folioExecutionContext = mock(FolioExecutionContext.class);
    when(folioExecutionContext.getTenantId()).thenReturn("olympia");
    try (var x = new FolioExecutionContextSetter(folioExecutionContext)) {
      assertThat(getTenantId(), is("olympia"));
      throw new IllegalArgumentException();
    } catch (IllegalArgumentException e) {
      // ignore
    }
    assertThat(getTenantId(), is(nullValue()));
  }

  @Test
  void withoutTryWithResources() {
    var folioExecutionContext = mock(FolioExecutionContext.class);
    when(folioExecutionContext.getTenantId()).thenReturn("kiev");
    var x = new FolioExecutionContextSetter(folioExecutionContext);
    assertThat(getTenantId(), is("kiev"));
    x.close();
    assertThat(getTenantId(), is(nullValue()));
  }

  private static Enumeration<String> enumeration(String s) {
    return Collections.enumeration(List.of(s));
  }

  private static String getTenantId() {
    var folioExecutionContext = FolioExecutionScopeExecutionContextManager.getFolioExecutionContext();
    return folioExecutionContext == null ? null : folioExecutionContext.getTenantId();
  }
}
