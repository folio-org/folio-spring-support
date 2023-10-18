package org.folio.spring.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SystemUserPropertiesTest {

  @Test
  void rejectEmptyPassword() {
    assertThatThrownBy(() -> new SystemUserProperties("username", "", "lastname", "path"))
        .as("system user password must be configured to be non-empty");
  }

  @Test
  void rejectNullPassword() {
    assertThatThrownBy(() -> new SystemUserProperties("username", null, "lastname", "path"))
        .as("system user password must be configured to be non-empty");
  }

}
