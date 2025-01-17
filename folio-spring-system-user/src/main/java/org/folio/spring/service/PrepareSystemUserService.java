package org.folio.spring.service;

import static org.springframework.util.CollectionUtils.isEmpty;

import feign.FeignException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.AuthnClient.UserCredentials;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.PermissionsClient.Permission;
import org.folio.spring.client.PermissionsClient.Permissions;
import org.folio.spring.client.UsersClient;
import org.folio.spring.client.UsersClient.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class PrepareSystemUserService {

  public static final String SYSTEM_USER_TYPE = "system";

  private final FolioExecutionContext folioExecutionContext;
  private final SystemUserProperties systemUserProperties;
  private final SystemUserService systemUserService;

  private UsersClient usersClient;
  private AuthnClient authnClient;
  private PermissionsClient permissionsClient;

  public void setupSystemUser() {
    if (!systemUserProperties.isEnabled()) {
      log.info("System user is disabled, skipping setup operation");
      return;
    }

    try {
      log.info("Preparing system user...");

      String userId = getOrCreateSystemUser();
      assignPermissions(userId);
      saveCredentials(userId);

      // a nice sanity check, to fail sooner and ensure later logins will go smoothly
      systemUserService.authSystemUser(folioExecutionContext.getTenantId(),
                                       systemUserProperties.username(), systemUserProperties.password());
      log.info("System user authenticated successfully");
    } catch (RuntimeException e) {
      log.error("Unexpected error while preparing system user with username={}:", systemUserProperties.username(), e);
      throw e;
    }

    log.info("Preparing system user is completed!");
  }

  /**
   * Gets or creates the system user (reactivating if needed).
   *
   * @return the system user's ID
   */
  private String getOrCreateSystemUser() {
    Optional<User> folioUser = systemUserService.getFolioUser(systemUserProperties.username());
    String userId = folioUser.map(User::getId).orElse(UUID.randomUUID().toString());

    if (folioUser.isPresent()) {
      User user = folioUser.get();
      log.info("Found existing system user, id={}", user.getId());

      if (!user.isActive()) {
        log.info("System user is inactive, attempting to mark active...");
        user = user.toBuilder().active(true).expirationDate(null).build();
        usersClient.updateUser(user);
      }
    } else {
      log.info("Could not find a system user with username={}, creating a new one with id={}...",
                systemUserProperties.username(), userId);
      createFolioUser(userId);
    }

    return userId;
  }

  private void createFolioUser(String id) {
    final User user = prepareUserObject(id);
    usersClient.createUser(user);
  }

  public void deleteCredentials(String userId) {
    authnClient.deleteCredentials(userId);

    log.info("Removed credentials for user {}.", userId);
  }

  /** Save the credentials for a user, removing an existing login if necessary. */
  private void saveCredentials(String id) {
    saveCredentials(id, true);
  }

  private void saveCredentials(String id, boolean clearExisting) {
    try {
      authnClient.saveCredentials(new UserCredentials(systemUserProperties.username(),
                                                      systemUserProperties.password()));

      log.info("Saved credentials for user with username={}", systemUserProperties.username());
    } catch (FeignException.UnprocessableEntity e) {
      if (!clearExisting) {
        throw e;
      }

      log.warn("Credentials already exist for user with username={}, removing them and re-adding...",
               systemUserProperties.username());

      deleteCredentials(id);
      saveCredentials(id, false);
    }
  }

  private void assignPermissions(String userId) {
    List<String> permissionsToAssign = getResourceLines(systemUserProperties.permissionsFilePath());
    log.info("Found {} permissions to assign: {}", permissionsToAssign.size(), permissionsToAssign);

    if (isEmpty(permissionsToAssign)) {
      throw log.throwing(
        new IllegalStateException("No permissions found to assign to system user; maybe the permissions file is empty?")
      );
    }

    try {
      List<String> existingPermissions = permissionsClient.getUserPermissions(userId).getResult();
      log.info("System user currently has {} permissions", existingPermissions.size());

      Set<String> permissionsToAdd = new HashSet<>(permissionsToAssign);
      existingPermissions.forEach(permissionsToAdd::remove);

      log.info("Adding {} permissions to system user: {}", permissionsToAdd.size(), permissionsToAdd);

      permissionsToAdd.forEach(permission -> permissionsClient.addPermission(userId, new Permission(permission)));
    } catch (FeignException.NotFound | NullPointerException e) {
      UUID newPermissionsId = UUID.randomUUID();
      log.warn("No permissions record found for system user, creating a fresh one with id={}...", newPermissionsId);
      Permissions permissions = new Permissions(UUID.randomUUID().toString(), userId, permissionsToAssign);

      permissionsClient.assignPermissionsToUser(permissions);
    }

    log.info("System user permission assignment successful");
  }

  private User prepareUserObject(String id) {
    return User.builder().id(id).username(systemUserProperties.username()).type(SYSTEM_USER_TYPE).active(true)
              .personal(new User.Personal(systemUserProperties.lastname())).build();
  }

  private List<String> getResourceLines(String permissionsFilePath) {
    try {
      ClassPathResource resource = new ClassPathResource(permissionsFilePath);
      List<String> lines = IOUtils.readLines(resource.getInputStream(), StandardCharsets.UTF_8);
      lines.removeIf(String::isBlank);
      return lines;
    } catch (IOException | UncheckedIOException e) {
      throw log.throwing(new IllegalArgumentException("Unable to open permissions file " + permissionsFilePath, e));
    }
  }

  @Autowired(required = false)
  public void setUsersClient(UsersClient usersClient) {
    this.usersClient = usersClient;
  }

  @Autowired(required = false)
  public void setAuthnClient(AuthnClient authnClient) {
    this.authnClient = authnClient;
  }

  @Autowired(required = false)
  public void setPermissionsClient(PermissionsClient permissionsClient) {
    this.permissionsClient = permissionsClient;
  }
}
