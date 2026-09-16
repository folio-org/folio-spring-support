package org.folio.spring.kafka.filtering.entitlement;

/**
 * Entitlement change event consumed from the {@code entitlement} Kafka topic, published by
 * mgr-tenant-entitlements whenever a tenant is entitled to or revoked from a module.
 *
 * @param type entitlement change type
 * @param moduleId module id the event applies to
 * @param tenantName tenant name the event applies to
 */
public record EntitlementEvent(Type type, String moduleId, String tenantName) {

  /**
   * Entitlement change type.
   */
  public enum Type {
    ENTITLE,
    UPGRADE,
    REVOKE
  }
}
