package org.folio.spring.service;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SystemUserPropertiesTest {

  @Test
  void rejectEmptyPassword() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> new SystemUserProperties("username", "", "lastname", "path"))
      .withMessage("system user password must be configured to be non-empty");
  }

  @Test
  void rejectNullPassword() {
    assertThatIllegalArgumentException()
      .isThrownBy(() -> new SystemUserProperties("username", null, "lastname", "path"))
      .withMessage("system user password must be configured to be non-empty");
  }

}
