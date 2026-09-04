package org.folio.spring.kafka.filtering.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Getter;

/**
 * Thrown when no tenants are entitled to receive messages for the current module.
 */
@Getter
public final class TenantsAreDisabledException extends RuntimeException {

  private static final String DEFAULT_MESSAGE_FORMAT = "No tenants are enabled for the module: moduleId = %s";

  /** Current module id. */
  private final String moduleId;

  private TenantsAreDisabledException(String moduleId) {
    super(String.format(DEFAULT_MESSAGE_FORMAT, moduleId));
    this.moduleId = moduleId;
  }

  /**
   * Creates an exception for a module with no entitled tenants.
   *
   * @param moduleId current module id
   * @return all-tenants-disabled exception
   */
  public static TenantsAreDisabledException of(String moduleId) {
    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }

    return new TenantsAreDisabledException(moduleId);
  }
}
