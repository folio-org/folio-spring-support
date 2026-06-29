package org.folio.spring.cql.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.cql.IgnoreAccents;
import org.folio.spring.cql.IgnoreCase;
import org.folio.spring.cql.RespectAccents;
import org.folio.spring.cql.RespectCase;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

@UnitTest
class JpaCqlRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  JpaCqlRuntimeHintsTest() {
    new JpaCqlRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registersCqlAnnotationReflection() {
    assertThat(RuntimeHintsPredicates.reflection().onType(RespectAccents.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onType(RespectCase.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onType(IgnoreAccents.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.reflection().onType(IgnoreCase.class)).accepts(hints);
  }
}
