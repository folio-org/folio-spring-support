package org.folio.spring.kafka.filtering.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.kafka.filtering.entitlement.EntitlementEvent;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Applies entitlement change events directly to the cached entitled-tenants set, so per-message
 * filtering picks up the change on the next message without waiting on
 * {@link TenantEntitlementService#refresh()}.
 */
@RequiredArgsConstructor
public class EntitlementEventListener {

  public static final String ENTITLEMENT_TOPIC_NAME = entitlementTopicName();

  private final TenantEntitlementService tenantEntitlementService;

  /**
   * Applies an entitlement change event received on the {@code entitlement} Kafka topic.
   *
   * @param event entitlement change event, or {@code null} for a tombstone record
   */
  @KafkaListener(
    topics = "#{T(org.folio.spring.kafka.filtering.configuration.EntitlementEventListener).ENTITLEMENT_TOPIC_NAME}",
    containerFactory = "entitlementEventListenerContainerFactory"
  )
  public void onEntitlementEvent(EntitlementEvent event) {
    if (event != null) {
      tenantEntitlementService.applyEntitlementEvent(event);
    }
  }

  private static String entitlementTopicName() {
    var env = StringUtils.firstNonBlank(System.getenv("ENV"), System.getProperty("env"), "folio");
    return env + ".entitlement";
  }
}
