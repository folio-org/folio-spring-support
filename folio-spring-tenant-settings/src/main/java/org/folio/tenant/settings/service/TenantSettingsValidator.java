package org.folio.tenant.settings.service;

import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.settings.entity.SettingEntity;
import org.folio.tenant.settings.exception.TenantSettingsValidationException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * Validator for tenant settings operations.
 */
@Component
public class TenantSettingsValidator {

  /**
   * Validate the setting update request against the setting entity.
   *
   * @param updateRequest the update request
   * @param entity the setting entity
   */
  public void validate(@NonNull SettingUpdateRequest updateRequest, @NonNull SettingEntity entity) {
    if (updateRequest.getValue() != null) {
      validateSettingValue(updateRequest.getValue(), entity.getType());
    }
  }

  private void validateSettingValue(Object value, SettingEntity.SettingType type) {
    switch (type) {
      case STRING -> validateType(value, String.class, "Setting value should be a string");
      case INTEGER -> validateType(value, Integer.class, "Setting value should be an integer");
      case BOOLEAN -> validateType(value, Boolean.class, "Setting value should be a boolean");
      default -> throw new IllegalStateException("Unexpected value: " + type);
    }
  }

  private <T> void validateType(Object value, Class<T> type, String errorMessage) {
    if (!type.isInstance(value)) {
      throw new TenantSettingsValidationException(errorMessage);
    }
  }
}
