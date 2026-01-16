package org.folio.tenant.settings.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.domain.dto.SettingGroupCollection;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.mapper.TenantSettingsMapper;
import org.folio.tenant.settings.repository.SettingGroupRepository;
import org.folio.tenant.settings.repository.SettingRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing tenant settings and setting groups.
 * Provides business logic for retrieving and updating settings.
 */
@Log4j2
@Service
@NullMarked
@RequiredArgsConstructor
public class TenantSettingsService {

  private final SettingGroupRepository settingGroupRepository;
  private final SettingRepository settingRepository;
  private final FolioExecutionContext context;
  private final TenantSettingsMapper mapper;
  private final TenantSettingsValidator validator;

  /**
   * Retrieves all configuration groups.
   *
   * @return collection of all setting groups
   */
  public SettingGroupCollection getConfigGroups() {
    log.debug("Fetching all configuration groups from database");

    var entities = settingGroupRepository.findAll();
    var groups = entities.stream()
      .map(mapper::toDto)
      .toList();

    return new SettingGroupCollection(groups, groups.size());
  }

  /**
   * Retrieves all settings for a specific group.
   *
   * @param groupId the group identifier
   * @return optional containing the setting collection if group exists
   */
  public Optional<SettingCollection> getGroupSettings(String groupId) {
    log.debug("Fetching settings for group: {}", groupId);

    if (!settingGroupRepository.existsById(groupId)) {
      log.warn("Group not found: {}", groupId);
      return Optional.empty();
    }

    var entities = settingRepository.findByGroupId(groupId);
    var settings = entities.stream()
      .map(mapper::toDto)
      .toList();

    return Optional.of(new SettingCollection(settings, settings.size()));
  }

  /**
   * Updates a specific setting in a group.
   *
   * @param groupId the group identifier
   * @param key the setting key
   * @param updateRequest the update request with new values
   * @return optional containing the updated setting if found
   */
  @Transactional
  public Optional<Setting> updateGroupSetting(String groupId, String key, SettingUpdateRequest updateRequest) {
    log.debug("Updating setting: {} in group: {}", key, groupId);

    var entityOpt = settingRepository.findByGroupIdAndKey(groupId, key);
    if (entityOpt.isEmpty()) {
      log.warn("Setting not found: {} in group: {}", key, groupId);
      return Optional.empty();
    }
    var entity = entityOpt.get();

    validator.validate(updateRequest, entity);

    mapper.updateEntity(entity, updateRequest);
    entity.setUpdatedDate(OffsetDateTime.now());
    entity.setUpdatedByUserId(context.getUserId());

    var savedEntity = settingRepository.save(entity);
    log.info("Setting updated: {} in group: {}", key, groupId);

    return Optional.of(mapper.toDto(savedEntity));
  }
}
