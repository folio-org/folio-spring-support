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
  public static final String
      LOGIN_WITH_LEGACY_END_POINT_RETURNED_UNEXPECTED_ERROR = "Login with legacy end-point returned "
      + "unexpected error: {}";
  public static final String
      LOGIN_WITH_EXPIRY_END_POINT_RETURNED_UNEXPECTED_ERROR = "Login with expiry end-point returned "
      + "unexpected error: {}";
  public static final String LOGIN_WITH_EXPIRY = "login with expiry";
  public static final String
      LOGIN_WITH_EXPIRY_END_POINT_NOT_FOUND_CALLING_LOGIN_WITH_LEGACY_END_POINT = "Login with expiry end-point "
      + "not found. calling login with legacy end-point.";
  public static final String LOGIN_WITH_LEGACY_END_POINT_NOT_FOUND = "Login with legacy end-point not found.";
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
    var now = Instant.now().minusSeconds(30L);
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
    return getToken(new UserCredentials(user.username(), systemUserProperties.password()));
  }

  public UserToken authSystemUser(String tenantId, String username, String password) {
    log.info("Attempting to issue token for system user [tenantId={}]", tenantId);
    try (var fex =
             new FolioExecutionContextSetter(contextBuilder.forSystemUser(prepareSystemUser(tenantId, username)))) {
      var token = getToken(new UserCredentials(username, password));
      log.info("Token for system user has been issued [tenantId={}]", tenantId);
      return token;
    } catch (Exception exp) {
      log.error("Unexpected error occurred while setting folio context" + exp);
      throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + tenantId);
    }
  }

  private UserToken getToken(UserCredentials userCredentials) {
    var token = getTokenWithExpiry(userCredentials);
    if (!isValidUserToken(token)) {
      log.info("Login with expiry end-point returned null");
      return getTokenLegacy(userCredentials);
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
    var systemUser = prepareSystemUser(tenantId, systemUserProperties.username());

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

  private UserToken getTokenLegacy(UserCredentials credentials) {
    try {
      var response =
          authnClient.login(credentials);

      var accessToken = response.getHeaders().get(X_OKAPI_TOKEN).get(0);

      if (isNull(accessToken)) {
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + credentials.username());
      }

      return UserToken.builder().accessToken(accessToken).accessTokenExpiration(Instant.MAX).build();
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.error(LOGIN_WITH_LEGACY_END_POINT_NOT_FOUND);
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + credentials.username());
      } else {
        log.error(LOGIN_WITH_LEGACY_END_POINT_RETURNED_UNEXPECTED_ERROR, fex);
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + credentials.username());
      }
    }
  }

  private UserToken getTokenWithExpiry(UserCredentials credentials) {
    try {
      var response =
          authnClient.loginWithExpiry(credentials);

      if (isNull(response.getBody())) {
        throw new IllegalStateException(
            String.format("User [%s] cannot %s because expire times missing for status %s",
            credentials.username(), LOGIN_WITH_EXPIRY, response.getStatusCode()));
      }

      var cookieHeaders = response.getHeaders().get(SET_COOKIE);

      if (isNull(cookieHeaders) || CollectionUtils.isEmpty(cookieHeaders)) {
        throw new IllegalStateException(
            String.format("User [%s] cannot %s because of missing tokens",
                credentials.username(), LOGIN_WITH_EXPIRY));
      }

      return parseUserTokenFromCookies(cookieHeaders, response.getBody());
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.error(LOGIN_WITH_EXPIRY_END_POINT_NOT_FOUND_CALLING_LOGIN_WITH_LEGACY_END_POINT);
        return null;
      } else {
        log.error(LOGIN_WITH_EXPIRY_END_POINT_RETURNED_UNEXPECTED_ERROR, fex);
        throw new AuthorizationException(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_TENANT + credentials.username());
      }
    }
  }

  private SystemUser prepareSystemUser(String tenantId, String username) {
    return SystemUser.builder()
        .tenantId(tenantId)
        .username(username)
        .okapiUrl(environment.getOkapiUrl())
        .build();
  }
}
