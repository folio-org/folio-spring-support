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
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "folio-spring-base-users-client", url = "users")
public interface UsersClient {
  /** Searches for user(s) by CQL query. */
  @GetMapping
  ResultList<User> query(@RequestParam("query") String query);

  /**
   * Creates a new user.
   *
   * @deprecated use {@link #createUser(User)} instead
   */
  @Deprecated(since = "4.0.0")
  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  void saveUser(@RequestBody User user);

  /** Creates a new user. */
  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  void createUser(@RequestBody User user);

  /** Updates an existing user. */
  @PutMapping(value = "{user.id}", consumes = APPLICATION_JSON_VALUE)
  void updateUser(@RequestBody User user);

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

    /**
     * Stores all the extra properties from the incoming JSON that we do not specifically use in this library.
     * We must store these to be able to update the user (in the case of re-activation), otherwise all additional
     * information from mod-users will be lost when re-serializing.
     */
    @JsonAnyGetter
    @JsonAnySetter
    @Singular("extraProperties")
    private Map<String, Object> extraProperties;

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
