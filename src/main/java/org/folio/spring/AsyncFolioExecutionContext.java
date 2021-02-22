package org.folio.spring;

import static java.util.Collections.emptyMap;

import java.util.Collection;
import java.util.Map;
import lombok.AllArgsConstructor;

/**
 * Execution context for a background tasks, e.g. kafka listeners.
 */
@AllArgsConstructor
public final class AsyncFolioExecutionContext implements FolioExecutionContext {
  private final String tenantId;
  private final FolioModuleMetadata moduleMetadata;

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public String getOkapiUrl() {
    throw new UnsupportedOperationException("getOkapiUrl");
  }

  @Override
  public String getToken() {
    throw new UnsupportedOperationException("getToken");
  }

  @Override
  public String getUserName() {
    throw new UnsupportedOperationException("getUserName");
  }

  @Override
  public Map<String, Collection<String>> getAllHeaders() {
    return emptyMap();
  }

  @Override
  public Map<String, Collection<String>> getOkapiHeaders() {
    return emptyMap();
  }

  @Override
  public FolioModuleMetadata getFolioModuleMetadata() {
    return moduleMetadata;
  }
}
