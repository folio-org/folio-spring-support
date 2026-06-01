package org.folio.spring.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.client.UsersClient.User;

import java.util.HashMap;
import java.util.Map;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@UnitTest
class UsersClientTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testDeserialization() {
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
  void testSerialization() {
    Map<String, Object> user = objectMapper.readValue(
      objectMapper.writeValueAsString(
        new User("id", "username", null, true, null,
          new User.Personal("Smith"),
          Map.of("someOtherProperty", "value"))
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
