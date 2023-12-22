package org.folio.spring.testing.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.testing.type.IntegrationTest;
import org.junit.jupiter.api.Test;

@IntegrationTest
class PostgresContainerExtensionTest {

  private final PostgresContainerExtension extension = new PostgresContainerExtension();

  @Test
  void beforeAllAddSystemProperties_positive() {
    extension.beforeAll(null);
    assertThat(System.getProperty("spring.datasource.url")).contains("folio_test");
    assertThat(System.getProperty("spring.datasource.username")).isEqualTo("folio_admin");
    assertThat(System.getProperty("spring.datasource.password")).isEqualTo("password");
  }

  @Test
  void afterAllAddSystemProperties_positive() {
    extension.afterAll(null);
    assertThat(System.getProperty("spring.datasource.url")).isNull();
  }
}
