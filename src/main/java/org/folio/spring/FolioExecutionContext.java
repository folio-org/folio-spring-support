package org.folio.spring;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

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
}
