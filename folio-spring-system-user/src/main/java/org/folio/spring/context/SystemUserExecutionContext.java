package org.folio.spring.context;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.folio.spring.utils.TokenUtils;

@Log4j2
public class SystemUserExecutionContext implements FolioExecutionContext {

  @Getter
  private final FolioModuleMetadata moduleMetadata;

  @CheckForNull
  private final Supplier<SystemUser> refresher;

  private final AtomicReference<SystemUser> user;
  private final AtomicReference<Map<String, Collection<String>>> headers;

  public SystemUserExecutionContext(
    FolioModuleMetadata moduleMetadata,
    SystemUser user,
    Supplier<SystemUser> refresher
  ) {
    this.moduleMetadata = moduleMetadata;
    this.refresher = refresher;
    this.user = new AtomicReference<>(user);

    this.headers = new AtomicReference<>();
    fillHeadersMap();
  }

  public String getTenantId() {
    return user.get().tenantId();
  }

  public String getOkapiUrl() {
    return user.get().okapiUrl();
  }

  public String getToken() {
    updateTokenIfNeeded();
    return user.get().token().accessToken();
  }

  public UUID getUserId() {
    return UUID.fromString(user.get().userId());
  }

  public String getRequestId() {
    return null;
  }

  public Map<String, Collection<String>> getAllHeaders() {
    return getOkapiHeaders();
  }

  public Map<String, Collection<String>> getOkapiHeaders() {
    updateTokenIfNeeded();
    return headers.get();
  }

  private void updateTokenIfNeeded() {
    if (!TokenUtils.tokenAboutToExpire(user.get())) {
      return;
    }

    if (refresher == null) {
      log.warn(
        "System user token is about to expire at {}, but no refresh method was provided, so I can't do anything about it... :(",
        user.get().token().accessTokenExpiration()
      );
      return;
    }

    log.info("System user token is about to expire at {}, preemptively refreshing...",
             user.get().token().accessTokenExpiration());

    user.set(refresher.get());
    fillHeadersMap();
  }

  private void fillHeadersMap() {
    Map<String, Collection<String>> newHeaders = new HashMap<>();

    SystemUser systemUser = user.get();

    newHeaders.put(XOkapiHeaders.URL, singleton(systemUser.okapiUrl()));
    newHeaders.put(XOkapiHeaders.TENANT, singleton(systemUser.tenantId()));
    newHeaders.put(XOkapiHeaders.TOKEN, singleton(systemUser.token().accessToken()));
    newHeaders.put(XOkapiHeaders.USER_ID, singleton(systemUser.userId()));

    headers.set(newHeaders);
  }
}
