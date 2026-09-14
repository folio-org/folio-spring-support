package org.folio.spring.kafka.filtering.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * {@link RecordFilterStrategy} that accepts only records whose tenant is currently entitled
 * to receive messages for the current module.
 *
 * @param <K> the Kafka record key type
 * @param <V> the Kafka record value type
 */
@Log4j2
public class EnabledTenantMessageFilterStrategy<K, V> implements RecordFilterStrategy<K, V> {

  /**
   * Tenant header set unconditionally by several FOLIO producer utilities - for example
   * folio-service-tools' {@code FolioMessageProducer} and folio-kafka-wrapper's
   * {@code KafkaProducerRecordBuilder} - as a fallback alongside or instead of {@code X-Okapi-Tenant}.
   */
  private static final String FOLIO_TENANT_ID_HEADER = "folio.tenantId";

  private final String moduleId;
  private final TenantEntitlementService tenantEntitlementService;
  private final boolean ignoreEmptyBatch;
  private final DisabledTenantStrategy tenantDisabledStrategy;
  private final DisabledTenantStrategy allTenantsDisabledStrategy;

  /**
   * Creates a tenant-aware Kafka record filter.
   *
   * @param moduleId current module id
   * @param tenantEntitlementService service for tenant entitlement lookups
   * @param ignoreEmptyBatch whether listener invocation is skipped for empty filtered batches
   * @param tenantDisabledStrategy strategy when a message tenant is not entitled
   * @param allTenantsDisabledStrategy strategy when no tenants are entitled to the module
   */
  public EnabledTenantMessageFilterStrategy(String moduleId, TenantEntitlementService tenantEntitlementService,
    boolean ignoreEmptyBatch, DisabledTenantStrategy tenantDisabledStrategy,
    DisabledTenantStrategy allTenantsDisabledStrategy) {
    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }

    this.moduleId = moduleId;
    this.tenantEntitlementService = Objects.requireNonNull(tenantEntitlementService,
      "tenantEntitlementService must not be null");
    this.ignoreEmptyBatch = ignoreEmptyBatch;
    this.tenantDisabledStrategy = Objects.requireNonNull(tenantDisabledStrategy,
      "tenantDisabledStrategy must not be null");
    this.allTenantsDisabledStrategy = Objects.requireNonNull(allTenantsDisabledStrategy,
      "allTenantsDisabledStrategy must not be null");
  }

  @Override
  public boolean filter(ConsumerRecord<K, V> consumerRecord) {
    var tenant = resolveTenant(consumerRecord);

    if (tenant.isEmpty()) {
      return false;
    }

    log.debug("Filtering message for tenant: messageKey = {}, tenant = {}", consumerRecord.key(), tenant.get());
    var enabledTenants = tenantEntitlementService.getEnabledTenants();
    var result = filterByEnabledTenants(enabledTenants, tenant.get());

    log.debug("Message for tenant is {}: messageKey = {}, tenant = {}",
      result ? "filtered out" : "accepted", consumerRecord.key(), tenant.get());
    return result;
  }

  private Optional<String> resolveTenant(ConsumerRecord<K, V> consumerRecord) {
    var tenant = findHeaderValue(consumerRecord, XOkapiHeaders.TENANT);
    if (tenant == null) {
      tenant = findHeaderValue(consumerRecord, FOLIO_TENANT_ID_HEADER);
    }

    if (tenant == null) {
      log.warn("Received message with missing or blank {}/{} header: messageKey = {}. Filter won't be applied.",
        XOkapiHeaders.TENANT, FOLIO_TENANT_ID_HEADER, consumerRecord.key());
      return Optional.empty();
    }
    return Optional.of(tenant);
  }

  private static String findHeaderValue(ConsumerRecord<?, ?> consumerRecord, String headerName) {
    for (Header header : consumerRecord.headers()) {
      if (headerName.equalsIgnoreCase(header.key())) {
        var value = trimToNull(headerValue(header));
        if (value != null) {
          return value;
        }
      }
    }
    return null;
  }

  private boolean filterByEnabledTenants(Set<String> enabledTenants, String currentTenant) {
    if (enabledTenants == null || enabledTenants.isEmpty()) {
      log.warn("No tenants are enabled for the module. Applying 'no enabled tenants' strategy: {}",
        allTenantsDisabledStrategy);
      return applyStrategy(allTenantsDisabledStrategy, () -> TenantsAreDisabledException.of(moduleId));
    }

    var notEnabled = !enabledTenants.contains(currentTenant);
    return notEnabled && applyStrategy(tenantDisabledStrategy,
      () -> TenantIsDisabledException.of(currentTenant, moduleId));
  }

  @Override
  public boolean ignoreEmptyBatch() {
    return ignoreEmptyBatch;
  }

  private static String headerValue(Header header) {
    return header.value() == null ? null : new String(header.value(), UTF_8);
  }

  private static boolean applyStrategy(DisabledTenantStrategy strategy, Supplier<RuntimeException> exceptionSupplier) {
    return switch (strategy) {
      case ACCEPT -> false;
      case SKIP -> true;
      case FAIL -> throw exceptionSupplier.get();
    };
  }
}
