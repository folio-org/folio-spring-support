package org.folio.spring.kafka.filtering.entitlement;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Resolves tenants entitled to the current module.
 *
 * <p>The entitled-tenants result is cached in-process so per-message filtering never has to make a
 * network call. The cache is kept up to date two ways: entitlement change events for this module are
 * applied directly to the cached set as they arrive (see {@link #applyEntitlementEvent(EntitlementEvent)}),
 * and {@link #refresh()} periodically re-fetches the full set from the entitlement client so any drift
 * from a missed or duplicate event self-heals.
 */
@Log4j2
public class TenantEntitlementService {

  @Getter
  private final String moduleId;
  private final TenantEntitlementClient tenantEntitlementClient;
  private final AtomicReference<Set<String>> enabledTenants = new AtomicReference<>();

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
   * Returns tenants entitled to the current module, fetching and caching the result on first call.
   *
   * @return entitled tenant ids
   */
  public Set<String> getEnabledTenants() {
    var cached = enabledTenants.get();
    return cached != null ? cached : refresh();
  }

  /**
   * Re-fetches the full entitled-tenants set from the entitlement client and replaces the cached
   * result, correcting any drift accumulated from missed or duplicate entitlement events.
   *
   * @return the freshly fetched entitled tenant ids
   */
  public Set<String> refresh() {
    var beforeFetch = enabledTenants.get();
    var result = tenantEntitlementClient.lookupTenantsByModuleId(moduleId);
    var refreshed = result == null ? Set.<String>of() : Set.copyOf(result);

    if (!enabledTenants.compareAndSet(beforeFetch, refreshed)) {
      log.debug("Skipped applying stale refresh: moduleId = {}, an entitlement event was applied "
        + "while the refresh was in flight", moduleId);
      return enabledTenants.get();
    }

    log.debug("Refreshed tenant entitlement cache: moduleId = {}, enabledTenants = {}", moduleId, refreshed);
    return refreshed;
  }

  /**
   * Applies an entitlement change event directly to the cached set, skipping a round trip to the
   * entitlement client. Events for other modules, or received before the cache is populated, are ignored.
   *
   * @param event entitlement change event received from the {@code entitlement} Kafka topic
   */
  public void applyEntitlementEvent(EntitlementEvent event) {
    if (!moduleId.equals(event.moduleId())) {
      return;
    }

    var updated = enabledTenants.updateAndGet(current -> {
      if (current == null) {
        return null;
      }

      var next = new HashSet<>(current);
      if (event.type() == EntitlementEvent.Type.REVOKE) {
        next.remove(event.tenantName());
      } else {
        next.add(event.tenantName());
      }
      return Set.copyOf(next);
    });

    log.info("Applied entitlement change event: moduleId = {}, tenant = {}, type = {}, enabledTenants = {}",
      moduleId, event.tenantName(), event.type(), updated);
  }
}
