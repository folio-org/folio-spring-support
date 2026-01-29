package org.folio.tenant.settings.mapper;

import org.folio.tenant.domain.dto.Metadata;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingGroup;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.entity.SettingEntity;
import org.folio.tenant.settings.entity.SettingGroupEntity;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between tenant settings entities and DTOs.
 */
@Component
@NullMarked
public class TenantSettingsMapper {

  /**
   * Converts a setting group entity to a DTO.
   *
   * @param entity the setting group entity
   * @return the setting group DTO
   */
  public SettingGroup toDto(SettingGroupEntity entity) {
    return new SettingGroup()
      .id(entity.getId())
      .name(entity.getName())
      .description(entity.getDescription());
  }

  /**
   * Converts a setting entity to a DTO.
   *
   * @param entity the setting entity
   * @return the setting DTO
   */
  public Setting toDto(SettingEntity entity) {
    var metadata = new Metadata()
      .createdDate(entity.getCreatedDate())
      .createdByUserId(entity.getCreatedByUserId())
      .updatedDate(entity.getUpdatedDate())
      .updatedByUserId(entity.getUpdatedByUserId());

    return new Setting()
      .key(entity.getKey())
      .value(convertValueFromDb(entity.getValue(), entity.getType()))
      .type(Setting.TypeEnum.valueOf(entity.getType().name()))
      .description(entity.getDescription())
      .groupId(entity.getGroupId())
      .metadata(metadata);
  }

  /**
   * Updates a setting entity with values from the update request.
   *
   * @param entity the setting entity to update
   * @param dto the update request containing new values
   */
  public void updateEntity(SettingEntity entity, SettingUpdateRequest dto) {
    if (dto.getValue() != null) {
      entity.setValue(dto.getValue().toString());
    }

    if (dto.getDescription() != null) {
      entity.setDescription(dto.getDescription());
    }
  }

  private @Nullable Object convertValueFromDb(@Nullable String value, SettingEntity.SettingType type) {
    if (value == null) {
      return null;
    }
    return switch (type) {
      case BOOLEAN -> Boolean.valueOf(value);
      case INTEGER -> Integer.valueOf(value);
      case STRING -> value;
    };
  }
}
