package org.folio.tenant.settings.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.settings.config.TenantSettingsProperties;
import org.folio.tenant.settings.exception.TenantSettingsUnauthorizedOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantSettingsPermissionCheckerTest {

  private static final String DOMAIN = "test-domain";

  @Mock
  private TenantSettingsProperties properties;

  @Mock
  private FolioExecutionContext context;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private TenantSettingsPermissionChecker permissionChecker;

  @BeforeEach
  void setUp() {
    when(properties.getDomain()).thenReturn(DOMAIN);
  }

  @Test
  void checkPermission_shouldPassWhenPermissionCheckDisabled() {
    when(properties.isPermissionCheckEnabled()).thenReturn(false);

    permissionChecker.checkPermission("test-group");

    verify(context, never()).getOkapiHeaders();
  }

  @Test
  void checkPermission_shouldPassWhenUserHasRequiredPermission() throws Exception {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    var permissions = List.of("test-domain.config.groups.settings.test-group.collection.get");
    setupPermissions(permissions);

    permissionChecker.checkPermission("test-group");

    verify(objectMapper).readValue(any(String.class), any(TypeReference.class));
  }

  @Test
  void checkPermission_shouldThrowWhenUserLacksRequiredPermission() throws Exception {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    var permissions = List.of("other.permission");
    setupPermissions(permissions);

    assertThatThrownBy(() -> permissionChecker.checkPermission("test-group"))
      .isInstanceOf(TenantSettingsUnauthorizedOperationException.class)
      .hasMessageContaining("test-domain.config.groups.settings.test-group.collection.get");
  }

  @Test
  void checkPermission_shouldThrowWhenNoPermissionsProvided() throws Exception {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    setupPermissions(Collections.emptyList());

    assertThatThrownBy(() -> permissionChecker.checkPermission("test-group"))
      .isInstanceOf(TenantSettingsUnauthorizedOperationException.class);
  }

  @Test
  void checkPermission_shouldThrowWhenEmptyPermissionsProvided() {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    setupEmptyPermissions();

    assertThatThrownBy(() -> permissionChecker.checkPermission("test-group"))
      .isInstanceOf(TenantSettingsUnauthorizedOperationException.class);
  }

  @Test
  void checkPermissionForKey_shouldPassWhenUserHasRequiredPermission() throws Exception {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    var permissions = List.of("test-domain.config.groups.settings.test-group.test-key.item.patch");
    setupPermissions(permissions);

    permissionChecker.checkPermission("test-group", "test-key");

    verify(objectMapper).readValue(any(String.class), any(TypeReference.class));
  }

  @Test
  void checkPermissionForKey_shouldThrowWhenUserLacksRequiredPermission() throws Exception {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    var permissions = List.of("other.permission");
    setupPermissions(permissions);

    assertThatThrownBy(() -> permissionChecker.checkPermission("test-group", "test-key"))
      .isInstanceOf(TenantSettingsUnauthorizedOperationException.class)
      .hasMessageContaining("test-domain.config.groups.settings.test-group.test-key.item.patch");
  }

  @Test
  void checkPermission_shouldHandleMissingPermissionsHeader() {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    when(context.getOkapiHeaders()).thenReturn(Map.of());

    assertThatThrownBy(() -> permissionChecker.checkPermission("test-group"))
      .isInstanceOf(TenantSettingsUnauthorizedOperationException.class);
  }

  @Test
  void checkPermission_shouldHandleInvalidJsonInPermissionsHeader() throws Exception {
    when(properties.isPermissionCheckEnabled()).thenReturn(true);
    when(context.getOkapiHeaders()).thenReturn(Map.of(XOkapiHeaders.PERMISSIONS, List.of("invalid-json")));
    when(objectMapper.readValue(any(String.class), any(TypeReference.class)))
      .thenThrow(new JacksonException("Invalid JSON") { });

    assertThatThrownBy(() -> permissionChecker.checkPermission("test-group"))
      .isInstanceOf(TenantSettingsUnauthorizedOperationException.class);
  }

  private void setupPermissions(List<String> permissions) {
    when(context.getOkapiHeaders()).thenReturn(
      Map.of(XOkapiHeaders.PERMISSIONS, List.of("[\"" + String.join("\",\"", permissions) + "\"]")));
    when(objectMapper.readValue(any(String.class), any(TypeReference.class)))
      .thenReturn(permissions);
  }

  private void setupEmptyPermissions() {
    when(context.getOkapiHeaders()).thenReturn(
      Map.of(XOkapiHeaders.PERMISSIONS, Collections.emptyList()));
  }
}
