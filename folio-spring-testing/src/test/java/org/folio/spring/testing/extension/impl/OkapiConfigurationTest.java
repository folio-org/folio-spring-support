package org.folio.spring.testing.extension.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class OkapiConfigurationTest {

  @Test
  void getOkapiUrl_positive() {
    var okapiConfiguration = new OkapiConfiguration(null, 1000);

    assertEquals("http://localhost:1000", okapiConfiguration.getOkapiUrl());
  }
}
