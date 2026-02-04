package org.folio.spring.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Singular;
import org.folio.spring.model.ResultList;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@Deprecated(since = "10.0.0", forRemoval = true)
@HttpExchange(url = "users", contentType = APPLICATION_JSON_VALUE)
public interface UsersClient {
  @GetExchange
  ResultList<User> query(@RequestParam("query") String query);

  @PostExchange
  void createUser(@RequestBody User user);

  @PutExchange("{user.id}")
  void updateUser(@RequestBody User user);

  record User(String id, String username, String type, Boolean active, String expirationDate, Personal personal,
              @JsonAnyGetter @JsonAnySetter @Singular("extraProperties") Map<String, Object> extraProperties) {

    public static User fromAndActive(User other) {
      return new User(other.id, other.username, other.type, true, null, other.personal, other.extraProperties);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Personal(@Nullable String firstName, String lastName) {
      public Personal(String lastName) {
        this(null, lastName);
      }
    }
  }
}
