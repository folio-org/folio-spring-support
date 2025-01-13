package org.folio.spring.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.folio.spring.model.ResultList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "folio-spring-base-users-client", url = "users")
public interface UsersClient {
  @GetMapping
  ResultList<User> query(@RequestParam("query") String query);

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  void createUser(@RequestBody User user);

  @PutMapping(value = "{user.id}", consumes = APPLICATION_JSON_VALUE)
  void updateUser(@RequestBody User user);

  @Getter
  @Accessors(fluent = true)
  @Builder(toBuilder = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  class User {

    private String id;
    private String username;
    private String type;
    private boolean active;
    private String expirationDate;
    private Personal personal;

    @JsonIgnore
    @Builder.Default
    private Map<String, Object> extraProperties = new HashMap<>();

    // we must store extra properties to be able to update the user, otherwise
    // all additional information from mod-users will be lost.
    @JsonAnyGetter
    public Map<String, Object> getExtraProperties() {
      return this.extraProperties;
    }

    @JsonAnySetter
    public void set(String key, Object value) {
      this.extraProperties.put(key, value);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Personal(String firstName, String lastName) {
      public Personal(String lastName) {
        this(null, lastName);
      }
    }
  }
}
