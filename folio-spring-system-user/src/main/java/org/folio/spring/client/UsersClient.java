package org.folio.spring.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
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

  @Builder(toBuilder = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  record User(String id, String username, String type, boolean active, String expirationDate, Personal personal) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Personal(String firstName, String lastName) {
      public Personal(String lastName) {
        this(null, lastName);
      }
    }
  }
}
