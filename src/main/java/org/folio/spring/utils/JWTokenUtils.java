package org.folio.spring.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class JWTokenUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Optional<UserInfo> parseToken(String token) {
    try {
      String[] split = token.split("\\.");
      return Optional.of(parse(split[1]));
    } catch (Exception e) {
      log.error("JWT parsing failed with exception", e);
      return Optional.empty();
    }
  }

  private static UserInfo parse(String strEncoded) throws JsonProcessingException {
    byte[] decodedBytes = Base64.getDecoder().decode(strEncoded);
    var json = new String(decodedBytes, StandardCharsets.UTF_8);
    return OBJECT_MAPPER.readValue(json, UserInfo.class);
  }

  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class UserInfo {

    @JsonProperty("user_id")
    private UUID userId;
    @JsonProperty("sub")
    private String userName;
  }
}
