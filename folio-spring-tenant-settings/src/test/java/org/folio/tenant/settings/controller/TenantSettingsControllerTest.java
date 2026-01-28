package org.folio.tenant.settings.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Metadata;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.domain.dto.SettingGroup;
import org.folio.tenant.domain.dto.SettingGroupCollection;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.exception.TenantSettingsUnauthorizedOperationException;
import org.folio.tenant.settings.exception.TenantSettingsValidationException;
import org.folio.tenant.settings.service.TenantSettingsPermissionChecker;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@WebMvcTest(TenantSettingsController.class)
@Import(TenantSettingsControllerTestConfiguration.class)
@TestPropertySource(properties = {
  "folio.tenant.settings.domain=authorities"
})
class TenantSettingsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private TenantSettingsService tenantSettingsService;

  @MockitoBean
  private TenantSettingsPermissionChecker permissionChecker;

  @Test
  void getConfigGroups_shouldReturnAllGroups() throws Exception {
    var group1 = new SettingGroup()
      .id("authorities")
      .name("Authorities Settings");
    var group2 = new SettingGroup()
      .id("audit.authority")
      .name("Audit Authority Settings");

    var collection = new SettingGroupCollection()
      .settingGroups(List.of(group1, group2))
      .totalRecords(2);

    when(tenantSettingsService.getConfigGroups()).thenReturn(collection);

    mockMvc.perform(get("/authorities/config/groups")
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.settingGroups", hasSize(2)))
      .andExpect(jsonPath("$.settingGroups[0].id", is("authorities")))
      .andExpect(jsonPath("$.settingGroups[0].name", is("Authorities Settings")))
      .andExpect(jsonPath("$.settingGroups[1].id", is("audit.authority")))
      .andExpect(jsonPath("$.totalRecords", is(2)));

    verify(tenantSettingsService).getConfigGroups();
  }

  @Test
  void getGroupSettings_shouldReturnSettingsWhenFound() throws Exception {
    var groupId = "authorities";
    var userId = UUID.randomUUID();

    var setting1 = new Setting()
      .key("mapping.extended")
      .value("true")
      .type(Setting.TypeEnum.BOOLEAN)
      .groupId(groupId)
      .metadata(new Metadata()
        .createdDate(OffsetDateTime.parse("2026-01-16T10:00:00Z"))
        .createdByUserId(userId));

    var collection = new SettingCollection()
      .settings(List.of(setting1))
      .totalRecords(1);

    when(tenantSettingsService.getGroupSettings(groupId)).thenReturn(Optional.of(collection));

    mockMvc.perform(get("/authorities/config/groups/{groupId}/settings", groupId)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.settings", hasSize(1)))
      .andExpect(jsonPath("$.settings[0].key", is("mapping.extended")))
      .andExpect(jsonPath("$.settings[0].value", is("true")))
      .andExpect(jsonPath("$.settings[0].type", is("BOOLEAN")))
      .andExpect(jsonPath("$.totalRecords", is(1)));

    verify(permissionChecker).checkPermission(groupId);
    verify(tenantSettingsService).getGroupSettings(groupId);
  }

  @Test
  void getGroupSettings_shouldReturnNotFoundWhenGroupDoesNotExist() throws Exception {
    var groupId = "nonexistent";

    when(tenantSettingsService.getGroupSettings(groupId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/authorities/config/groups/{groupId}/settings", groupId)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNotFound());

    verify(permissionChecker).checkPermission(groupId);
    verify(tenantSettingsService).getGroupSettings(groupId);
  }

  @Test
  void updateGroupSetting_shouldReturnUpdatedSettingWhenFound() throws Exception {
    var groupId = "authorities";
    var key = "mapping.extended";
    var userId = UUID.randomUUID();
    var updateRequest = new SettingUpdateRequest().value("false");

    var updatedSetting = new Setting()
      .key(key)
      .value("false")
      .type(Setting.TypeEnum.BOOLEAN)
      .groupId(groupId)
      .metadata(new Metadata()
        .createdDate(OffsetDateTime.parse("2026-01-16T10:00:00Z"))
        .createdByUserId(userId)
        .updatedDate(OffsetDateTime.parse("2026-01-16T12:00:00Z"))
        .updatedByUserId(userId));

    when(tenantSettingsService.updateGroupSetting(eq(groupId), eq(key), any(SettingUpdateRequest.class)))
      .thenReturn(Optional.of(updatedSetting));

    mockMvc.perform(patch("/authorities/config/groups/{groupId}/settings/{key}", groupId, key)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.key", is(key)))
      .andExpect(jsonPath("$.value", is("false")))
      .andExpect(jsonPath("$.type", is("BOOLEAN")))
      .andExpect(jsonPath("$.metadata.updatedDate").exists());

    verify(permissionChecker).checkPermission(groupId, key);
    verify(tenantSettingsService).updateGroupSetting(eq(groupId), eq(key), any(SettingUpdateRequest.class));
  }

  @Test
  void updateGroupSetting_shouldReturnNotFoundWhenSettingDoesNotExist() throws Exception {
    var groupId = "authorities";
    var key = "nonexistent.key";
    var updateRequest = new SettingUpdateRequest().value("value");

    when(tenantSettingsService.updateGroupSetting(eq(groupId), eq(key), any(SettingUpdateRequest.class)))
      .thenReturn(Optional.empty());

    mockMvc.perform(patch("/authorities/config/groups/{groupId}/settings/{key}", groupId, key)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isNotFound());

    verify(permissionChecker).checkPermission(groupId, key);
    verify(tenantSettingsService).updateGroupSetting(eq(groupId), eq(key), any(SettingUpdateRequest.class));
  }

  @Test
  void getGroupSettings_shouldReturnForbiddenWhenUnauthorized() throws Exception {
    var groupId = "authorities";
    var requiredPermission = "authorities.config.groups.settings.authorities.collection.get";

    doThrow(new TenantSettingsUnauthorizedOperationException(requiredPermission))
      .when(permissionChecker).checkPermission(groupId);

    mockMvc.perform(get("/authorities/config/groups/{groupId}/settings", groupId)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.errors", hasSize(1)))
      .andExpect(jsonPath("$.errors[0].message",
        is("Permission '" + requiredPermission + "' required to perform this operation")));

    verify(permissionChecker).checkPermission(groupId);
  }

  @Test
  void updateGroupSetting_shouldReturnUnprocessableEntityWhenValidationFails() throws Exception {
    var groupId = "authorities";
    var key = "mapping.extended";
    var errorMessage = "Invalid value for BOOLEAN type";
    var updateRequest = new SettingUpdateRequest().value("invalid");

    when(tenantSettingsService.updateGroupSetting(eq(groupId), eq(key), any(SettingUpdateRequest.class)))
      .thenThrow(new TenantSettingsValidationException(errorMessage));

    mockMvc.perform(patch("/authorities/config/groups/{groupId}/settings/{key}", groupId, key)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(updateRequest)))
      .andExpect(status().isUnprocessableContent())
      .andExpect(jsonPath("$.errors", hasSize(1)))
      .andExpect(jsonPath("$.errors[0].message", is(errorMessage)));

    verify(permissionChecker).checkPermission(groupId, key);
  }
}
