package org.folio.spring.kafka.filtering.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically re-fetches the entitled-tenants set, correcting any drift from a missed or duplicate
 * entitlement change event.
 */
@Log4j2
@RequiredArgsConstructor
public class EntitlementReconciliationTask {

  private final TenantEntitlementService tenantEntitlementService;

  /**
   * Re-fetches the entitled-tenants set, replacing whatever is currently cached.
   */
  @Scheduled(fixedDelayString = "${" + KafkaTenantFilterProperties.CONFIG_PREFIX + ".reconciliation-interval:15m}",
    initialDelayString = "${" + KafkaTenantFilterProperties.CONFIG_PREFIX + ".reconciliation-interval:15m}")
  public void reconcile() {
    log.debug("Reconciling tenant entitlement cache: moduleId = {}", tenantEntitlementService.getModuleId());
    tenantEntitlementService.refresh();
  }
}
