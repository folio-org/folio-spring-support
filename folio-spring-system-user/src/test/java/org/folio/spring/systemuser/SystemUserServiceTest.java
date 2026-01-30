package org.folio.spring.systemuser;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.folio.spring.service.PrepareSystemUserService.SYSTEM_USER_TYPE;
import static org.folio.spring.utils.TokenUtils.FOLIO_ACCESS_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.AuthnClient.UserCredentials;
import org.folio.spring.client.UsersClient;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.exception.SystemUserAuthorizationException;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.ResultList;
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserProperties;
import org.folio.spring.service.SystemUserService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.client.HttpClientErrorException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

  public static final String OKAPI_URL = "http://okapi";
  private static final String TENANT_ID = "test";
  private static final Instant NOW = Instant.now();
  private static final Instant TOKEN_EXPIRATION = NOW.plus(1, ChronoUnit.DAYS);
  private static final Instant REFRESH_TOKEN_EXPIRATION = NOW.plus(7, ChronoUnit.DAYS);
  private static final Instant CUSTOM_TOKEN_EXPIRATION = TOKEN_EXPIRATION.minus(12, ChronoUnit.HOURS);
  private static final String MOCK_TOKEN = "test_token";
  private static final String CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME
    = "Cannot retrieve okapi token for system user: username";
  private static final String CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME_AND_DIKU
    = "Cannot retrieve okapi token for system user: username [tenantId=diku]";
  private final ResponseEntity<AuthnClient.LoginResponse> expectedResponse =
    Mockito.spy(ResponseEntity.of(Optional.of(
      new AuthnClient.LoginResponse(TOKEN_EXPIRATION.toString(), REFRESH_TOKEN_EXPIRATION.toString()))));
  @Mock
  private AuthnClient authnClient;
  @Mock
  private UsersClient usersClient;
  @Mock
  private ExecutionContextBuilder contextBuilder;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private FolioEnvironment environment;
  @Mock
  private PrepareSystemUserService prepareSystemUserService;
  @Mock
  private Cache<String, SystemUser> userCache;

  @Test
  void getAuthedSystemUser_positive() {
    var expectedUserId = UUID.randomUUID();
    var expectedUserToken = userToken(CUSTOM_TOKEN_EXPIRATION);

    when(authnClient
      .loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(usersClient.query("username==\"username\"")).thenReturn(ResultList.asSinglePage(
      UsersClient.User.builder().id(expectedUserId.toString()).username("username").type(SYSTEM_USER_TYPE).active(true)
              .personal(new UsersClient.User.Personal("last")).build()));
    when(environment.getOkapiUrl()).thenReturn(OKAPI_URL);
    when(contextBuilder.forSystemUser(any(), any())).thenReturn(context);
    when(expectedResponse.getHeaders()).thenReturn(cookieHeaders(expectedUserToken.accessToken()));

    var actual = systemUserService(systemUserProperties()).getAuthedSystemUser(TENANT_ID);
    assertToken(actual.token(), expectedUserToken);
    assertThat(actual.userId()).isEqualTo(expectedUserId.toString());
  }

  @ParameterizedTest
  @ValueSource(ints = {40, 24 * 60 * 60})
  void getAuthedSystemUserUsingCache_positive(int plusSeconds) {
    var expectedUserToken = userToken(NOW.plusSeconds(plusSeconds));
    var systemUserService = systemUserService(systemUserProperties());
    systemUserService.setSystemUserCache(userCache);

    when(userCache.get(eq(TENANT_ID), any())).thenReturn(systemUserValue().withToken(expectedUserToken));

    var actual = systemUserService.getAuthedSystemUser(TENANT_ID);
    assertThat(actual.token().accessToken()).isEqualTo(expectedUserToken.accessToken());
    verify(userCache).get(eq(TENANT_ID), any());
    verify(authnClient, never()).loginWithExpiry(any());
    verify(environment, never()).getOkapiUrl();
    verify(contextBuilder, never()).forSystemUser(any(), any());
  }

  @ParameterizedTest
  @ValueSource(ints = {-24 * 60 * 60, -1})
  void getAuthedSystemUserUsingCacheWithExpiredAccessToken_positive(int plusSeconds) {
    var cachedUserToken = userToken(NOW.plusSeconds(plusSeconds));
    var systemUserService = systemUserService(systemUserProperties());
    systemUserService.setSystemUserCache(userCache);
    when(contextBuilder.forSystemUser(any(), any())).thenReturn(context);
    var tokenResponseMock = cachedUserToken.accessToken();
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(expectedResponse.getHeaders()).thenReturn(cookieHeaders("access-token"));
    when(userCache.get(eq(TENANT_ID), any())).thenReturn(systemUserValue().withToken(cachedUserToken));

    var actual = systemUserService.getAuthedSystemUser(TENANT_ID);
    assertThat(actual.token().accessToken())
      .isEqualTo(tokenResponseMock);
    assertThat(actual.token().accessTokenExpiration())
      .isCloseTo(CUSTOM_TOKEN_EXPIRATION, within(10, ChronoUnit.SECONDS));
    verify(userCache).get(eq(TENANT_ID), any());
  }

  @Test
  void authSystemUser_positive() {
    var expectedToken = "x-okapi-token-value";
    var expectedUserToken = UserToken.builder()
      .accessToken(expectedToken)
      .accessTokenExpiration(CUSTOM_TOKEN_EXPIRATION)
      .build();
    var systemUser = systemUserValue();

    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(expectedResponse.getHeaders()).thenReturn(cookieHeaders(expectedToken));

    var actual = systemUserService(systemUserProperties()).authSystemUser(systemUser);
    assertToken(actual, expectedUserToken);
  }

  @Test
  void overloaded_authSystemUser_positive() {
    var expectedToken = "x-okapi-token-value";
    when(contextBuilder.forSystemUser(any(), any())).thenReturn(context);
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(expectedResponse.getHeaders()).thenReturn(cookieHeaders(expectedToken));
    var expectedUserToken = UserToken.builder()
      .accessToken(expectedToken)
      .accessTokenExpiration(CUSTOM_TOKEN_EXPIRATION)
      .build();

    var actual = systemUserService(systemUserProperties()).authSystemUser("tenantId", "username", "password");
    assertToken(actual, expectedUserToken);
  }

  @Test
  void authSystemUser_negative_emptyHeaders() {
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(expectedResponse.getHeaders()).thenReturn(new HttpHeaders());

    var systemUser = systemUserValue();

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(IllegalStateException.class)
      .hasMessage("User [username] cannot login with expiry because of missing tokens");
  }

  @Test
  void overloaded_authSystemUser_negative_emptyHeaders() {
    when(expectedResponse.getHeaders()).thenReturn(new HttpHeaders());

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService
      .authSystemUser("diku", "username", "password"))
      .isInstanceOf(SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME_AND_DIKU);
  }

  @Test
  void authSystemUser_negative_headersDoesNotContainsRequiredValue() {
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    var expectedHeaders = new HttpHeaders();
    expectedHeaders.put(HttpHeaders.SET_COOKIE, emptyList());
    when(expectedResponse.getHeaders()).thenReturn(expectedHeaders);

    var systemUser = systemUserValue();

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(IllegalStateException.class)
      .hasMessage("User [username] cannot login with expiry because of missing tokens");
  }

  @Test
  void overloaded_authSystemUser_negative_headersDoesNotContainsRequiredValue() {
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    var expectedHeaders = new HttpHeaders();
    expectedHeaders.put(HttpHeaders.SET_COOKIE, emptyList());
    when(expectedResponse.getHeaders()).thenReturn(expectedHeaders);
    when(contextBuilder.forSystemUser(any(), any())).thenReturn(context);

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService
      .authSystemUser("diku", "username", "password"))
      .isInstanceOf(SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME_AND_DIKU);
  }

  @Test
  void authSystemUser_negative_emptyBody() {
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password")))
      .thenReturn(new ResponseEntity<>(org.springframework.http.HttpStatus.OK));

    var systemUser = systemUserValue();

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(IllegalStateException.class)
      .hasMessage("User [username] cannot login with expiry because expire times missing for status 200 OK");
  }

  @Test
  void overloaded_authSystemUser_negative_emptyBody() {
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password")))
      .thenReturn(new ResponseEntity<>(org.springframework.http.HttpStatus.OK));
    when(contextBuilder.forSystemUser(any(), any())).thenReturn(context);

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService
      .authSystemUser("diku", "username", "password"))
      .isInstanceOf(SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME_AND_DIKU);
  }

  @Test
  void authSystemUser_when_loginExpiry_notFoundException() {
    var expectedUserToken = new UserToken(MOCK_TOKEN, Instant.MAX);
    doThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null))
      .when(authnClient).loginWithExpiry(any());
    when(authnClient.login(new UserCredentials("username", "password")))
      .thenReturn(buildClientResponse(MOCK_TOKEN));
    var systemUser = systemUserValue();
    var systemUserService = systemUserService(systemUserProperties());
    var actual = systemUserService.authSystemUser(systemUser);
    assertThat(actual).isEqualTo(expectedUserToken);
  }

  @Test
  void overloaded_authSystemUser_when_loginExpiry_notFoundException() {
    var expectedUserToken = new UserToken(MOCK_TOKEN, Instant.MAX);
    doThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null))
      .when(authnClient).loginWithExpiry(any());
    when(authnClient.login(new UserCredentials("username", "password")))
      .thenReturn(buildClientResponse(MOCK_TOKEN));
    when(contextBuilder.forSystemUser(any(), any())).thenReturn(context);
    var systemUserService = systemUserService(systemUserProperties());
    var actual = systemUserService.authSystemUser("tenantId", "username", "password");
    assertThat(actual).isEqualTo(expectedUserToken);
  }

  @Test
  void authSystemUser_when_loginExpiry_notFoundException_loginLegacReturnsNull() {
    doThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null))
      .when(authnClient).loginWithExpiry(any());
    when(authnClient.login(new UserCredentials("username", "password")))
      .thenReturn(buildClientResponse(null));
    var systemUser = systemUserValue();
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(
        SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME);
  }

  @Test
  void authSystemUser_when_loginExpiry_Returns400Response() {
    doThrow(HttpClientErrorException.BadRequest.create(
        HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY, null, null))
      .when(authnClient).loginWithExpiry(any());
    var systemUser = systemUserValue();
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(
        SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME);
  }

  @Test
  void overloaded_authSystemUser_when_loginExpiry_Returns400Response() {
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService
      .authSystemUser("diku", "username", "password"))
      .isInstanceOf(SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME_AND_DIKU);
  }

  @Test
  void overloaded_authSystemUser_when_loginExpiryReturnsNull() {
    when(contextBuilder.forSystemUser(any(), any())).thenReturn(context);
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService
      .authSystemUser("diku", "username", "password"))
      .isInstanceOf(SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME_AND_DIKU);
  }

  @Test
  void authSystemUser_when_loginExpiry_and_tokenLegacy_both_notFound() {
    doThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null))
      .when(authnClient).loginWithExpiry(any());
    doThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null))
      .when(authnClient).login(any());
    var systemUser = systemUserValue();
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(
        SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME);
  }

  @Test
  void overloaded_authSystemUser_when_loginExpiry_and_tokenLegacy_both_notFound() {
    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService
      .authSystemUser("diku", "username", "password"))
      .isInstanceOf(SystemUserAuthorizationException.class)
      .hasMessage(CANNOT_RETRIEVE_OKAPI_TOKEN_FOR_USERNAME_AND_DIKU);
  }

  @Test
  void testGetFolioUserSuccess() {
    UsersClient.User entity = mock(UsersClient.User.class); // gives us an object reference
    when(usersClient.query("username==\"foo\"")).thenReturn(ResultList.asSinglePage(entity));

    Optional<UsersClient.User> result = systemUserService(systemUserProperties()).getFolioUser("foo");

    assertThat(result).containsSame(entity);

    verify(usersClient, times(1)).query("username==\"foo\"");
    verifyNoMoreInteractions(usersClient);
  }

  @Test
  void testGetFolioUserNullResponse() {
    when(usersClient.query("username==\"foo\"")).thenReturn(null);

    Optional<UsersClient.User> result = systemUserService(systemUserProperties()).getFolioUser("foo");

    assertThat(result).isEmpty();
  }

  @Test
  void testGetFolioUserEmptyResponse() {
    when(usersClient.query("username==\"foo\"")).thenReturn(ResultList.empty());

    Optional<UsersClient.User> result = systemUserService(systemUserProperties()).getFolioUser("foo");

    assertThat(result).isEmpty();
  }

  private static SystemUser systemUserValue() {
    return SystemUser.builder().username("username").okapiUrl(OKAPI_URL).tenantId(TENANT_ID).build();
  }

  private static SystemUserProperties systemUserProperties() {
    return new SystemUserProperties("username", "password", "system", "permissions/test-permissions.csv");
  }

  private void assertToken(UserToken actualToken, UserToken expectedToken) {
    assertThat(actualToken.accessToken())
      .isEqualTo(expectedToken.accessToken());
    assertThat(actualToken.accessTokenExpiration())
      .isCloseTo(expectedToken.accessTokenExpiration(), within(10, ChronoUnit.SECONDS));
  }

  private SystemUserService systemUserService(SystemUserProperties properties) {
    return new SystemUserService(contextBuilder, properties, environment, authnClient, usersClient);
  }

  private UserToken userToken(Instant accessExpiration) {
    return UserToken.builder()
      .accessToken("access-token")
      .accessTokenExpiration(accessExpiration)
      .build();
  }

  private HttpHeaders cookieHeaders(String token) {
    return new HttpHeaders(new MultiValueMapAdapter<>(Map.of(HttpHeaders.SET_COOKIE, List.of(
      new DefaultCookie(FOLIO_ACCESS_TOKEN, token).toString()
    ))));
  }

  private ResponseEntity<AuthnClient.LoginResponse> buildClientResponse(String token) {
    return ResponseEntity.ok()
      .header(XOkapiHeaders.TOKEN, token)
      .body(new AuthnClient.LoginResponse(TOKEN_EXPIRATION.toString(), REFRESH_TOKEN_EXPIRATION.toString()));
  }
}
