package org.folio.tenant.settings.exception;

/**
 * Exception thrown when validation of tenant settings fails.
 */
public class TenantSettingsValidationException extends RuntimeException {

  /**
   * Constructs a new validation exception with the specified message.
   *
   * @param msg the validation error message
   */
  public TenantSettingsValidationException(String msg) {
    super(msg);
  }
}
