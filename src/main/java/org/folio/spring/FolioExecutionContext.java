package org.folio.spring;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface FolioExecutionContext {

  String getTenantId();

  String getOkapiUrl();

  String getToken();

  UUID getUserId();

  String getUserName();

  Map<String, Collection<String>> getAllHeaders();

  Map<String, Collection<String>> getOkapiHeaders();

  FolioModuleMetadata getFolioModuleMetadata();
}
