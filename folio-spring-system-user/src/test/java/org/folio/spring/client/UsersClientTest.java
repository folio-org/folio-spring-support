package org.folio.spring.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.client.UsersClient.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class UsersClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testDeserialization() throws JsonProcessingException {
    User user = objectMapper.readValue(
      """
        {
          "id": "id",
          "username": "username",
          "active": true,
          "personal": {"lastName": "Smith"},
          "someOtherProperty": "value"
        }
        """,
      User.class
    );

    // we want someOtherProperty moved under extraProperties for deserialization
    assertThat(user)
      .extracting("id", "username", "active", "personal.lastName", "extraProperties.someOtherProperty")
      .containsExactly("id", "username", true, "Smith", "value");
  }

  @Test
  void testSerialization() throws JsonProcessingException {
    Map<String, Object> user = objectMapper.readValue(
      objectMapper.writeValueAsString(
        User
          .builder()
          .id("id")
          .username("username")
          .active(true)
          .personal(new User.Personal("Smith"))
          .extraProperties(Map.of("someOtherProperty", "value"))
          .build()
      ),
      new TypeReference<HashMap<String, Object>>() { }
    );

    // we want someOtherProperty extracted out of extraProperties for serialization
    assertThat(user)
      .extracting("id", "username", "active", "personal.lastName", "someOtherProperty")
      .containsExactly("id", "username", true, "Smith", "value");

    assertThat(user).doesNotContainKey("extraProperties");
  }
}
