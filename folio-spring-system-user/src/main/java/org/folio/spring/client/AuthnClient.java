package org.folio.spring.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@Deprecated(since = "10.0.0", forRemoval = true)
@HttpExchange(url = "authn", contentType = APPLICATION_JSON_VALUE)
public interface AuthnClient {

  @PostExchange("/login-with-expiry")
  ResponseEntity<LoginResponse> loginWithExpiry(UserCredentials credentials);

  @PostExchange("/login")
  ResponseEntity<LoginResponse> login(UserCredentials credentials);

  @PostExchange("/credentials")
  void saveCredentials(UserCredentials credentials);

  @DeleteExchange("/credentials")
  void deleteCredentials(@RequestParam("userId") String userId);

  record UserCredentials(String username, String password) {
  }

  record LoginResponse(String accessTokenExpiration, String refreshTokenExpiration) {
  }
}
