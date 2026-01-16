package org.folio.tenant.settings.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class TenantSettingsPropertiesTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void shouldAcceptValidDomain() {
    var properties = TenantSettingsProperties.of("authorities", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldAcceptDomainWithHyphens() {
    var properties = TenantSettingsProperties.of("my-test-domain", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldRejectDomainStartingWithHyphen() {
    var properties = TenantSettingsProperties.of("-invalid", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
    assertThat(violations.iterator().next().getMessage())
      .contains("Domain must follow the pattern");
  }

  @Test
  void shouldRejectDomainEndingWithHyphen() {
    var properties = TenantSettingsProperties.of("invalid-", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
    assertThat(violations.iterator().next().getMessage())
      .contains("Domain must follow the pattern");
  }

  @Test
  void shouldRejectDomainWithUppercase() {
    var properties = TenantSettingsProperties.of("Invalid", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
  }

  @Test
  void shouldRejectDomainWithNumbers() {
    var properties = TenantSettingsProperties.of("domain123", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
  }

  @Test
  void shouldRejectSingleCharacterDomain() {
    var properties = TenantSettingsProperties.of("a", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
  }

  @Test
  void shouldRejectTwoCharacterDomain() {
    var properties = TenantSettingsProperties.of("ab", true);

    Set<ConstraintViolation<TenantSettingsProperties>> violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
  }

  @Test
  void shouldDefaultPermissionCheckToTrue() {
    var properties = new TenantSettingsProperties();
    properties.setDomain("test");

    assertThat(properties.isPermissionCheckEnabled()).isTrue();
  }

  @Test
  void shouldSetPermissionCheckEnabled() {
    var properties = TenantSettingsProperties.of("test", false);

    assertThat(properties.isPermissionCheckEnabled()).isFalse();
  }

  @Test
  void shouldGetDomain() {
    var properties = TenantSettingsProperties.of("test-domain", true);

    assertThat(properties.getDomain()).isEqualTo("test-domain");
  }
}
