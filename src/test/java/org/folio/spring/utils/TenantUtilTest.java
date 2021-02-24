package org.folio.spring.utils;

import static org.folio.spring.utils.TenantUtil.isValidTenantName;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TenantUtilTest {
  @ParameterizedTest
  @ValueSource(strings = {
    "fs900000",
    "main_library",
    "library10000",
    "diku"
  })
  void tenantNameIsValid(String validTenantName) {
    assertTrue(isValidTenantName(validTenantName));
  }

  @ParameterizedTest
  @CsvSource(value = {
    "drop schema",
    "a\"destroy",
    "fs 9000",
    "null",
    "\"  \""
  }, nullValues = "null")
  void tenantNameIsInvalid(String invalidTenantName) {
    assertFalse(isValidTenantName(invalidTenantName));
  }
}
