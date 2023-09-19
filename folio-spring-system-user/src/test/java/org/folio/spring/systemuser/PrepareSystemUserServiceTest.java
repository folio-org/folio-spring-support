package org.folio.spring.systemuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.model.ResultList.asSinglePage;
import static org.folio.spring.service.PrepareSystemUserService.SYSTEM_USER_TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.PermissionsClient.Permission;
import org.folio.spring.client.UsersClient;
import org.folio.spring.client.UsersClient.User.Personal;
import org.folio.spring.model.ResultList;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrepareSystemUserServiceTest {

  @Mock
  private UsersClient usersClient;
  @Mock
  private AuthnClient authnClient;
  @Mock
  private PermissionsClient permissionsClient;

  @Captor
  private ArgumentCaptor<UsersClient.User> userArgumentCaptor;

  private static SystemUserProperties systemUserProperties() {
    return new SystemUserProperties("username", "password", "system", "permissions/test-permissions.csv");
  }

  private static SystemUserProperties systemUserPropertiesWithoutPermissions() {
    return new SystemUserProperties("username", "password", "system", "permissions/empty-permissions.csv");
  }

  @Test
  void shouldCreateSystemUserWhenNotExist() {
    when(usersClient.query(any())).thenReturn(userNotExistResponse());

    prepareSystemUser(systemUserProperties());

    verify(usersClient).saveUser(userArgumentCaptor.capture());
    verify(permissionsClient).assignPermissionsToUser(any());

    assertThat(userArgumentCaptor.getValue())
      .extracting("username", "active", "personal.lastName")
      .containsExactly(systemUserProperties().username(), true, systemUserProperties().lastname());
  }

  @Test
  void shouldCreateSystemUserWhenNullResponse() {
    when(usersClient.query(any())).thenReturn(null);

    prepareSystemUser(systemUserProperties());

    verify(usersClient).saveUser(any());
    verify(permissionsClient).assignPermissionsToUser(any());
  }

  @Test
  void shouldNotCreateSystemUserWhenExists() {
    when(usersClient.query(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any())).thenReturn(ResultList.empty());

    prepareSystemUser(systemUserProperties());

    verify(permissionsClient, times(2)).addPermission(any(), any());
  }

  @Test
  void cannotUpdateUserIfEmptyPermissions() {
    var systemUser = systemUserPropertiesWithoutPermissions();
    when(usersClient.query(any())).thenReturn(userNotExistResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));

    verifyNoInteractions(permissionsClient);
  }

  @Test
  void cannotCreateUserIfEmptyPermissions() {
    var systemUser = systemUserPropertiesWithoutPermissions();
    when(usersClient.query(any())).thenReturn(userExistsResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));
  }

  @Test
  void shouldAddOnlyNewPermissions() {
    when(usersClient.query(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any()))
      .thenReturn(asSinglePage("inventory-storage.instance.item.get"));

    prepareSystemUser(systemUserProperties());

    verify(permissionsClient, times(1)).addPermission(any(), any());
    verify(permissionsClient, times(0))
      .addPermission(any(), eq(new Permission("inventory-storage.instance.item.get")));
    verify(permissionsClient, times(1))
      .addPermission(any(), eq(new Permission("inventory-storage.instance.item.post")));
  }

  private ResultList<UsersClient.User> userExistsResponse() {
    return asSinglePage(new UsersClient.User("id", "username", SYSTEM_USER_TYPE, true,
      new Personal("lastName")));
  }

  private ResultList<UsersClient.User> userNotExistResponse() {
    return ResultList.empty();
  }

  private PrepareSystemUserService systemUserService(SystemUserProperties properties) {
    return new PrepareSystemUserService(usersClient, authnClient, permissionsClient, properties);
  }

  private void prepareSystemUser(SystemUserProperties properties) {
    systemUserService(properties).setupSystemUser();
  }

}
