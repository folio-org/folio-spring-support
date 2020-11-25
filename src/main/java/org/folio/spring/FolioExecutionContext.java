package org.folio.spring;

import java.util.Collection;
import java.util.Map;

public interface FolioExecutionContext {
  String getTenantId();

  String getOkapiUrl();

  String getToken();

  String getUserName();

  Map<String, Collection<String>> getAllHeaders();

  Map<String, Collection<String>> getOkapiHeaders();

  FolioModuleMetadata getFolioModuleMetadata();
}
