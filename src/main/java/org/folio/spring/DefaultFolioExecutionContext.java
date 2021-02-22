package org.folio.spring;

import static org.folio.spring.integration.XOkapiHeaders.OKAPI_HEADERS_PREFIX;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.folio.spring.domain.SystemUser;

@Builder
@AllArgsConstructor
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

  public static DefaultFolioExecutionContext forSystemUser(
    FolioModuleMetadata folioModuleMetadata, SystemUser systemUser) {

    return DefaultFolioExecutionContext.builder()
      .allHeaders(Collections.emptyMap())
      .okapiHeaders(Collections.emptyMap())
      .folioModuleMetadata(folioModuleMetadata)
      .tenantId(systemUser.getTenantId())
      .okapiUrl(systemUser.getOkapiUrl())
      .token(systemUser.getOkapiToken())
      .userName(systemUser.getUsername())
      .build();
  }

  private static String retrieveFirstSafe(Collection<String> strings) {
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
