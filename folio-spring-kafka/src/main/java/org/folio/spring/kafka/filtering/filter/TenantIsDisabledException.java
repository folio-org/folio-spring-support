package org.folio.spring.kafka.filtering.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Thrown when a tenant is not entitled to receive messages for the current module.
 */
public final class TenantIsDisabledException extends RuntimeException {

  private static final String DEFAULT_MESSAGE_FORMAT =
    "Tenant is not enabled for the module: tenant = %s, module = %s";

  /** Message tenant id. */
  private final String tenant;

  /** Current module id. */
  private final String moduleId;

  private TenantIsDisabledException(String tenant, String moduleId) {
    super(String.format(DEFAULT_MESSAGE_FORMAT, tenant, moduleId));
    this.tenant = tenant;
    this.moduleId = moduleId;
  }

  /**
   * Creates an exception for a tenant that is not entitled to the module.
   *
   * @param tenant message tenant id
   * @param moduleId current module id
   * @return tenant-disabled exception
   */
  public static TenantIsDisabledException of(String tenant, String moduleId) {
    if (isBlank(tenant)) {
      throw new IllegalArgumentException("Tenant must not be blank");
    }

    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }

    return new TenantIsDisabledException(tenant, moduleId);
  }

  /**
   * Returns the message tenant id.
   *
   * @return tenant id
   */
  public String getTenant() {
    return tenant;
  }

  /**
   * Returns the current module id.
   *
   * @return module id
   */
  public String getModuleId() {
    return moduleId;
  }
}
