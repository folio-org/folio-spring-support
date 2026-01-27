package org.folio.tenant.settings.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.entity.SettingEntity;
import org.folio.tenant.settings.entity.SettingGroupEntity;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantSettingsMapperTest {

  private final TenantSettingsMapper mapper = new TenantSettingsMapper();

  @Test
  void toDto_shouldMapSettingGroupEntityCorrectly() {
    var entity = new SettingGroupEntity();
    entity.setId("test-group");
    entity.setName("Test Group");
    entity.setDescription("Test group description");

    var dto = mapper.toDto(entity);

    assertThat(dto.getId()).isEqualTo("test-group");
    assertThat(dto.getName()).isEqualTo("Test Group");
    assertThat(dto.getDescription()).isEqualTo("Test group description");
  }

  @Test
  void toDto_shouldMapSettingEntityWithBooleanValue() {
    var createdDate = OffsetDateTime.now().minusDays(1);
    var updatedDate = OffsetDateTime.now();
    var createdBy = UUID.randomUUID();
    var updatedBy = UUID.randomUUID();

    var entity = new SettingEntity();
    entity.setId(UUID.randomUUID());
    entity.setKey("enabled");
    entity.setValue("true");
    entity.setType(SettingEntity.SettingType.BOOLEAN);
    entity.setDescription("Enable feature");
    entity.setGroupId("test-group");
    entity.setCreatedDate(createdDate);
    entity.setCreatedByUserId(createdBy);
    entity.setUpdatedDate(updatedDate);
    entity.setUpdatedByUserId(updatedBy);

    var dto = mapper.toDto(entity);

    assertThat(dto.getKey()).isEqualTo("enabled");
    assertThat(dto.getValue()).isEqualTo(true);
    assertThat(dto.getType()).isEqualTo(Setting.TypeEnum.BOOLEAN);
    assertThat(dto.getDescription()).isEqualTo("Enable feature");
    assertThat(dto.getGroupId()).isEqualTo("test-group");
    assertThat(dto.getMetadata()).isNotNull();
    assertThat(dto.getMetadata().getCreatedDate()).isEqualTo(createdDate);
    assertThat(dto.getMetadata().getCreatedByUserId()).isEqualTo(createdBy);
    assertThat(dto.getMetadata().getUpdatedDate()).isEqualTo(updatedDate);
    assertThat(dto.getMetadata().getUpdatedByUserId()).isEqualTo(updatedBy);
  }

  @Test
  void toDto_shouldMapSettingEntityWithIntegerValue() {
    var entity = new SettingEntity();
    entity.setKey("max-count");
    entity.setValue("100");
    entity.setType(SettingEntity.SettingType.INTEGER);
    entity.setDescription("Maximum count");
    entity.setGroupId("test-group");
    entity.setCreatedDate(OffsetDateTime.now());
    entity.setUpdatedDate(OffsetDateTime.now());

    var dto = mapper.toDto(entity);

    assertThat(dto.getValue()).isEqualTo(100);
    assertThat(dto.getType()).isEqualTo(Setting.TypeEnum.INTEGER);
  }

  @Test
  void toDto_shouldMapSettingEntityWithStringValue() {
    var entity = new SettingEntity();
    entity.setKey("api-key");
    entity.setValue("secret-key-123");
    entity.setType(SettingEntity.SettingType.STRING);
    entity.setDescription("API Key");
    entity.setGroupId("test-group");
    entity.setCreatedDate(OffsetDateTime.now());
    entity.setUpdatedDate(OffsetDateTime.now());

    var dto = mapper.toDto(entity);

    assertThat(dto.getValue()).isEqualTo("secret-key-123");
    assertThat(dto.getType()).isEqualTo(Setting.TypeEnum.STRING);
  }

  @Test
  void toDto_shouldMapSettingEntityWithNullValue() {
    var entity = new SettingEntity();
    entity.setKey("api-key");
    entity.setValue(null);
    entity.setType(SettingEntity.SettingType.STRING);
    entity.setDescription("API Key");
    entity.setGroupId("test-group");
    entity.setCreatedDate(OffsetDateTime.now());
    entity.setUpdatedDate(OffsetDateTime.now());

    var dto = mapper.toDto(entity);

    assertThat(dto.getValue()).isNull();
    assertThat(dto.getType()).isEqualTo(Setting.TypeEnum.STRING);
  }

  @Test
  void updateEntity_shouldUpdateValueAndDescription() {
    var entity = new SettingEntity();
    entity.setValue("old-value");
    entity.setDescription("Old description");

    var updateRequest = new SettingUpdateRequest()
      .value("new-value")
      .description("New description");

    mapper.updateEntity(entity, updateRequest);

    assertThat(entity.getValue()).isEqualTo("new-value");
    assertThat(entity.getDescription()).isEqualTo("New description");
  }

  @Test
  void updateEntity_shouldUpdateOnlyValue() {
    var entity = new SettingEntity();
    entity.setValue("old-value");
    entity.setDescription("Original description");

    var updateRequest = new SettingUpdateRequest().value("new-value");

    mapper.updateEntity(entity, updateRequest);

    assertThat(entity.getValue()).isEqualTo("new-value");
    assertThat(entity.getDescription()).isEqualTo("Original description");
  }

  @Test
  void updateEntity_shouldUpdateOnlyDescription() {
    var entity = new SettingEntity();
    entity.setValue("original-value");
    entity.setDescription("Old description");

    var updateRequest = new SettingUpdateRequest().description("New description");

    mapper.updateEntity(entity, updateRequest);

    assertThat(entity.getValue()).isEqualTo("original-value");
    assertThat(entity.getDescription()).isEqualTo("New description");
  }

  @Test
  void updateEntity_shouldHandleNullValuesInRequest() {
    var entity = new SettingEntity();
    entity.setValue("original-value");
    entity.setDescription("Original description");

    var updateRequest = new SettingUpdateRequest();

    mapper.updateEntity(entity, updateRequest);

    assertThat(entity.getValue()).isEqualTo("original-value");
    assertThat(entity.getDescription()).isEqualTo("Original description");
  }

  @Test
  void updateEntity_shouldHandleNullValuesInEntity() {
    var entity = new SettingEntity();
    var updateRequest = new SettingUpdateRequest();
    updateRequest.value("new-value");
    updateRequest.description("New description");

    mapper.updateEntity(entity, updateRequest);

    assertThat(entity.getValue()).isEqualTo("new-value");
    assertThat(entity.getDescription()).isEqualTo("New description");
  }

  @Test
  void updateEntity_shouldConvertBooleanToString() {
    var entity = new SettingEntity();
    entity.setValue("false");

    var updateRequest = new SettingUpdateRequest().value(true);

    mapper.updateEntity(entity, updateRequest);

    assertThat(entity.getValue()).isEqualTo("true");
  }

  @Test
  void updateEntity_shouldConvertIntegerToString() {
    var entity = new SettingEntity();
    entity.setValue("10");

    var updateRequest = new SettingUpdateRequest().value(42);

    mapper.updateEntity(entity, updateRequest);

    assertThat(entity.getValue()).isEqualTo("42");
  }
}
