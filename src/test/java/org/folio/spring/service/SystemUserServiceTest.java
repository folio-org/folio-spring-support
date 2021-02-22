package org.folio.spring.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.config.FolioSystemUserProperties;
import org.folio.spring.domain.SystemUser;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.repository.SystemUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {
  @Mock
  private UsersClient usersClient;
  @Mock
  private AuthnClient authnClient;
  @Mock
  private PermissionsClient permissionsClient;
  @Mock
  private FolioExecutionContext executionContext;
  @Mock
  private SystemUserRepository repository;

  @Test
  void shouldCreateSystemUserWhenNotExist() {
    when(repository.getByTenantId(any())).thenReturn(Optional.empty());
    when(usersClient.query(any())).thenReturn(userNotExistResponse());
    when(authnClient.getApiKey(any())).thenReturn(ResponseEntity.status(200)
      .header(XOkapiHeaders.TOKEN, "token").build());

    prepareSystemUser(systemUser());

    verify(usersClient).saveUser(any());
    verify(permissionsClient).assignPermissionsToUser(any());

    var captor = ArgumentCaptor.forClass(SystemUser.class);
    verify(repository).save(captor.capture());

    assertThat(captor.getValue().getUsername(), is(systemUser().getUsername()));
    assertThat(captor.getValue().getPassword(), is(systemUser().getPassword()));
    assertThat(captor.getValue().getOkapiToken(), is("token"));
  }

  @Test
  void shouldNotCreateSystemUserWhenExists() {
    when(repository.getByTenantId(any())).thenReturn(Optional.empty());
    when(usersClient.query(any())).thenReturn(userExistsResponse());
    when(authnClient.getApiKey(any())).thenReturn(ResponseEntity.status(200)
      .header(XOkapiHeaders.TOKEN, "token2").build());

    prepareSystemUser(systemUser());

    verify(permissionsClient, times(2)).addPermission(any(), any());

    var captor = ArgumentCaptor.forClass(SystemUser.class);
    verify(repository).save(captor.capture());

    assertThat(captor.getValue().getUsername(), is(systemUser().getUsername()));
    assertThat(captor.getValue().getPassword(), is(systemUser().getPassword()));
    assertThat(captor.getValue().getOkapiToken(), is("token2"));
  }

  @Test
  void cannotAssignPermissionsIfEmptyFile() {
    when(repository.getByTenantId(any())).thenReturn(Optional.empty());
    when(usersClient.query(any())).thenReturn(userNotExistResponse());

    assertThrows(IllegalStateException.class,
      () -> prepareSystemUser(systemUserNoPermissions()));

    verify(usersClient).saveUser(any());
    verifyNoInteractions(permissionsClient);
    verify(repository, times(0)).save(any());
  }

  @Test
  void cannotAddPermissionsIfEmptyFile() {
    when(repository.getByTenantId(any())).thenReturn(Optional.empty());
    when(usersClient.query(any())).thenReturn(userExistsResponse());

    assertThrows(IllegalStateException.class,
      () -> prepareSystemUser(systemUserNoPermissions()));

    verify(usersClient).query(any());
    verifyNoInteractions(permissionsClient);
    verify(repository, times(0)).save(any());
  }

  @Test
  void shouldIgnoreErrorWhenPermissionExists() {
    when(repository.getByTenantId(any())).thenReturn(Optional.empty());
    when(usersClient.query(any())).thenReturn(userExistsResponse());
    when(authnClient.getApiKey(any())).thenReturn(ResponseEntity.status(200)
      .header(XOkapiHeaders.TOKEN, "token").build());

    var permission = PermissionsClient.Permission.of("inventory-storage.instance.item.get");
    doThrow(new RuntimeException("Permission exists"))
      .when(permissionsClient).addPermission(any(), eq(permission));

    prepareSystemUser(systemUser());

    verify(repository).save(any());
  }

  @Test
  void shouldReturnSystemUserFromRepository() {
    when(repository.getByTenantId(any()))
      .thenReturn(Optional.of(new SystemUser()));

    var user = systemUserService(null).getSystemUserParameters("tenant");

    assertThat(user, notNullValue());
  }

  @Test
  void shouldThrowExceptionIfNoUser() {
    when(repository.getByTenantId(any())).thenReturn(Optional.empty());

    var systemUserService = systemUserService(null);

    assertThrows(IllegalArgumentException.class,
      () -> systemUserService.getSystemUserParameters("tenant"));
  }

  private FolioSystemUserProperties systemUser() {
    return FolioSystemUserProperties.builder()
      .password("password")
      .username("username")
      .permissionsFilePath("classpath:user-permissions.csv")
      .build();
  }

  private FolioSystemUserProperties systemUserNoPermissions() {
    return FolioSystemUserProperties.builder()
      .password("password")
      .username("username")
      .permissionsFilePath("classpath:empty-permissions.csv")
      .build();
  }

  private UsersClient.Users userExistsResponse() {
    return UsersClient.Users.builder()
      .user(new UsersClient.User())
      .build();
  }

  private UsersClient.Users userNotExistResponse() {
    return new UsersClient.Users();
  }

  private SystemUserService systemUserService(FolioSystemUserProperties properties) {
    return new SystemUserService(permissionsClient, usersClient, authnClient,
      repository, properties, executionContext);
  }

  private void prepareSystemUser(FolioSystemUserProperties properties) {
    systemUserService(properties).prepareSystemUser();
  }
}
