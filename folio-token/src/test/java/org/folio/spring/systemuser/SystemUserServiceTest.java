package org.folio.spring.systemuser;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.utils.TokenUtils.FOLIO_ACCESS_TOKEN;
import static org.folio.spring.utils.TokenUtils.FOLIO_REFRESH_TOKEN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserProperties;
import org.folio.spring.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMapAdapter;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

  public static final String OKAPI_URL = "http://okapi";
  private static final String TENANT_ID = "test";
  private static final Instant TOKEN_EXPIRATION = Instant.now().plus(1, ChronoUnit.DAYS);
  @Mock
  private AuthnClient authnClient;
  @Mock
  private ExecutionContextBuilder contextBuilder;
  private final ResponseEntity<AuthnClient.LoginResponse> expectedResponse = Mockito.spy(ResponseEntity.of(Optional.of(
      new AuthnClient.LoginResponse(TOKEN_EXPIRATION.toString()))));
  @Mock
  private FolioExecutionContext context;
  @Mock
  private FolioEnvironment environment;
  @Mock
  private PrepareSystemUserService prepareSystemUserService;
  @Mock
  private Cache<String, SystemUser> userCache;

  private static SystemUser systemUserValue() {
    return SystemUser.builder().username("username").okapiUrl(OKAPI_URL).tenantId(TENANT_ID).build();
  }

  private static SystemUserProperties systemUserProperties() {
    return new SystemUserProperties("username", "password", "system", "permissions/test-permissions.csv");
  }

  @Test
  void getAuthedSystemUser_positive() {
    var expectedUserId = UUID.randomUUID();
    var expectedUserToken = userToken(TOKEN_EXPIRATION);
    var expectedHeaders = cookieHeaders(expectedUserToken.accessToken(), expectedUserToken.accessToken());

    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(prepareSystemUserService.getFolioUser("username")).thenReturn(Optional.of(
        new UsersClient.User(expectedUserId.toString(), "username", true, new UsersClient.User.Personal("last"))));
    when(environment.getOkapiUrl()).thenReturn(OKAPI_URL);
    when(contextBuilder.forSystemUser(any())).thenReturn(context);
    when(expectedResponse.getHeaders()).thenReturn(expectedHeaders);

    var actual = systemUserService(systemUserProperties()).getAuthedSystemUser(TENANT_ID);
    assertThat(actual.token()).isEqualTo(expectedUserToken);
    assertThat(actual.userId()).isEqualTo(expectedUserId.toString());
  }

  @Test
  void getAuthedSystemUserUsingCache_positive() {
    var expectedUserToken = userToken(Instant.now().plus(1, ChronoUnit.DAYS));
    var systemUserService = systemUserService(systemUserProperties());
    systemUserService.setSystemUserCache(userCache);

    when(userCache.get(eq(TENANT_ID), any())).thenReturn(systemUserValue().withToken(expectedUserToken));

    var actual = systemUserService.getAuthedSystemUser(TENANT_ID);
    assertThat(actual.token().accessToken()).isEqualTo(expectedUserToken.accessToken());
    verify(userCache).get(eq(TENANT_ID), any());
    verify(authnClient, never()).login(any());
    verify(environment, never()).getOkapiUrl();
    verify(contextBuilder, never()).forSystemUser(any());
  }

  @Test
  void authSystemUser_positive() {
    var expectedToken = "x-okapi-token-value";
    var expectedUserToken = UserToken.builder()
        .accessToken(expectedToken)
        .accessTokenExpiration(TOKEN_EXPIRATION)
        .build();
    var expectedHeaders = cookieHeaders(expectedToken);
    var systemUser = systemUserValue();

    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(expectedResponse.getHeaders()).thenReturn(expectedHeaders);

    var actual = systemUserService(systemUserProperties()).authSystemUser(systemUser);
    assertThat(actual).isEqualTo(expectedUserToken);
  }

  @Test
  void authSystemUser_negative_emptyHeaders() {
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password"))).thenReturn(expectedResponse);
    when(expectedResponse.getHeaders()).thenReturn(new HttpHeaders());

    var systemUser = systemUserValue();

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(IllegalStateException.class)
        .hasMessage("User [username] cannot log in expiry because of missing tokens");
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
        .hasMessage("User [username] cannot log in expiry because of missing tokens");
  }

  @Test
  void authSystemUser_negative_emptyBody() {
    when(authnClient.loginWithExpiry(new UserCredentials("username", "password")))
        .thenReturn(new ResponseEntity<>(org.springframework.http.HttpStatus.OK));

    var systemUser = systemUserValue();

    var systemUserService = systemUserService(systemUserProperties());
    assertThatThrownBy(() -> systemUserService.authSystemUser(systemUser)).isInstanceOf(IllegalStateException.class)
        .hasMessage("User [username] cannot log in expiry because expire times missing for status 200 OK");
  }

  private SystemUserService systemUserService(SystemUserProperties properties) {
    return new SystemUserService(contextBuilder, properties, environment, authnClient, prepareSystemUserService);
  }

  private UserToken userToken(Instant accessExpiration) {
    return UserToken.builder()
        .accessToken("access-token")
        .accessTokenExpiration(accessExpiration)
        .build();
  }

  private HttpHeaders cookieHeaders(String token) {
    return cookieHeaders(token, token);
  }

  private HttpHeaders cookieHeaders(String accessToken, String refreshToken) {
    return new HttpHeaders(new MultiValueMapAdapter<>(Map.of(HttpHeaders.SET_COOKIE, List.of(
        new DefaultCookie(FOLIO_ACCESS_TOKEN, accessToken).toString(),
        new DefaultCookie(FOLIO_REFRESH_TOKEN, refreshToken).toString()
    ))));
  }

  private ResponseEntity<AuthnClient.LoginResponse> buildClientResponse(String token) {
    return ResponseEntity.ok()
        .headers(cookieHeaders(token))
        .body(new AuthnClient.LoginResponse(TOKEN_EXPIRATION.toString()));
  }
}
