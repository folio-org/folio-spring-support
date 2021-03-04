package org.folio.spring;

import static org.folio.spring.integration.XOkapiHeaders.OKAPI_HEADERS_PREFIX;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Getter;

import org.folio.spring.utils.JWTokenUtils;

@Getter
public class DefaultFolioExecutionContext implements FolioExecutionContext {

  private final FolioModuleMetadata folioModuleMetadata;
  private final Map<String, Collection<String>> allHeaders;
  private final Map<String, Collection<String>> okapiHeaders;

  private final String tenantId;
  private final String okapiUrl;
  private final String token;
  private final UUID userId;
  private final String userName;

  public DefaultFolioExecutionContext(FolioModuleMetadata folioModuleMetadata, Map<String, Collection<String>> allHeaders) {
    this.folioModuleMetadata = folioModuleMetadata;
    this.allHeaders = allHeaders;
    this.okapiHeaders = new HashMap<>(allHeaders);
    this.okapiHeaders.entrySet().removeIf(e -> !e.getKey().toLowerCase().startsWith(OKAPI_HEADERS_PREFIX));

    this.tenantId = retrieveFirstSafe(okapiHeaders.get(TENANT));
    this.okapiUrl = retrieveFirstSafe(okapiHeaders.get(URL));
    this.token = retrieveFirstSafe(okapiHeaders.get(TOKEN));

    var optionalUserInfo = JWTokenUtils.parseToken(token);
    if (optionalUserInfo.isPresent()) {
      var userInfo = optionalUserInfo.get();
      this.userId = userInfo.getUserId();
      this.userName = userInfo.getUserName();
    } else {
      this.userId = null;
      this.userName = "NO_USER";
    }
  }

  private String retrieveFirstSafe(Collection<String> strings) {
    return strings != null && !strings.isEmpty() ? strings.iterator().next() : "";
  }

}
