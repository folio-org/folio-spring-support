package org.folio.spring.kafka.filtering.configuration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementClient;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.folio.spring.kafka.filtering.filter.EnabledTenantMessageFilterStrategy;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class KafkaConsumerFilteringConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(KafkaConsumerFilteringConfiguration.class));

  @Test
  @SuppressWarnings("unchecked")
  void disabledTenantMessageFilter_positive_registeredByDefault() {
    contextRunner.run(context -> {
      assertThat(context).hasBean("tenantAwareMessageFilter");

      RecordFilterStrategy<String, Object> filter = context.getBean("tenantAwareMessageFilter",
        RecordFilterStrategy.class);

      assertThat(filter.filter(consumerRecord())).isFalse();
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  void disabledTenantMessageFilter_positive_registeredWhenPropertyIsFalse() {
    contextRunner
      .withPropertyValues("folio.kafka.tenant-filter.enabled=false")
      .run(context -> {
        assertThat(context).hasBean("tenantAwareMessageFilter");

        RecordFilterStrategy<String, Object> filter = context.getBean("tenantAwareMessageFilter",
          RecordFilterStrategy.class);

        assertThat(filter.filter(consumerRecord())).isFalse();
      });
  }

  @Test
  void enabledTenantMessageFilter_positive_registeredWhenPropertyIsTrue() {
    contextRunner
      .withBean(FolioModuleMetadata.class, () -> folioModuleMetadata("mod-foo", "1.2.3"))
      .withBean(JsonMapper.class, JsonMapper::new)
      .withPropertyValues(
        "folio.kafka.tenant-filter.enabled=true",
        "okapi.url=http://localhost:9130")
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context).hasBean("tenantAwareMessageFilter");
        assertThat(context).hasSingleBean(TenantEntitlementService.class);
        assertThat(context.getBean("tenantAwareMessageFilter")).isInstanceOf(EnabledTenantMessageFilterStrategy.class);
      });
  }

  @Test
  void entitlementEventListenerContainer_positive_notRegisteredWithoutKafkaProperties() {
    contextRunner
      .withBean(FolioModuleMetadata.class, () -> folioModuleMetadata("mod-foo", "1.2.3"))
      .withBean(JsonMapper.class, JsonMapper::new)
      .withPropertyValues(
        "folio.kafka.tenant-filter.enabled=true",
        "okapi.url=http://localhost:9130")
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context).doesNotHaveBean("entitlementEventListenerContainer");
        assertThat(context).hasSingleBean(EntitlementReconciliationTask.class);
      });
  }

  @Test
  void entitlementEventListenerContainer_positive_registeredWithKafkaProperties() {
    contextRunner
      .withBean(FolioModuleMetadata.class, () -> folioModuleMetadata("mod-foo", "1.2.3"))
      .withBean(JsonMapper.class, JsonMapper::new)
      .withBean(KafkaProperties.class, KafkaProperties::new)
      .withPropertyValues(
        "folio.kafka.tenant-filter.enabled=true",
        "okapi.url=http://localhost:9130",
        "spring.kafka.bootstrap-servers=localhost:19092")
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context).hasBean("entitlementEventListenerContainer");
      });
  }

  @Test
  void tenantEntitlementService_positive_usesFolioModuleMetadataModuleId() {
    var configuration = new KafkaConsumerFilteringConfiguration.EnabledTenantFilterConfiguration();

    var service = configuration.tenantEntitlementService(
      folioModuleMetadata("mod-foo", "1.2.3"), moduleId -> Set.of());

    assertThat(service.getModuleId()).isEqualTo("mod-foo-1.2.3");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void tenantEntitlementService_negative_failsWhenVersionIsMissing(String moduleVersion) {
    var configuration = new KafkaConsumerFilteringConfiguration.EnabledTenantFilterConfiguration();
    var metadata = folioModuleMetadata("mod-foo", moduleVersion);
    TenantEntitlementClient tenantEntitlementClient = moduleId -> Set.of();

    assertThatThrownBy(() -> configuration.tenantEntitlementService(metadata, tenantEntitlementClient))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("spring.application.version");
  }

  private static ConsumerRecord<String, Object> consumerRecord() {
    var kafkaRecord = new ConsumerRecord<>("test-topic", 0, 0L, "key-1", new Object());
    kafkaRecord.headers().add(XOkapiHeaders.TENANT, "tenant-1".getBytes(UTF_8));
    return kafkaRecord;
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
