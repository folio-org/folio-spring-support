package org.folio.spring.testing.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

@UnitTest
class OkapiExtensionTest {

  private final OkapiExtension extension = new OkapiExtension();

  @Test
  void beforeAllAddSystemProperties_positive() {
    var mock = mock(ExtensionContext.class);
    when(mock.getRequiredTestClass()).thenAnswer(unused -> this.getClass());
    extension.beforeAll(mock);
    assertThat(System.getProperty("folio.okapi-url")).contains("http://localhost");
  }

  @Test
  void afterAllAddSystemProperties_positive() {
    extension.afterAll(null);
    assertThat(System.getProperty("folio.okapi-url")).isNull();
  }

}
