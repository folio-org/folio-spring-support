package org.folio.spring;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * FolioExecutionContext is used to store essential request headers in thread local.
 * Folio Spring Base populates this data using
 * {@link org.folio.spring.scope.filter.FolioExecutionScopeFilter FolioExecutionScopeFilter}.
 *
 * <p>It is used by {@link org.folio.spring.client.EnrichUrlAndHeadersClient EnrichUrlAndHeadersClient},
 * to provide right tenant id and other headers for outgoing REST requests.
 *
 * <p>It is also used in {@link org.folio.spring.config.DataSourceSchemaAdvisorBeanPostProcessor
 * DataSourceSchemaAdvisorBeanPostProcessor} for selection of the appropriate schema for sql queries.
 *<br><br>
 *
 * <p><pre>CAUTION:
 * <strong>It should not be used to in asynchronous code executions (as it is stored in thread local),</strong>
 * unless the appropriate data is manually set by
 * {@link org.folio.spring.scope.FolioExecutionScopeExecutionContextManager#beginFolioExecutionContext
 * FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(folioExecutionContext)}
 * The `FolioExecutionScopeExecutionContextManager.endFolioExecutionContext()` should be called
 * when the execution is finished.
 * </pre>
 *
 */
public interface FolioExecutionContext {

  default String getTenantId() {
    return null;
  }

  default String getOkapiUrl() {
    return null;
  }

  default String getToken() {
    return null;
  }

  default UUID getUserId() {
    return null;
  }

  default String getRequestId() {
    return null;
  }

  default Map<String, Collection<String>> getAllHeaders() {
    return null;
  }

  default Map<String, Collection<String>> getOkapiHeaders() {
    return null;
  }

  default FolioModuleMetadata getFolioModuleMetadata() {
    return null;
  }

  /**
   * A useful method to get an actual instance of the FolioExecutionContext when the one is injected through
   * a wrapper/proxy. Pay attention, that the result type must be Object, otherwise a proxy will return itself
   */
  default Object getInstance() {
    return this;
  }
}
