package org.folio.spring.service;

import static java.util.Objects.isNull;
import static org.folio.spring.utils.TokenUtils.parseUserTokenFromCookies;
import static org.folio.spring.utils.TokenUtils.tokenAboutToExpire;
import static org.springframework.http.HttpHeaders.SET_COOKIE;

import com.github.benmanes.caffeine.cache.Cache;
import feign.FeignException;
import java.time.Instant;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.AuthnClient.UserCredentials;
import org.folio.spring.client.UsersClient;
import org.folio.spring.client.UsersClient.User;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.exception.SystemUserAuthorizationException;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.ResultList;
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

@Log4j2
@RequiredArgsConstructor
public class SystemUserService {

  public static final String TOKEN_FAILED_MSG = "Cannot retrieve okapi token for system user: ";
  public static final String LOGIN_LEGACY_UNEXPECTED_MSG = "Login with legacy end-point returned unexpected error";
  public static final String LOGIN_EXPIRY_UNEXPECTED_MSG = "Login with expiry end-point returned unexpected error";
  public static final String LOGIN_WITH_EXPIRY = "login with expiry";

  private final ExecutionContextBuilder contextBuilder;
  private final SystemUserProperties systemUserProperties;
  private final FolioEnvironment environment;
  private final AuthnClient authnClient;
  private final UsersClient usersClient;
  private Cache<String, SystemUser> systemUserCache;

  /**
   * Get authenticated system user.
   *
   * <p>Get from cache if present and is valid (not expired) for at least 30 seconds from now.
   * Otherwise call login expiry endpoint to get a new system user token.
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
    if (!tokenAboutToExpire(userToken)) {
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
    log.info("Attempting to issue token for system user [tenantId={}][username={}]", tenantId, username);
    var systemUser = buildSystemUserEntity(tenantId, username);
    try (var fex = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser, null))) {
      var token = getToken(new UserCredentials(username, password));
      log.info("Token for system user has been issued [tenantId={}][username={}]", tenantId, username);
      return token;
    } catch (Exception exp) {
      log.error("Unexpected error occurred while setting folio context" + exp);
      throw new SystemUserAuthorizationException(TOKEN_FAILED_MSG + username + " [tenantId=" + tenantId + "]");
    }
  }

  @Autowired(required = false)
  public void setSystemUserCache(Cache<String, SystemUser> systemUserCache) {
    this.systemUserCache = systemUserCache;
  }

  private UserToken getToken(UserCredentials userCredentials) {
    var token = getTokenWithExpiry(userCredentials);
    if (!isValidUserToken(token)) {
      log.info("Login with expiry end-point returned null; attempting a login with legacy end-point.");
      return getTokenLegacy(userCredentials);
    }
    return token;
  }

  private boolean isValidUserToken(UserToken token) {
    return token != null && token.accessToken() != null && token.accessTokenExpiration() != null;
  }

  private SystemUser getSystemUser(String tenantId) {
    log.info("Attempting to issue token for system user [tenantId={}]", tenantId);
    var systemUser = buildSystemUserEntity(tenantId, systemUserProperties.username());

    // create context for authentication
    try (var fex = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser, null))) {
      var token = authSystemUser(systemUser);
      systemUser = systemUser.withToken(token);
      log.info("Token for system user has been issued [tenantId={}]", tenantId);
    }
    // create context for user with token for getting user id
    try (var fex = new FolioExecutionContextSetter(contextBuilder.forSystemUser(systemUser, null))) {
      var userId = getFolioUser(systemUserProperties.username())
        .map(UsersClient.User::id)
        .orElse(null);
      return systemUser.withUserId(userId);
    }
  }

  private UserToken getTokenLegacy(UserCredentials credentials) {
    try {
      var response = authnClient.login(credentials);
      var tokenHeaders = response.getHeaders().get(XOkapiHeaders.TOKEN);

      if (isNull(tokenHeaders) || isNull(tokenHeaders.get(0))) {
        throw new SystemUserAuthorizationException(TOKEN_FAILED_MSG + credentials.username());
      }

      return UserToken.builder().accessToken(tokenHeaders.get(0)).accessTokenExpiration(Instant.MAX).build();
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.error("Login with legacy end-point not found.");
      } else {
        log.error(LOGIN_LEGACY_UNEXPECTED_MSG, fex);
      }
      throw new SystemUserAuthorizationException(TOKEN_FAILED_MSG + credentials.username(), fex);
    }
  }

  private UserToken getTokenWithExpiry(UserCredentials credentials) {
    try {
      var response = authnClient.loginWithExpiry(credentials);

      if (isNull(response.getBody())) {
        throw new IllegalStateException(String.format("User [%s] cannot %s because expire times missing for status %s",
          credentials.username(), LOGIN_WITH_EXPIRY, response.getStatusCode()));
      }

      var cookieHeaders = response.getHeaders().get(SET_COOKIE);

      if (isNull(cookieHeaders) || CollectionUtils.isEmpty(cookieHeaders)) {
        throw new IllegalStateException(String.format("User [%s] cannot %s because of missing tokens",
          credentials.username(), LOGIN_WITH_EXPIRY));
      }

      return parseUserTokenFromCookies(cookieHeaders, response.getBody());
    } catch (FeignException fex) {
      if (fex.status() == HttpStatus.NOT_FOUND.value()) {
        log.error("Login with expiry end-point not found. calling login with legacy end-point.");
        return null;
      } else {
        log.error(LOGIN_EXPIRY_UNEXPECTED_MSG, fex);
        throw new SystemUserAuthorizationException(TOKEN_FAILED_MSG + credentials.username(), fex);
      }
    }
  }

  private SystemUser buildSystemUserEntity(String tenantId, String username) {
    return SystemUser.builder()
      .tenantId(tenantId)
      .username(username)
      .okapiUrl(environment.getOkapiUrl())
      .build();
  }

  public Optional<User> getFolioUser(String username) {
    ResultList<User> users = usersClient.query("username==" + username);
    return Optional.ofNullable(users).map(ResultList::getResult).flatMap(r -> r.stream().findFirst());
  }
}
