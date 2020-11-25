package org.folio.spring;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.folio.spring.integration.XOkapiHeaders.OKAPI_HEADERS_PREFIX;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;

public class DefaultFolioExecutionContext implements FolioExecutionContext {
  private final FolioModuleMetadata folioModuleMetadata;
  private final Map<String, Collection<String>> allHeaders;
  private final Map<String, Collection<String>> okapiHeaders;

  private final String tenantId;
  private final String okapiUrl;
  private final String token;
  private final String userName;

  public DefaultFolioExecutionContext(FolioModuleMetadata folioModuleMetadata, Map<String, Collection<String>> allHeaders) {
    this.folioModuleMetadata = folioModuleMetadata;
    this.allHeaders = allHeaders;
    this.okapiHeaders = new HashMap<>(allHeaders);
    this.okapiHeaders.entrySet().removeIf(e -> !e.getKey().toLowerCase().startsWith(OKAPI_HEADERS_PREFIX));

    this.tenantId = retrieveFirstSafe(okapiHeaders.get(TENANT));
    this.okapiUrl = retrieveFirstSafe(okapiHeaders.get(URL));
    this.token = retrieveFirstSafe(okapiHeaders.get(TOKEN));
    //TODO: retrieve user name correctly from JWT token
    this.userName = "NO_USER";
  }

  private String retrieveFirstSafe(Collection<String> strings) {
    return strings != null && !strings.isEmpty() ? strings.iterator().next() : "";
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  @Override
  public String getOkapiUrl() {
    return okapiUrl;
  }

  @Override
  public String getToken() {
    return token;
  }

  @Override
  public String getUserName() {
    return userName;
  }

  @Override
  public Map<String, Collection<String>> getAllHeaders() {
    return allHeaders;
  }

  @Override
  public Map<String, Collection<String>> getOkapiHeaders() {
    return okapiHeaders;
  }

  @Override
  public FolioModuleMetadata getFolioModuleMetadata() {
    return folioModuleMetadata;
  }
}
