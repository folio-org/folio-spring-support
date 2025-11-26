package org.folio.spring.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.folio.spring.model.ResultList;
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
  void createUser(User user);

  @PutExchange("{user.id}")
  void updateUser(User user);

  @Value
  @Jacksonized
  @Builder(toBuilder = true)
  class User {

    private String id;
    private String username;
    private String type;
    private boolean active;
    private String expirationDate;
    private Personal personal;

    @JsonAnySetter
    @Singular("extraProperties")
    private Map<String, Object> extraProperties;

    @JsonAnyGetter
    public Map<String, Object> getExtraProperties() {
      return this.extraProperties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Personal(String firstName, String lastName) {
      public Personal(String lastName) {
        this(null, lastName);
      }
    }
  }
}
