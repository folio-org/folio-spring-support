package org.folio.spring.kafka.filtering.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementClient;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.folio.spring.testing.type.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies that {@link EntitlementReconciliationTask} is actually invoked by Spring's scheduler on
 * the configured interval, rather than only exercising {@code refresh()} directly.
 */
@IntegrationTest
class EntitlementReconciliationTaskIT {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(KafkaConsumerFilteringConfiguration.class))
    .withBean(FolioModuleMetadata.class, () -> folioModuleMetadata("mod-foo", "1.2.3"))
    .withBean(JsonMapper.class, JsonMapper::new)
    .withPropertyValues(
      "folio.kafka.tenant-filter.enabled=true",
      "okapi.url=http://localhost:9130",
      "folio.kafka.tenant-filter.entitlement-refresh-interval-seconds=1");

  @Test
  void reconcile_positive_refreshesCacheOnSchedule() {
    var callCount = new AtomicInteger();
    TenantEntitlementClient client = moduleId -> Set.of("tenant-" + callCount.incrementAndGet());

    contextRunner.withBean(TenantEntitlementClient.class, () -> client).run(context -> {
      var tenantEntitlementService = context.getBean(TenantEntitlementService.class);

      await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(callCount.get()).isGreaterThanOrEqualTo(2));

      assertThat(tenantEntitlementService.getEnabledTenants()).containsExactly("tenant-" + callCount.get());
    });
  }

  private static FolioModuleMetadata folioModuleMetadata(String moduleName, String moduleVersion) {
    return new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return moduleName;
      }

      @Override
      public Optional<String> getModuleVersion() {
        return Optional.ofNullable(moduleVersion);
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return tenantId + "_schema";
      }
    };
  }
}
