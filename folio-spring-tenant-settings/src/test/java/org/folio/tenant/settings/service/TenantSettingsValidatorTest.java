package org.folio.tenant.settings.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.spring.testing.type.UnitTest;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.entity.SettingEntity;
import org.folio.tenant.settings.exception.TenantSettingsValidationException;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantSettingsValidatorTest {

  private final TenantSettingsValidator validator = new TenantSettingsValidator();

  @Test
  void validate_shouldAcceptValidBooleanValue() {
    var entity = createEntity(SettingEntity.SettingType.BOOLEAN);
    var request = new SettingUpdateRequest().value(true);

    assertThatCode(() -> validator.validate(request, entity))
      .doesNotThrowAnyException();
  }

  @Test
  void validate_shouldAcceptValidIntegerValue() {
    var entity = createEntity(SettingEntity.SettingType.INTEGER);
    var request = new SettingUpdateRequest().value(42);

    assertThatCode(() -> validator.validate(request, entity))
      .doesNotThrowAnyException();
  }

  @Test
  void validate_shouldAcceptValidStringValue() {
    var entity = createEntity(SettingEntity.SettingType.STRING);
    var request = new SettingUpdateRequest().value("test-value");

    assertThatCode(() -> validator.validate(request, entity))
      .doesNotThrowAnyException();
  }

  @Test
  void validate_shouldAcceptNullValue() {
    var entity = createEntity(SettingEntity.SettingType.BOOLEAN);
    var request = new SettingUpdateRequest().value(null);

    assertThatCode(() -> validator.validate(request, entity))
      .doesNotThrowAnyException();
  }

  @Test
  void validate_shouldRejectStringForBooleanType() {
    var entity = createEntity(SettingEntity.SettingType.BOOLEAN);
    var request = new SettingUpdateRequest().value("not-a-boolean");

    assertThatThrownBy(() -> validator.validate(request, entity))
      .isInstanceOf(TenantSettingsValidationException.class)
      .hasMessage("Setting value should be a boolean");
  }

  @Test
  void validate_shouldRejectIntegerForBooleanType() {
    var entity = createEntity(SettingEntity.SettingType.BOOLEAN);
    var request = new SettingUpdateRequest().value(1);

    assertThatThrownBy(() -> validator.validate(request, entity))
      .isInstanceOf(TenantSettingsValidationException.class)
      .hasMessage("Setting value should be a boolean");
  }

  @Test
  void validate_shouldRejectStringForIntegerType() {
    var entity = createEntity(SettingEntity.SettingType.INTEGER);
    var request = new SettingUpdateRequest().value("not-an-integer");

    assertThatThrownBy(() -> validator.validate(request, entity))
      .isInstanceOf(TenantSettingsValidationException.class)
      .hasMessage("Setting value should be an integer");
  }

  @Test
  void validate_shouldRejectBooleanForIntegerType() {
    var entity = createEntity(SettingEntity.SettingType.INTEGER);
    var request = new SettingUpdateRequest().value(false);

    assertThatThrownBy(() -> validator.validate(request, entity))
      .isInstanceOf(TenantSettingsValidationException.class)
      .hasMessage("Setting value should be an integer");
  }

  @Test
  void validate_shouldRejectIntegerForStringType() {
    var entity = createEntity(SettingEntity.SettingType.STRING);
    var request = new SettingUpdateRequest().value(123);

    assertThatThrownBy(() -> validator.validate(request, entity))
      .isInstanceOf(TenantSettingsValidationException.class)
      .hasMessage("Setting value should be a string");
  }

  @Test
  void validate_shouldRejectBooleanForStringType() {
    var entity = createEntity(SettingEntity.SettingType.STRING);
    var request = new SettingUpdateRequest().value(true);

    assertThatThrownBy(() -> validator.validate(request, entity))
      .isInstanceOf(TenantSettingsValidationException.class)
      .hasMessage("Setting value should be a string");
  }

  private SettingEntity createEntity(SettingEntity.SettingType type) {
    var entity = new SettingEntity();
    entity.setType(type);
    entity.setKey("test-key");
    entity.setValue("test-value");
    entity.setGroupId("test-group");
    return entity;
  }
}
