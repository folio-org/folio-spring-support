package org.folio.tenant.settings.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

@UnitTest
class TenantSettingsRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  TenantSettingsRuntimeHintsTest() {
    new TenantSettingsRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registersChangelogResource() {
    assertThat(RuntimeHintsPredicates.resource().forResource("db/tenant-settings/changelog.xml"))
      .accepts(hints);
  }
}
