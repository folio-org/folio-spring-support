package org.folio.spring.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

class TranslationMapTest {

  static TranslationFile FILE_EN_CA;
  static TranslationFile FILE_EN_BASE;
  static TranslationFile FILE_FR_FR;

  static {
    ResourcePatternResolver rr = new PathMatchingResourcePatternResolver();
    FILE_EN_CA =
      new TranslationFile(
        Arrays.asList(
          rr.getResource(
            "classpath:/test-translations/test-multiple/mod-bar/en_ca.json"
          ),
          rr.getResource(
            "classpath:/test-translations/test-multiple/mod-foo/en_ca.json"
          )
        )
      );
    FILE_EN_BASE =
      new TranslationFile(
        Arrays.asList(
          rr.getResource(
            "classpath:/test-translations/test-multiple/mod-bar/en.json"
          ),
          rr.getResource(
            "classpath:/test-translations/test-multiple/mod-foo/en.json"
          )
        )
      );
    FILE_FR_FR =
      new TranslationFile(
        Arrays.asList(
          rr.getResource(
            "classpath:/test-translations/test-multiple/mod-bar/fr_fr.json"
          ),
          rr.getResource(
            "classpath:/test-translations/test-multiple/mod-foo/fr_fr.json"
          )
        )
      );
  }

  @Test
  void testDefaultTranslationMapFromFile() {
    TranslationMap france = new TranslationMap(Locale.FRANCE, FILE_FR_FR);

    assertThat(france.getQuality(), is(TranslationMatchQuality.PERFECT_MATCH));
    assertThat(france.getFallback(), is(nullValue()));
  }

  /**
   * Tests .get as a result
   */
  @Test
  void testFallbackFormat() {
    TranslationMap englishBase = new TranslationMap(
      Locale.FRANCE,
      FILE_EN_BASE
    );
    TranslationMap englishCa = new TranslationMap(
      Locale.FRANCE,
      FILE_EN_CA,
      englishBase
    );
    TranslationMap france = new TranslationMap(
      Locale.FRANCE,
      FILE_FR_FR,
      englishCa
    );

    assertThat(
      france.format("mod-foo.foo", "test", "bar"),
      is("[mod-foo] fr_fr bar")
    );
    assertThat(
      france.format("mod-bar.foo", "test", "bar"),
      is("[mod-bar] fr_fr bar")
    );

    assertThat(france.format("mod-foo.en_only"), is("[mod-foo] In en_ca!"));
    assertThat(
      france.format("mod-foo.en_base_only"),
      is("[mod-foo] In en base!")
    );
    assertThat(
      france.format("mod-bar.en_base_only"),
      is("[mod-bar] In en base!")
    );
    assertThat(france.format("thisDoesNotExist"), is("thisDoesNotExist"));
    assertThat(
      france.format("mod-foo.thisDoesNotExist"),
      is("mod-foo.thisDoesNotExist")
    );
  }
}
