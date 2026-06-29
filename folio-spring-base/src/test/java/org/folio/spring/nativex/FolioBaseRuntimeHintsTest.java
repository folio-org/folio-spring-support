package org.folio.spring.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioPostgresDatabase;
import org.folio.spring.logging.FolioLoggingContextLookup;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.DecoratingProxy;

@UnitTest
class FolioBaseRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  FolioBaseRuntimeHintsTest() {
    new FolioBaseRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registersFolioExecutionContextJdkProxy() {
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(
        FolioExecutionContext.class, SpringProxy.class, Advised.class, DecoratingProxy.class))
      .accepts(hints);
  }

  @Test
  void registersLog4j2PluginReflection() {
    assertThat(RuntimeHintsPredicates.reflection().onType(FolioLoggingContextLookup.class))
      .accepts(hints);
  }

  @Test
  void registersLiquibaseDatabaseReflection() {
    assertThat(RuntimeHintsPredicates.reflection().onType(FolioPostgresDatabase.class))
      .accepts(hints);
  }
}
