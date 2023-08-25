package org.folio.spring.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.utils.TokenUtils.FOLIO_ACCESS_TOKEN;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.netty.handler.codec.http.cookie.DefaultCookie;
import java.time.Instant;
import java.util.List;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.model.UserToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TokenUtilsTest {

  @Test
  void parseUserTokenFromCookies_positive() {
    var accessExp = Instant.now();
    var refreshExp = Instant.now();
    var authResponse = new AuthnClient.LoginResponse(accessExp.toString());
    var accessToken = "acc";
    var cookies = List.of(new DefaultCookie(FOLIO_ACCESS_TOKEN, accessToken).toString());
    var expected = UserToken.builder()
        .accessToken(accessToken)
        .accessTokenExpiration(accessExp)
        .build();

    var result = TokenUtils.parseUserTokenFromCookies(cookies, authResponse);
    assertThat(result)
        .isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({",1970-01-01T00:00:00Z"})
  void parseUserTokenFromCookies_negative_invalidExpiration(String accessExp, String refreshExp) {
    var authResponse = new AuthnClient.LoginResponse(accessExp);
    var accessToken = "acc";
    var refreshToken = "ref";
    var cookies = List.of(new DefaultCookie(FOLIO_ACCESS_TOKEN, accessToken).toString());

    var ex = assertThrows(IllegalArgumentException.class,
        () -> TokenUtils.parseUserTokenFromCookies(cookies, authResponse));

    assertThat(ex.getMessage()).isEqualTo("Unable to parse token expiration: null");
  }

  @ParameterizedTest
  @CsvSource({"folioRefreshToken,folioAccessToken"})
  void parseUserTokenFromCookies_negative_missingCookie(String cookieName, String missingCookie) {
    var authResponse = new AuthnClient.LoginResponse("");
    var token = "token";
    var cookies = List.of(new DefaultCookie(cookieName, token).toString());

    var ex = assertThrows(IllegalArgumentException.class,
        () -> TokenUtils.parseUserTokenFromCookies(cookies, authResponse));

    assertThat(ex.getMessage()).isEqualTo("No cookie found for name: " + missingCookie);
  }

}
