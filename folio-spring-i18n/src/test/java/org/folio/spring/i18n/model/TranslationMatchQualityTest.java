package org.folio.spring.i18n.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.FileSystemResource;

@UnitTest
class TranslationMatchQualityTest {

  protected static TranslationFile FILE_NO_NAME = new TranslationFile(
    Arrays.asList(new FileSystemResource(""))
  );
  protected static TranslationFile FILE_EN = new TranslationFile(
    Arrays.asList(new FileSystemResource("en.json"))
  );
  protected static TranslationFile FILE_EN_US = new TranslationFile(
    Arrays.asList(new FileSystemResource("en_us.json"))
  );

  protected static Locale LOCALE_EN = Locale.ENGLISH;
  protected static Locale LOCALE_EN_US = Locale.US;
  protected static Locale LOCALE_EN_CA = Locale.CANADA;
  protected static Locale LOCALE_FR_FR = Locale.FRANCE;

  static List<Arguments> mismatchedBaseCases() {
    return Arrays.asList(
      arguments(LOCALE_EN_US, FILE_NO_NAME, TranslationMatchQuality.NO_MATCH),
      arguments(LOCALE_EN, FILE_NO_NAME, TranslationMatchQuality.NO_MATCH),
      arguments(LOCALE_FR_FR, FILE_EN, TranslationMatchQuality.NO_MATCH),
      arguments(LOCALE_FR_FR, FILE_EN_US, TranslationMatchQuality.NO_MATCH)
    );
  }

  static List<Arguments> languageOnlyCases() {
    return Arrays.asList(
      arguments(LOCALE_EN, FILE_EN, TranslationMatchQuality.PERFECT_MATCH),
      arguments(LOCALE_EN_US, FILE_EN, TranslationMatchQuality.LANG_ONLY),
      arguments(LOCALE_EN, FILE_EN_US, TranslationMatchQuality.LANG_ONLY)
    );
  }

  static List<Arguments> languageAndRegionCases() {
    return Arrays.asList(
      arguments(LOCALE_EN_CA, FILE_EN_US, TranslationMatchQuality.LANG_ONLY),
      arguments(LOCALE_EN_US, FILE_EN_US, TranslationMatchQuality.PERFECT_MATCH)
    );
  }

  @ParameterizedTest
  @MethodSource(
    { "mismatchedBaseCases", "languageOnlyCases", "languageAndRegionCases" }
  )
  void testMatchQuality(
    Locale locale,
    TranslationFile file,
    TranslationMatchQuality expected
  ) {
    assertThat(TranslationMatchQuality.getQuality(locale, file), is(expected));
  }
}
