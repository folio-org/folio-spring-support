package org.folio.spring.kafka.filtering.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy.FAIL;
import static org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy.SKIP;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KafkaTenantFilterPropertiesTest {

  @Test
  void constructor_positive_usesDefaults() {
    var properties = new KafkaTenantFilterProperties();

    assertThat(properties.isEnabled()).isFalse();
    assertThat(properties.isIgnoreEmptyBatch()).isTrue();
    assertThat(properties.getTenantDisabledStrategy()).isEqualTo(SKIP);
    assertThat(properties.getAllTenantsDisabledStrategy()).isEqualTo(FAIL);
  }
}
