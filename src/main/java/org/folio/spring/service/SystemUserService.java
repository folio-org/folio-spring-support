package org.folio.spring.service;

import static org.folio.spring.client.AuthnClient.UserCredentials;
import static org.folio.spring.client.PermissionsClient.Permission;
import static org.folio.spring.client.PermissionsClient.Permissions;
import static org.folio.spring.client.UsersClient.User;
import static org.folio.spring.utils.ResourceUtil.getResourceLines;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.config.FolioSystemUserProperties;
import org.folio.spring.domain.SystemUser;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.repository.SystemUserRepository;
import org.springframework.util.CollectionUtils;

@Log4j2
@AllArgsConstructor
public class SystemUserService {
  private final PermissionsClient permissionsClient;
  private final UsersClient usersClient;
  private final AuthnClient authnClient;
  private final SystemUserRepository systemUserRepository;
  private final FolioSystemUserProperties folioSystemUserConf;
  private final FolioExecutionContext folioContext;

  public void prepareSystemUser() {
    var systemUserParameters = systemUserRepository
      .getByTenantId(folioContext.getTenantId())
      .orElse(buildDefaultSystemUserParameters());

    var folioUser = getFolioUser(systemUserParameters.getUsername());

    if (folioUser.isPresent()) {
      addPermissions(folioUser.get().getId());
    } else {
      var userId = createFolioUser();
      saveCredentials(systemUserParameters);
      assignPermissions(userId);
    }

    var backgroundUserApiKey = loginSystemUser(systemUserParameters);
    systemUserParameters.setOkapiToken(backgroundUserApiKey);
    saveSystemUserParameters(systemUserParameters);
  }

  private String loginSystemUser(SystemUser params) {
    var response = authnClient.getApiKey(UserCredentials
      .of(params.getUsername(), params.getPassword()));

    var headers = response.getHeaders().get(XOkapiHeaders.TOKEN);
    if (CollectionUtils.isEmpty(headers)) {
      throw new IllegalStateException(String.format("User [%s] cannot log in", params.getUsername()));
    } else {
      return headers.get(0);
    }
  }

  private SystemUser buildDefaultSystemUserParameters() {
    return SystemUser.builder()
      .id(UUID.randomUUID())
      .username(folioSystemUserConf.getUsername())
      .password(folioSystemUserConf.getPassword())
      .okapiUrl(folioContext.getOkapiUrl())
      .tenantId(folioContext.getTenantId()).build();
  }

  private void saveSystemUserParameters(SystemUser systemUserParams) {
    systemUserRepository.save(systemUserParams);
  }

  public SystemUser getSystemUserParameters(String tenantId) {
    return systemUserRepository.getByTenantId(tenantId)
      .orElseThrow(() -> new IllegalArgumentException("No system user for tenant " + tenantId));
  }

  private Optional<UsersClient.User> getFolioUser(String username) {
    var users = usersClient.query("username==" + username);
    return users.getUsers().stream().findFirst();
  }

  private String createFolioUser() {
    final var user = createUserObject();
    final var id = user.getId();
    usersClient.saveUser(user);
    return id;
  }

  private void saveCredentials(SystemUser parameters) {
    authnClient.saveCredentials(UserCredentials.of(parameters.getUsername(),
      parameters.getPassword()));

    log.info("Saved credentials for user: [{}]", parameters.getUsername());
  }

  private void assignPermissions(String userId) {
    List<String> perms = getResourceLines(folioSystemUserConf.getPermissionsFilePath());

    if (isEmpty(perms)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    var permissions = Permissions.of(UUID.randomUUID()
      .toString(), userId, perms);

    permissionsClient.assignPermissionsToUser(permissions);
  }

  private void addPermissions(String userId) {
    var permissions = getResourceLines(folioSystemUserConf.getPermissionsFilePath());

    if (isEmpty(permissions)) {
      throw new IllegalStateException("No permissions found to assign to user with id: " + userId);
    }

    permissions.forEach(permission -> {
      var p = new Permission();
      p.setPermissionName(permission);
      try {
        permissionsClient.addPermission(userId, p);
      } catch (Exception e) {
        log.info("Error adding permission {} to System User. Permission may be already assigned. Error was {}",
          permission, e);
      }
    });
  }

  private User createUserObject() {
    final var user = new User();

    user.setId(UUID.randomUUID()
      .toString());
    user.setActive(true);
    user.setUsername(folioSystemUserConf.getUsername());

    user.setPersonal(new User.Personal());
    user.getPersonal()
      .setLastName(folioSystemUserConf.getLastname());

    return user;
  }
}
