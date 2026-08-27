package org.folio.spring.kafka.filtering.configuration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
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
  void disabledTenantMessageFilter_positive_registeredWhenPropertyIsFalse() {
    contextRunner
      .withPropertyValues("folio.kafka.tenant-filter.enabled=false")
      .run(context -> assertThat(context).hasBean("tenantAwareMessageFilter"));
  }

  @Test
  void enabledTenantMessageFilter_positive_registeredWhenPropertyIsTrue() {
    contextRunner
      .withBean(JsonMapper.class, JsonMapper::new)
      .withPropertyValues(
        "folio.kafka.tenant-filter.enabled=true",
        "okapi.url=http://localhost:9130",
        "spring.application.name=mod-foo",
        "spring.application.version=1.2.3")
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context).hasBean("tenantAwareMessageFilter");
        assertThat(context.getBean("kafkaTenantFilterModuleId")).isEqualTo("mod-foo-1.2.3");
      });
  }

  @Test
  void kafkaTenantFilterModuleId_positive_usesSpringApplicationNameAndVersion() {
    var configuration = new KafkaConsumerFilteringConfiguration.EnabledTenantFilterConfiguration();

    var moduleId = configuration.kafkaTenantFilterModuleId("mod-foo", "1.2.3");

    assertThat(moduleId).isEqualTo("mod-foo-1.2.3");
  }

  @Test
  void kafkaTenantFilterModuleId_negative_failsWhenVersionIsMissing() {
    var configuration = new KafkaConsumerFilteringConfiguration.EnabledTenantFilterConfiguration();

    assertThatThrownBy(() -> configuration.kafkaTenantFilterModuleId("mod-foo", ""))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("spring.application.name")
      .hasMessageContaining("spring.application.version");
  }

  private static ConsumerRecord<String, Object> consumerRecord() {
    var record = new ConsumerRecord<>("test-topic", 0, 0L, "key-1", new Object());
    record.headers().add(XOkapiHeaders.TENANT, "tenant-1".getBytes(UTF_8));
    return record;
  }
}
