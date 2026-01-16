package org.folio.tenant.settings.exception;

/**
 * Exception thrown when a user attempts to perform an operation without the required permission.
 */
public class TenantSettingsUnauthorizedOperationException extends RuntimeException {

  private static final String MESSAGE = "Permission '%s' required to perform this operation";

  /**
   * Constructs a new exception with the required permission.
   *
   * @param requiredPermission the permission required to perform the operation
   */
  public TenantSettingsUnauthorizedOperationException(String requiredPermission) {
    super(MESSAGE.formatted(requiredPermission));
  }
}
