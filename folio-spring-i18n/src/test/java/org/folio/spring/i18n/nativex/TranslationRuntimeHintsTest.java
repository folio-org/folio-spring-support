package org.folio.spring.i18n.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

@UnitTest
class TranslationRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  TranslationRuntimeHintsTest() {
    new TranslationRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registersTranslationResourcePatterns() {
    assertThat(RuntimeHintsPredicates.resource().forResource("translations/mod-foo/en.json"))
      .accepts(hints);
    assertThat(RuntimeHintsPredicates.resource().forResource("custom-translations/mod-foo/en.json"))
      .accepts(hints);
  }
}
