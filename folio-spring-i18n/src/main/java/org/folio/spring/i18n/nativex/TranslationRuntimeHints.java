package org.folio.spring.i18n.nativex;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM native-image reachability hints for {@code folio-spring-i18n}.
 *
 * <p>Wired into the context via {@code @ImportRuntimeHints} on
 * {@link org.folio.spring.i18n.config.TranslationConfiguration} (an auto-configuration entry point).</p>
 */
public class TranslationRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    // TranslationService enumerates translation files at runtime via
    // ResourcePatternResolver.getResources("classpath:%s*/*.json") for each configured directory.
    // The default directories (TranslationConfiguration) are "/translations/" and
    // "/custom-translations/", so the resolved patterns are translations/<module>/<locale>.json and
    // custom-translations/<module>/<locale>.json. Native-image does not scan the classpath at
    // runtime — these resource paths must be declared as patterns.
    hints.resources().registerPattern("translations/*/*.json");
    hints.resources().registerPattern("custom-translations/*/*.json");

    // ICU4J (com.ibm.icu:icu4j) locale data is bundled as resources. Recent ICU4J ships its own
    // GraalVM metadata; only add hints.resources().registerPattern("com/ibm/icu/impl/data/icudt*/*")
    // here if `native:compile` / list-libraries-missing-metadata reports ICU4J as missing.
  }
}
