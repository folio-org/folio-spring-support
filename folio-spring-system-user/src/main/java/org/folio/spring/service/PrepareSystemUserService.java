package org.folio.spring.service;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.AuthnClient.UserCredentials;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.PermissionsClient.Permission;
import org.folio.spring.client.PermissionsClient.Permissions;
import org.folio.spring.client.UsersClient;
import org.folio.spring.client.UsersClient.User;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class PrepareSystemUserService {

  private final UsersClient usersClient;
  private final AuthnClient authnClient;
  private final PermissionsClient permissionsClient;
  private final SystemUserProperties systemUserProperties;

  public final static String SYSTEM = "system";

  public void setupSystemUser() {
    log.info("Preparing system user...");
    var folioUser = getFolioUser(systemUserProperties.username());
    var userId = folioUser.map(User::id)
        .orElse(UUID.randomUUID().toString());

    if (folioUser.isPresent()) {
      log.info("System user already exists");
      addPermissions(userId);
    } else {
      log.info("No system user exist, creating...");

      createFolioUser(userId);
      assignPermissions(userId);
      saveCredentials();
    }
    log.info("System user has been created");
  }

  public Optional<User> getFolioUser(String username) {
    var users = usersClient.query("username==" + username);
    return (users == null || users.getResult() == null) ? Optional.empty() : users.getResult().stream().findFirst();
  }

  private void createFolioUser(String id) {
    final var user = prepareUserObject(id);
    usersClient.saveUser(user);
  }

  private void saveCredentials() {
    authnClient.saveCredentials(new UserCredentials(systemUserProperties.username(), systemUserProperties.password()));

    log.info("Saved credentials for user: [{}]", systemUserProperties.username());
  }

  private void assignPermissions(String userId) {
    List<String> permissionsToAssign = getResourceLines(systemUserProperties.permissionsFilePath());

    if (isEmpty(permissionsToAssign)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    var permissions = new Permissions(UUID.randomUUID().toString(), userId, permissionsToAssign);

    permissionsClient.assignPermissionsToUser(permissions);
    log.info("Permissions assigned to system user: [{}]", permissionsToAssign);
  }

  private void addPermissions(String userId) {
    var permissionsToAssign = getResourceLines(systemUserProperties.permissionsFilePath());

    if (isEmpty(permissionsToAssign)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    var permissionsToAdd = new HashSet<>(permissionsToAssign);
    permissionsClient.getUserPermissions(userId).getResult().forEach(permissionsToAdd::remove);

    permissionsToAdd.forEach(permission ->
        permissionsClient.addPermission(userId, new Permission(permission)));
    log.info("Permissions assigned to system user: [{}]", permissionsToAdd);
  }

  private User prepareUserObject(String id) {
    return new User(id, systemUserProperties.username(), SYSTEM,  true,
        new User.Personal(systemUserProperties.lastname()));
  }

  @SneakyThrows
  private List<String> getResourceLines(String permissionsFilePath) {
    var resource = new ClassPathResource(permissionsFilePath);
    return IOUtils.readLines(resource.getInputStream(), StandardCharsets.UTF_8);
  }
}
