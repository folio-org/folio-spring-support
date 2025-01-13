package org.folio.spring.context;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.folio.spring.utils.TokenUtils;

@Log4j2
public class SystemUserExecutionContext implements FolioExecutionContext {

  private final FolioModuleMetadata moduleMetadata;

  @CheckForNull
  private final Supplier<SystemUser> refresher;

  private SystemUser user;
  private Map<String, Collection<String>> headers;

  public SystemUserExecutionContext(
    FolioModuleMetadata moduleMetadata,
    SystemUser user,
    Supplier<SystemUser> refresher
  ) {
    this.moduleMetadata = moduleMetadata;
    this.refresher = refresher;
    this.user = user;

    this.headers = getHeadersMap();
  }

  public FolioModuleMetadata getFolioModuleMetadata() {
    return moduleMetadata;
  }

  public String getTenantId() {
    return Optional.ofNullable(user.tenantId()).orElse("");
  }

  public String getOkapiUrl() {
    return Optional.ofNullable(user.okapiUrl()).orElse("");
  }

  public String getToken() {
    updateTokenIfNeeded();
    return Optional.ofNullable(user.token()).map(UserToken::accessToken).orElse("");
  }

  public UUID getUserId() {
    return Optional.ofNullable(user.userId()).map(UUID::fromString).orElse(null);
  }

  public String getRequestId() {
    return null;
  }

  public Map<String, Collection<String>> getAllHeaders() {
    return getOkapiHeaders();
  }

  public Map<String, Collection<String>> getOkapiHeaders() {
    updateTokenIfNeeded();
    return headers;
  }

  private void updateTokenIfNeeded() {
    if (user.token() == null || !TokenUtils.tokenAboutToExpire(user)) {
      return;
    }

    if (refresher == null) {
      log.warn(
        "System user token is about to expire at {}, but no refresh method was provided, "
          + "so I can't do anything about it... :(",
        user.token().accessTokenExpiration()
      );
      return;
    }

    log.info("System user token is about to expire at {}, preemptively refreshing...",
             user.token().accessTokenExpiration());

    user = refresher.get();
    headers = getHeadersMap();
  }

  private Map<String, Collection<String>> getHeadersMap() {
    Map<String, Collection<String>> newHeaders = new HashMap<>();

    SystemUser systemUser = user;

    if (StringUtils.isNotBlank(systemUser.okapiUrl())) {
      newHeaders.put(XOkapiHeaders.URL, singleton(systemUser.okapiUrl()));
    }
    if (StringUtils.isNotBlank(systemUser.tenantId())) {
      newHeaders.put(XOkapiHeaders.TENANT, singleton(systemUser.tenantId()));
    }
    if (systemUser.token() != null && StringUtils.isNotBlank(systemUser.token().accessToken())) {
      newHeaders.put(XOkapiHeaders.TOKEN, singleton(systemUser.token().accessToken()));
    }
    if (StringUtils.isNotBlank(systemUser.userId())) {
      newHeaders.put(XOkapiHeaders.USER_ID, singleton(systemUser.userId()));
    }

    return newHeaders;
  }
}
