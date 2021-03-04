package org.folio.spring.utils;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class JWTokenUtilsTest {

  @Test
  void testValidToken() {
    var expectedUserId = UUID.fromString("ad162b38-1291-4437-8948-9d13eeced9f6");
    var expectedUserName = "John Doe";

    var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
      + "eyJzdWIiOiJKb2huIERvZSIsInVzZXJfaWQiOiJhZDE2MmIzOC0xMjkxLTQ0MzctODk0OC05ZDEzZWVjZWQ5ZjYifQ."
      + "CLC0P0Obed2A3bJc6JOzkYXPbuedvU0lvAY1MFMEGMM";

    var actual = JWTokenUtils.parseToken(token);

    assertTrue(actual.isPresent());
    assertEquals(expectedUserId, actual.get().getUserId());
    assertEquals(expectedUserName, actual.get().getUserName());
  }

  @Test
  void testInvalidToken() {
    var token = "INVALID_TOKEN";
    var actual = JWTokenUtils.parseToken(token);

    assertTrue(actual.isEmpty());
  }

  @Test
  void testNullToken() {
    var actual = JWTokenUtils.parseToken(null);

    assertTrue(actual.isEmpty());
  }
}