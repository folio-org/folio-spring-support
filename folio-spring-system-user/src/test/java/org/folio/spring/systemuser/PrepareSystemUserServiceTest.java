package org.folio.spring.systemuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.model.ResultList.asSinglePage;
import static org.folio.spring.service.PrepareSystemUserService.SYSTEM_USER_TYPE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.Map;
import java.util.Optional;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.PermissionsClient.Permission;
import org.folio.spring.client.UsersClient;
import org.folio.spring.client.UsersClient.User.Personal;
import org.folio.spring.model.ResultList;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserProperties;
import org.folio.spring.service.SystemUserService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PrepareSystemUserServiceTest {

  @Mock
  private FolioExecutionContext context;

  @Mock
  private UsersClient usersClient;

  @Mock
  private AuthnClient authnClient;

  @Mock
  private PermissionsClient permissionsClient;

  @Mock
  private SystemUserService systemUserService;

  @Captor
  private ArgumentCaptor<UsersClient.User> userArgumentCaptor;

  private static SystemUserProperties systemUserProperties() {
    return systemUserProperties(true);
  }

  private static SystemUserProperties systemUserProperties(boolean enabled) {
    return new SystemUserProperties(enabled, "username", "password", "system", "permissions/test-permissions.csv");
  }

  private static SystemUserProperties systemUserPropertiesWithoutPermissions() {
    return new SystemUserProperties("username", "password", "system", "permissions/empty-permissions.csv");
  }

  @Test
  void testCreatesSystemUserWhenNotExist() {
    when(systemUserService.getFolioUser(any())).thenReturn(userNotExistResponse());
    when(permissionsClient.getUserPermissions(any())).thenThrow(mock(FeignException.NotFound.class));

    prepareSystemUser(systemUserProperties());

    verify(usersClient, times(1)).createUser(userArgumentCaptor.capture());
    verify(permissionsClient, times(1)).getUserPermissions(any());
    verify(permissionsClient, times(1)).assignPermissionsToUser(any());
    verifyNoMoreInteractions(usersClient, permissionsClient);

    assertThat(userArgumentCaptor.getValue())
      .extracting("username", "active", "personal.lastName")
      .containsExactly(systemUserProperties().username(), true, systemUserProperties().lastname());
  }

  @Test
  void testNotCreateSystemUserWhenExistsAndIsActive() {
    when(systemUserService.getFolioUser(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any())).thenReturn(ResultList.empty());

    prepareSystemUser(systemUserProperties());

    verify(permissionsClient, times(2)).addPermission(any(), any());
  }

  @Test
  void testActivateSystemUserWhenInactive() {
    when(systemUserService.getFolioUser(any())).thenReturn(userExistsInactiveResponse());
    when(permissionsClient.getUserPermissions(any())).thenReturn(ResultList.empty());

    prepareSystemUser(systemUserProperties());

    verify(usersClient, times(1)).updateUser(userArgumentCaptor.capture());
    verifyNoMoreInteractions(usersClient);

    assertThat(userArgumentCaptor.getValue())
      .extracting("username", "active", "expirationDate", "extraProperties.foo")
      .containsExactly(systemUserProperties().username(), true, null, "bar");
  }

  @Test
  void cannotUpdateUserIfEmptyPermissions() {
    SystemUserProperties systemUser = systemUserPropertiesWithoutPermissions();
    when(systemUserService.getFolioUser(any())).thenReturn(userNotExistResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));

    verifyNoInteractions(permissionsClient);
  }

  @Test
  void cannotCreateUserIfEmptyPermissions() {
    SystemUserProperties systemUser = systemUserPropertiesWithoutPermissions();
    when(systemUserService.getFolioUser(any())).thenReturn(userExistsResponse());

    assertThrows(IllegalStateException.class, () -> prepareSystemUser(systemUser));
  }

  @Test
  void testAddOnlyNewPermissions() {
    when(systemUserService.getFolioUser(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any()))
      .thenReturn(asSinglePage("inventory-storage.instance.item.get"));

    prepareSystemUser(systemUserProperties());

    verify(permissionsClient, times(1))
      .addPermission(any(), eq(new Permission("inventory-storage.instance.item.post")));
    verifyNoMoreInteractions(permissionsClient);
  }

  @Test
  void testUpdateCredentialsForAnExistingUserWithNoExistingCredentials() {
    when(systemUserService.getFolioUser(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any()))
      .thenReturn(asSinglePage("inventory-storage.instance.item.get"));

    prepareSystemUser(systemUserProperties());

    verify(authnClient, times(1)).saveCredentials(any());
    verifyNoMoreInteractions(authnClient);
  }

  @Test
  void testUpdateCredentialsForAnExistingUserWithExistingCredentials() {
    when(systemUserService.getFolioUser(any())).thenReturn(userExistsResponse());
    when(permissionsClient.getUserPermissions(any())).thenReturn(asSinglePage("inventory-storage.instance.item.get"));

    // fail on the 1st try
    doThrow(mock(FeignException.UnprocessableEntity.class))
      .doNothing()
      .when(authnClient).saveCredentials(any());

    prepareSystemUser(systemUserProperties());

    InOrder verifier = inOrder(authnClient);
    verifier.verify(authnClient, times(1)).saveCredentials(any());
    verifier.verify(authnClient, times(1)).deleteCredentials(any());
    verifier.verify(authnClient, times(1)).saveCredentials(any());
    verifier.verifyNoMoreInteractions();
  }

  @Test
  void testNotCreateSystemUserIfDisabled() {
    prepareSystemUser(systemUserProperties(false));
    verifyNoInteractions(authnClient, permissionsClient, usersClient);
  }

  private Optional<UsersClient.User> userExistsResponse() {
    return Optional.of(UsersClient.User.builder().id("id").username("username").type(SYSTEM_USER_TYPE).active(true)
              .personal(new Personal("lastName")).build());
  }

  private Optional<UsersClient.User> userExistsInactiveResponse() {
    return Optional.of(UsersClient.User.builder().id("id").username("username").type(SYSTEM_USER_TYPE).active(false)
              .expirationDate("yesterday").personal(new Personal("lastName"))
              .extraProperties(Map.of("foo", "bar")).build());
  }

  private Optional<UsersClient.User> userNotExistResponse() {
    return Optional.empty();
  }

  private PrepareSystemUserService systemUserService(SystemUserProperties properties) {
    var prepareSystemUserService = new PrepareSystemUserService(context, properties, systemUserService);
    prepareSystemUserService.setAuthnClient(authnClient);
    prepareSystemUserService.setUsersClient(usersClient);
    prepareSystemUserService.setPermissionsClient(permissionsClient);
    return prepareSystemUserService;
  }

  private void prepareSystemUser(SystemUserProperties properties) {
    systemUserService(properties).setupSystemUser();
  }
}
