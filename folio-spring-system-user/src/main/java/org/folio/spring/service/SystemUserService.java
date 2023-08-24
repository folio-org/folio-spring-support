package org.folio.spring.service;

import static java.util.Objects.isNull;
import static org.folio.edge.api.utils.Constants.X_OKAPI_TOKEN;
import static org.folio.spring.utils.TokenUtils.parseUserTokenFromCookies;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import com.github.benmanes.caffeine.cache.Cache;
import feign.FeignException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.edge.api.utils.exception.AuthorizationException;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.AuthnClient.UserCredentials;
import org.folio.spring.client.UsersClient;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserService {

  private final ExecutionContextBuilder contextBuilder;
  private final SystemUserProperties systemUserProperties;
  private final FolioEnvironment environment;
  private final AuthnClient authnClient;
  private final PrepareSystemUserService prepareUserService;
  private Cache<String, SystemUser> systemUserCache;

  /**
   * Get authenticate system user.
   * Get from cache if present (or getSystemUser otherwise). Call login expiry endpoint in case
   * access token expired. Call login endpoint in case login-expiry endpoint returns null or it
   * doesn't exist
   *
   * @param tenantId The tenant name
   * @return {@link SystemUser} with token value
   */
  public SystemUser getAuthedSystemUser(String tenantId) {
    if (systemUserCache == null) {
      return getSystemUser(tenantId);
    }

    var user = systemUserCache.get(tenantId, this::getSystemUser);
    var userToken = user.token();
    var now = Instant.now();
    if (userToken.accessTokenExpiration().isAfter(now)) {
      return user;
    }

    systemUserCache.invalidate(tenantId);
    user = getSystemUser(tenantId);
    systemUserCache.put(tenantId, user);

    return user;
  }

  /**
   * Authenticate system user and return token value.
   *
   * @param user {@link SystemUser} to log with
   * @return token value
   */
  public UserToken authSystemUser(SystemUser user) {
    try {
      var token = getTokenWithExpiry(user);
      if (token == null) {
        log.info("Login with expiry end-point returned null");
        return getTokenLegacy(user);
      }
      return token;
    } catch (FeignException ex) {
      if (ex.status() == HttpStatus.NOT_FOUND.value()) {
        log.info("Login with expiry end-point not found");
        return getTokenLegacy(user);
      } else {
        log.info("Login with expiry and legacy end-point returned null or not found");
        return null;
      }
    }
  }

  @Autowired(required = false)
  public void setSystemUserCache(Cache<String, SystemUser> systemUserCache) {
    this.systemUserCache = systemUserCache;
  }

  private SystemUser getSystemUser(String tenantId) {
    log.info("Attempting to issue token for system user [tenantId={}]", tenantId);
    var systemUser = SystemUser.builder()
        .tenantId(tenantId)
        .username(systemUserProperties.username())
        .okapiUrl(environment.getOkapiUrl())
        .build();

    // create context for authentication
    try (var fex = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser))) {
      var token = authSystemUser(systemUser);
      systemUser = systemUser.withToken(token);
      log.info("Token for system user has been issued [tenantId={}]", tenantId);
    }
    // create context for user with token for getting user id
    try (var fex = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser))) {
      var userId = prepareUserService.getFolioUser(systemUserProperties.username())
          .map(UsersClient.User::id).orElse(null);
      return systemUser.withUserId(userId);
    }
  }

  private UserToken getTokenLegacy(SystemUser user) {
    var response =
        authnClient.login(new UserCredentials(user.username(), systemUserProperties.password()));

    if (isNull(response) || response.getStatusCode() == HttpStatusCode.valueOf(404)) {
      return null;
    }

    var accessToken = response.getHeaders().get(X_OKAPI_TOKEN).get(0);

    if (isNull(accessToken)) {
      throw new AuthorizationException("Cannot retrieve okapi token for tenant: " + user.username());
    }

    return UserToken.builder()
        .accessToken(accessToken)
        .accessTokenExpiration(Instant.MAX)
        .build();
  }

  private UserToken getTokenWithExpiry(SystemUser user) {
    var response =
        authnClient.loginWithExpiry(new UserCredentials(user.username(), systemUserProperties.password()));

    if (isNull(response) || response.getStatusCode() == HttpStatusCode.valueOf(404)) {
      return null;
    }

    if (isNull(response.getBody())) {
      throw new IllegalStateException(String.format(
          "User [%s] cannot %s because expire times missing for status %s",
          user.username(), "login with expiry", response.getStatusCode()));
    }

    var cookieHeaders = response.getHeaders().get(SET_COOKIE);

    if (isNull(cookieHeaders) || CollectionUtils.isEmpty(cookieHeaders)) {
      throw new IllegalStateException(String.format(
          "User [%s] cannot %s because of missing tokens", user.username(), "login with expiry"));
    }

    return parseUserTokenFromCookies(cookieHeaders, response.getBody());
  }
}
