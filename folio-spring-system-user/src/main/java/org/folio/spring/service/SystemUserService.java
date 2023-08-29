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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserService {

  public static final String CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT = "Cannot retrieve okapi token for tenant: ";
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
    var token = getTokenWithExpiry(user);
    if (!isValidUserToken(token)) {
      log.info("Login with expiry end-point returned null");
      return getTokenLegacy(user);
    }
    return token;
  }

  public UserToken authSystemUser(String tenantId, String username, String password) {
    var token = getTokenWithExpiry(tenantId, username, password);
    if (!isValidUserToken(token)) {
      log.info("Login with expiry end-point returned null");
      return getTokenLegacy(tenantId, username, password);
    }
    return token;
  }

  private boolean isValidUserToken(UserToken token) {
    return token != null && token.accessToken() != null && token.accessTokenExpiration() != null;
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
    try {
      var response =
          authnClient.login(new UserCredentials(user.username(), systemUserProperties.password()));

      var accessToken = response.getHeaders().get(X_OKAPI_TOKEN).get(0);

      if (isNull(accessToken)) {
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + user.username());
      }

      return UserToken.builder().accessToken(accessToken).accessTokenExpiration(Instant.MAX).build();
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.info("Login with legacy end-point not found");
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + user.username());
      } else {
        log.info("Login with legacy end-point returned unexpected error");
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + user.username());
      }
    }
  }

  private UserToken getTokenLegacy(String tenantId, String username, String password) {
    try {
      var response =
          authnClient.login(new UserCredentials(username, password));

      var accessToken = response.getHeaders().get(X_OKAPI_TOKEN).get(0);

      if (isNull(accessToken)) {
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + username);
      }

      return UserToken.builder().accessToken(accessToken).accessTokenExpiration(Instant.MAX).build();
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.info("Login with legacy end-point not found");
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + username);
      } else {
        log.info("Login with legacy end-point returned unexpected error");
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + username);
      }
    }
  }

  private UserToken getTokenWithExpiry(SystemUser user) {
    try {
      var response =
          authnClient.loginWithExpiry(new UserCredentials(user.username(), systemUserProperties.password()));

      if (isNull(response.getBody())) {
        throw new IllegalStateException(
            String.format("User [%s] cannot %s because expire times missing for status %s",
            user.username(), "login with expiry", response.getStatusCode()));
      }

      var cookieHeaders = response.getHeaders().get(SET_COOKIE);

      if (isNull(cookieHeaders) || CollectionUtils.isEmpty(cookieHeaders)) {
        throw new IllegalStateException(
            String.format("User [%s] cannot %s because of missing tokens",
                user.username(), "login with expiry"));
      }

      return parseUserTokenFromCookies(cookieHeaders, response.getBody());
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.info("Login with legacy end-point not found. calling login with legacy end-point.");
        return getTokenLegacy(user);
      } else {
        log.info("Login with legacy end-point returned unexpected error");
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + user.username());
      }
    }
  }

  private UserToken getTokenWithExpiry(String tenantId, String username, String password) {
    try {
      var response =
          authnClient.loginWithExpiry(new UserCredentials(username, password));

      if (isNull(response.getBody())) {
        throw new IllegalStateException(
            String.format("User [%s] cannot %s because expire times missing for status %s",
                username, "login with expiry", response.getStatusCode()));
      }

      var cookieHeaders = response.getHeaders().get(SET_COOKIE);

      if (isNull(cookieHeaders) || CollectionUtils.isEmpty(cookieHeaders)) {
        throw new IllegalStateException(
            String.format("User [%s] cannot %s because of missing tokens",
                username, "login with expiry"));
      }

      return parseUserTokenFromCookies(cookieHeaders, response.getBody());
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.info("Login with legacy end-point not found. calling login with legacy end-point.");
        return getTokenLegacy(tenantId, username, password);
      } else {
        log.info("Login with legacy end-point returned unexpected error");
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + username);
      }
    }
  }
}
