package org.folio.spring.kafka.filtering.configuration;

import static org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy.FAIL;
import static org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy.SKIP;

import lombok.Data;
import org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Property model for tenant-aware Kafka filtering.
 */
@Data
@ConfigurationProperties(KafkaTenantFilterProperties.CONFIG_PREFIX)
public class KafkaTenantFilterProperties {

  /** Property prefix for Kafka tenant filtering configuration. */
  public static final String CONFIG_PREFIX = "folio.kafka.tenant-filter";

  /** Whether tenant filtering is enabled. */
  private boolean enabled = false;

  /** Whether listener invocation is skipped when all records in a batch are filtered out. */
  private boolean ignoreEmptyBatch = true;

  /** Strategy applied when the message tenant is not entitled to the current module. */
  private DisabledTenantStrategy tenantDisabledStrategy = SKIP;

  /** Strategy applied when no tenants are entitled to the current module. */
  private DisabledTenantStrategy allTenantsDisabledStrategy = FAIL;
}
