package org.folio.spring.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.client.UsersClient.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class UsersClientTest {

  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testDeserialization() throws JsonProcessingException {
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

    assertThat(user)
      .extracting("id", "username", "active", "personal.lastName", "extraProperties.someOtherProperty")
      .containsExactly("id", "username", true, "Smith", "value");
  }

  @Test
  public void testSerialization() throws JsonProcessingException {
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
      new TypeReference<HashMap<String, Object>>() {}
    );

    assertThat(user)
      .extracting("id", "username", "active", "personal.lastName", "extraProperties.someOtherProperty")
      .containsExactly("id", "username", true, "Smith", "value");
  }
}
