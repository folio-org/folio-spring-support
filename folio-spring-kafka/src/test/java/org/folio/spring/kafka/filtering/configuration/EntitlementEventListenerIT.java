package org.folio.spring.kafka.filtering.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementClient;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.folio.spring.testing.extension.EnableKafka;
import org.folio.spring.testing.type.IntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

/**
 * Verifies that a real message on the {@code entitlement} Kafka topic updates the cached
 * entitled-tenants set, exercising the actual listener container rather than calling
 * {@link TenantEntitlementService#applyEntitlementEvent} directly.
 */
@IntegrationTest
@EnableKafka
class EntitlementEventListenerIT {

  private static final String MODULE_ID = "mod-foo-1.2.3";
  private static final String ENTITLEMENT_TOPIC = "folio.entitlement";

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class, KafkaConsumerFilteringConfiguration.class))
    .withBean(FolioModuleMetadata.class, () -> folioModuleMetadata("mod-foo", "1.2.3"))
    .withBean(JsonMapper.class, JsonMapper::new)
    .withBean(TenantEntitlementClient.class, () -> moduleId -> Set.of("tenant-1"))
    .withPropertyValues(
      "folio.kafka.tenant-filter.enabled=true",
      "okapi.url=http://localhost:9130");

  @BeforeAll
  static void createEntitlementTopic() {
    var configs = Map.<String, Object>of(
      AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.kafka.bootstrap-servers"));

    try (var adminClient = AdminClient.create(configs)) {
      adminClient.createTopics(List.of(new NewTopic(ENTITLEMENT_TOPIC, 1, (short) 1))).all().get();
    } catch (Exception e) {
      if (!(e.getCause() instanceof TopicExistsException)) {
        throw new IllegalStateException("Failed to create entitlement topic", e);
      }
    }
  }

  @Test
  void applyEntitlementEvent_positive_updatesCacheWhenEventIsConsumed() {
    contextRunner.run(context -> {
      var tenantEntitlementService = context.getBean(TenantEntitlementService.class);
      assertThat(tenantEntitlementService.getEnabledTenants()).containsExactly("tenant-1");

      try (var producer = createProducer()) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
          publishEntitlementEvent(producer, "ENTITLE", MODULE_ID, "tenant-2");
          assertThat(tenantEntitlementService.getEnabledTenants())
            .containsExactlyInAnyOrder("tenant-1", "tenant-2");
        });
      }
    });
  }

  @Test
  void applyEntitlementEvent_positive_addsTenantWhenUpgradeEventIsConsumed() {
    contextRunner.run(context -> {
      var tenantEntitlementService = context.getBean(TenantEntitlementService.class);
      assertThat(tenantEntitlementService.getEnabledTenants()).containsExactly("tenant-1");

      try (var producer = createProducer()) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
          publishEntitlementEvent(producer, "UPGRADE", MODULE_ID, "tenant-2");
          assertThat(tenantEntitlementService.getEnabledTenants())
            .containsExactlyInAnyOrder("tenant-1", "tenant-2");
        });
      }
    });
  }

  @Test
  void applyEntitlementEvent_positive_removesTenantWhenRevokeEventIsConsumed() {
    contextRunner.run(context -> {
      var tenantEntitlementService = context.getBean(TenantEntitlementService.class);
      assertThat(tenantEntitlementService.getEnabledTenants()).containsExactly("tenant-1");

      try (var producer = createProducer()) {
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
          publishEntitlementEvent(producer, "REVOKE", MODULE_ID, "tenant-1");
          assertThat(tenantEntitlementService.getEnabledTenants()).isEmpty();
        });
      }
    });
  }

  private static Producer<String, String> createProducer() {
    var configs = Map.<String, Object>of(
      ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.kafka.bootstrap-servers"));
    return new KafkaProducer<>(configs, new StringSerializer(), new StringSerializer());
  }

  private static void publishEntitlementEvent(Producer<String, String> producer, String type, String moduleId,
    String tenantName) {

    var value = """
      {
        "type": "%s",
        "moduleId": "%s",
        "tenantName": "%s"
      }
      """.formatted(type, moduleId, tenantName);

    try {
      producer.send(new ProducerRecord<>(ENTITLEMENT_TOPIC, tenantName + "_" + moduleId, value)).get();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to publish entitlement event", e);
    }
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
