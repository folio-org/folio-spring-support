package org.folio.spring.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.folio.spring.model.ResultList;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@Deprecated(since = "10.0.0", forRemoval = true)
@HttpExchange(url = "perms/users", contentType = APPLICATION_JSON_VALUE)
public interface PermissionsClient {

  @PostExchange
  void assignPermissionsToUser(Permissions permissions);

  @PostExchange("/{userId}/permissions?indexField=userId")
  void addPermission(@PathVariable("userId") String userId, Permission permission);

  @GetExchange("/{userId}/permissions?indexField=userId")
  ResultList<String> getUserPermissions(@PathVariable("userId") String userId);

  record Permission(String permissionName) {
  }

  record Permissions(String id,
                     String userId,
                     @JsonProperty("permissions") List<String> allowedPermissions) {
  }
}
