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

/**
 * An execution context representing requests being performed on behalf of a system user. This context contains
 * information about the system user, and is not associated with any particular request.
 */
@Log4j2
@Deprecated(since = "10.0.0", forRemoval = true)
public class SystemUserExecutionContext implements FolioExecutionContext {

  private final FolioModuleMetadata moduleMetadata;

  @CheckForNull
  private final Supplier<SystemUser> refresher;

  private SystemUser systemUser;
  private Map<String, Collection<String>> headers;

  /**
   * Creates a context for a system user.
   *
   * @param moduleMetadata metadata about the module
   * @param systemUser the user to send requests on behalf of
   * @param refresher an optional supplier which, upon the {@code systemUser}'s expiration, should return a new
   *                  {@link SystemUser} with a fresh access token
   */
  public SystemUserExecutionContext(
    FolioModuleMetadata moduleMetadata,
    SystemUser systemUser,
    Supplier<SystemUser> refresher
  ) {
    this.moduleMetadata = moduleMetadata;
    this.refresher = refresher;
    this.systemUser = systemUser;

    this.headers = getHeadersMap();
  }

  @Override
  public FolioModuleMetadata getFolioModuleMetadata() {
    return moduleMetadata;
  }

  @Override
  public String getTenantId() {
    return Optional.ofNullable(systemUser.tenantId()).orElse("");
  }

  @Override
  public String getOkapiUrl() {
    return Optional.ofNullable(systemUser.okapiUrl()).orElse("");
  }

  @Override
  public String getToken() {
    updateTokenIfNeeded();
    return Optional.ofNullable(systemUser.token()).map(UserToken::accessToken).orElse("");
  }

  @Override
  public UUID getUserId() {
    return Optional.ofNullable(systemUser.userId()).map(UUID::fromString).orElse(null);
  }

  /**
   * This execution context is for for requests originating from the system user, so they're not part
   * of any existing request chain. Therefore, request ID does not apply and this will always return null.
   */
  @Override
  public String getRequestId() {
    return null;
  }

  @Override
  public Map<String, Collection<String>> getAllHeaders() {
    return getOkapiHeaders();
  }

  @Override
  public Map<String, Collection<String>> getOkapiHeaders() {
    updateTokenIfNeeded();
    return headers;
  }

  private void updateTokenIfNeeded() {
    if (systemUser.token() == null || !TokenUtils.tokenAboutToExpire(systemUser)) {
      return;
    }

    if (refresher == null) {
      log.warn(
        "System user token is about to expire at {}, but no refresh method was provided, "
          + "so I can't do anything about it... :(",
        systemUser.token().accessTokenExpiration()
      );
      return;
    }

    log.info("System user token is about to expire at {}, preemptively refreshing...",
             systemUser.token().accessTokenExpiration());

    systemUser = refresher.get();
    headers = getHeadersMap();
  }

  private Map<String, Collection<String>> getHeadersMap() {
    Map<String, Collection<String>> newHeaders = new HashMap<>();

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
