package org.folio.spring.testing.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KafkaContainerExtensionTest {

  private static final Pattern IP_PATTERN =
    Pattern.compile("^((?:\\b\\.?(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){4}|localhost):\\d{2,5}$");
  private static final String PROPERTY_NAME = "spring.kafka.bootstrap-servers";
  private final KafkaContainerExtension extension = new KafkaContainerExtension();

  @Test
  void beforeAllAddSystemProperties_positive() {
    extension.beforeAll(null);
    assertThat(System.getProperty(PROPERTY_NAME)).matches(IP_PATTERN);
  }

  @Test
  void afterAllCleanSystemProperties_positive() {
    System.setProperty(PROPERTY_NAME, "prop-value");
    extension.afterAll(null);
    assertThat(System.getProperty(PROPERTY_NAME)).isNull();
  }
}
