package org.folio.spring.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Deprecated(since = "10.0.0", forRemoval = true)
@FeignClient(name = "folio-spring-base-authn-client", url = "authn")
public interface AuthnClient {

  @PostMapping(value = "/login-with-expiry", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<LoginResponse> loginWithExpiry(@RequestBody UserCredentials credentials);

  @PostMapping(value = "/login", consumes = APPLICATION_JSON_VALUE)
  ResponseEntity<LoginResponse> login(@RequestBody UserCredentials credentials);

  @PostMapping(value = "/credentials", consumes = APPLICATION_JSON_VALUE)
  void saveCredentials(@RequestBody UserCredentials credentials);

  @DeleteMapping(value = "/credentials", consumes = APPLICATION_JSON_VALUE)
  void deleteCredentials(@RequestParam("userId") String userId);

  record UserCredentials(String username, String password) {
  }

  record LoginResponse(String accessTokenExpiration, String refreshTokenExpiration) {
  }
}
