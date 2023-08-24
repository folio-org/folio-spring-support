package org.folio.spring.context;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionContextBuilder {

  private final FolioModuleMetadata moduleMetadata;

  public FolioExecutionContext forSystemUser(SystemUser systemUser) {
    var okapiUrl = systemUser.okapiUrl();
    var tenantId = systemUser.tenantId();
    var token = systemUser.token() == null ? null : systemUser.token().accessToken();
    var userId = systemUser.userId();

    return buildContext(okapiUrl, tenantId, token, userId, null);
  }

  private FolioExecutionContext buildContext(String okapiUrl, String tenantId, String token, String userId,
                                             String requestId) {
    Map<String, Collection<String>> headers = new HashMap<>();
    if (isNotBlank(okapiUrl)) {
      headers.put(XOkapiHeaders.URL, singleton(okapiUrl));
    }
    if (isNotBlank(tenantId)) {
      headers.put(XOkapiHeaders.TENANT, singleton(tenantId));
    }
    if (isNotBlank(token)) {
      headers.put(XOkapiHeaders.TOKEN, singleton(token));
    }
    if (isNotBlank(userId)) {
      headers.put(XOkapiHeaders.USER_ID, singleton(userId));
    }
    if (isNotBlank(requestId)) {
      headers.put(XOkapiHeaders.REQUEST_ID, singleton(requestId));
    }
    return new DefaultFolioExecutionContext(moduleMetadata, headers);
  }
}
