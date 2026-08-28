package org.folio.spring.kafka.filtering.entitlement;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Resolves tenants entitled to the current module.
 */
@Log4j2
public class TenantEntitlementService {

  @Getter
  private final String moduleId;
  private final TenantEntitlementClient tenantEntitlementClient;

  /**
   * Creates a tenant entitlement service for the current module.
   *
   * @param moduleId current module id
   * @param tenantEntitlementClient HTTP client for tenant entitlement lookups
   */
  public TenantEntitlementService(String moduleId, TenantEntitlementClient tenantEntitlementClient) {
    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }

    this.moduleId = moduleId;
    this.tenantEntitlementClient = Objects.requireNonNull(tenantEntitlementClient,
      "tenantEntitlementClient must not be null");
  }

  /**
   * Returns tenants entitled to the current module.
   *
   * @return entitled tenant ids, or an empty set when the entitlement service returns {@code null}
   */
  public Set<String> getEnabledTenants() {
    var result = tenantEntitlementClient.lookupTenantsByModuleId(moduleId);
    log.debug("Tenants entitled for module: {}", result);
    return result == null ? Set.of() : result;
  }
}
