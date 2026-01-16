package org.folio.tenant.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingGroup;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.entity.SettingEntity;
import org.folio.tenant.settings.entity.SettingGroupEntity;
import org.folio.tenant.settings.mapper.TenantSettingsMapper;
import org.folio.tenant.settings.repository.SettingGroupRepository;
import org.folio.tenant.settings.repository.SettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantSettingsServiceTest {

  @Mock
  private SettingGroupRepository settingGroupRepository;

  @Mock
  private SettingRepository settingRepository;

  @Mock
  private FolioExecutionContext context;

  @Mock
  private TenantSettingsMapper mapper;

  @Mock
  private TenantSettingsValidator validator;

  @InjectMocks
  private TenantSettingsService service;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
  }

  @Test
  void getConfigGroups_shouldReturnAllGroups() {
    var group1 = new SettingGroupEntity();
    group1.setId("group1");
    group1.setName("Group 1");

    var group2 = new SettingGroupEntity();
    group2.setId("group2");
    group2.setName("Group 2");

    var groupDto1 = new SettingGroup().id("group1").name("Group 1");
    var groupDto2 = new SettingGroup().id("group2").name("Group 2");

    when(settingGroupRepository.findAll()).thenReturn(List.of(group1, group2));
    when(mapper.toDto(group1)).thenReturn(groupDto1);
    when(mapper.toDto(group2)).thenReturn(groupDto2);

    var result = service.getConfigGroups();

    assertThat(result.getTotalRecords()).isEqualTo(2);
    assertThat(result.getSettingGroups()).containsExactly(groupDto1, groupDto2);
  }

  @Test
  void getConfigGroups_shouldReturnEmptyWhenNoGroups() {
    when(settingGroupRepository.findAll()).thenReturn(List.of());

    var result = service.getConfigGroups();

    assertThat(result.getTotalRecords()).isZero();
    assertThat(result.getSettingGroups()).isEmpty();
  }

  @Test
  void getGroupSettings_shouldReturnSettingsForExistingGroup() {
    var groupId = "test-group";
    when(settingGroupRepository.existsById(groupId)).thenReturn(true);

    var setting1 = new SettingEntity();
    setting1.setKey("key1");
    setting1.setGroupId(groupId);

    var setting2 = new SettingEntity();
    setting2.setKey("key2");
    setting2.setGroupId(groupId);

    var settingDto1 = new Setting().key("key1").groupId(groupId);
    var settingDto2 = new Setting().key("key2").groupId(groupId);

    when(settingRepository.findByGroupId(groupId)).thenReturn(List.of(setting1, setting2));
    when(mapper.toDto(setting1)).thenReturn(settingDto1);
    when(mapper.toDto(setting2)).thenReturn(settingDto2);

    var result = service.getGroupSettings(groupId);

    assertThat(result).isPresent();
    assertThat(result.get().getTotalRecords()).isEqualTo(2);
    assertThat(result.get().getSettings()).containsExactly(settingDto1, settingDto2);
  }

  @Test
  void getGroupSettings_shouldReturnEmptyForNonExistentGroup() {
    var groupId = "non-existent";
    when(settingGroupRepository.existsById(groupId)).thenReturn(false);

    var result = service.getGroupSettings(groupId);

    assertThat(result).isEmpty();
  }

  @Test
  void updateGroupSetting_shouldUpdateExistingSetting() {
    var groupId = "test-group";
    var key = "test-key";
    var entity = new SettingEntity();
    entity.setId(UUID.randomUUID());
    entity.setKey(key);
    entity.setGroupId(groupId);
    entity.setValue("old-value");
    entity.setType(SettingEntity.SettingType.STRING);

    var updateRequest = new SettingUpdateRequest().value("new-value");
    var settingDto = new Setting().key(key).groupId(groupId).value("new-value");

    when(context.getUserId()).thenReturn(userId);
    when(settingRepository.findByGroupIdAndKey(groupId, key)).thenReturn(Optional.of(entity));
    when(settingRepository.save(any(SettingEntity.class))).thenReturn(entity);
    when(mapper.toDto(entity)).thenReturn(settingDto);

    var result = service.updateGroupSetting(groupId, key, updateRequest);

    assertThat(result).isPresent().contains(settingDto);
    verify(validator).validate(updateRequest, entity);
    verify(settingRepository).save(entity);
  }

  @Test
  void updateGroupSetting_shouldReturnEmptyForNonExistentSetting() {
    var groupId = "test-group";
    var key = "non-existent-key";
    var updateRequest = new SettingUpdateRequest().value("new-value");

    when(settingRepository.findByGroupIdAndKey(groupId, key)).thenReturn(Optional.empty());

    var result = service.updateGroupSetting(groupId, key, updateRequest);

    assertThat(result).isEmpty();
    verifyNoMoreInteractions(settingRepository);
  }

  @Test
  void updateGroupSetting_shouldSetUpdatedDateAndUser() {
    var groupId = "test-group";
    var key = "test-key";
    var entity = new SettingEntity();
    entity.setId(UUID.randomUUID());
    entity.setKey(key);
    entity.setGroupId(groupId);
    entity.setType(SettingEntity.SettingType.BOOLEAN);

    var settingDto = new Setting().key(key).groupId(groupId).value(true);

    when(context.getUserId()).thenReturn(userId);
    when(settingRepository.findByGroupIdAndKey(groupId, key)).thenReturn(Optional.of(entity));
    when(settingRepository.save(any(SettingEntity.class))).thenAnswer(invocation -> {
      SettingEntity savedEntity = invocation.getArgument(0);
      assertThat(savedEntity.getUpdatedDate()).isNotNull();
      assertThat(savedEntity.getUpdatedByUserId()).isEqualTo(userId);
      return savedEntity;
    });
    when(mapper.toDto(entity)).thenReturn(settingDto);

    var updateRequest = new SettingUpdateRequest().value(true);
    service.updateGroupSetting(groupId, key, updateRequest);

    verify(settingRepository).save(entity);
  }
}
