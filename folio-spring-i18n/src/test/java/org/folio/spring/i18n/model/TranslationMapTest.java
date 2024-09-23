package org.folio.spring.i18n.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@UnitTest
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
   * Tests .get as a result.
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

  @Test
  void testComplexFormat() {
    TranslationMap map = new TranslationMap(Locale.ENGLISH, FILE_EN_BASE);

    assertThat(
      map.format(
        "mod-foo.complex",
        "text",
        "placeholder",
        "count",
        1,
        "date",
        LocalDate.of(2000, 1, 2),
        "time",
        LocalTime.of(1, 12)
      ),
      is("placeholder, #one, date Jan 2, 2000, time 1:12 AM")
    );
    assertThat(
      map.format(
        "mod-foo.complex",
        "text",
        "placeholder2",
        "count",
        2,
        "date",
        LocalDate.of(1991, 11, 23),
        "time",
        LocalTime.of(15, 57)
      ),
      is("placeholder2, #other, date Nov 23, 1991, time 3:57 PM")
    );
  }

  static Stream<Arguments> testPlural() {
    // https://www.unicode.org/cldr/charts/45/supplemental/language_plural_rules.html#fr
    return Stream.of(
        Arguments.of(Locale.FRENCH, FILE_FR_FR, 1, "1 jour"),
        Arguments.of(Locale.FRENCH, FILE_FR_FR, 2, "2 jours"),
        Arguments.of(Locale.FRENCH, FILE_FR_FR, 1_000_000, "1 000 000 de jours"),
        Arguments.of(Locale.ENGLISH, FILE_EN_BASE, 1, "1 day"),
        Arguments.of(Locale.ENGLISH, FILE_EN_BASE, 2, "2 days"),
        Arguments.of(Locale.ENGLISH, FILE_EN_BASE, 1_000_000, "1,000,000 days")
        );
  }

  @ParameterizedTest
  @MethodSource
  void testPlural(Locale locale, TranslationFile translationFile, int i, String expected) {
    var map = new TranslationMap(locale, translationFile);
    assertThat(map.format("mod-foo.days", "count", i), is(expected));
  }

  @Test
  void testBadFormat() {
    TranslationMap map = new TranslationMap(Locale.ENGLISH, FILE_EN_BASE);

    assertThrows(
      IllegalArgumentException.class,
      () -> map.format("test", "non-matched key"),
      "Every key must have a value"
    );
  }
}
