package org.folio.spring.i18n.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.FileSystemResource;

class TranslationFileNameExtractionTest {

  static List<Arguments> fullExtractionCases() {
    return Arrays.asList(
      arguments("en_us.json", new LanguageRegionPair("en", "us")),
      arguments("en_us", new LanguageRegionPair("en", "us")),
      arguments("EN_US", new LanguageRegionPair("en", "us")),
      arguments("es_419", new LanguageRegionPair("es", "419")),
      arguments("es_419.json", new LanguageRegionPair("es", "419"))
    );
  }

  static List<Arguments> partialExtractionCases() {
    return Arrays.asList(
      arguments("en.json", new LanguageRegionPair("en", "*")),
      arguments("en_", new LanguageRegionPair("en", "*")),
      arguments("en_ ", new LanguageRegionPair("en", "*")),
      arguments("_us", new LanguageRegionPair("*", "us")),
      arguments(" _us", new LanguageRegionPair("*", "us")),
      arguments("ber", new LanguageRegionPair("ber", "*")),
      arguments("ber.json", new LanguageRegionPair("ber", "*"))
    );
  }

  static List<Arguments> emptyExtractionCases() {
    return Arrays.asList(
      arguments("", new LanguageRegionPair("*", "*")),
      arguments("_", new LanguageRegionPair("*", "*")),
      arguments(null, new LanguageRegionPair("*", "*"))
    );
  }

  @ParameterizedTest
  @MethodSource(
    { "fullExtractionCases", "partialExtractionCases", "emptyExtractionCases" }
  )
  void testPartsExtraction(String filename, LanguageRegionPair expectedPair) {
    assertThat(
      filename + " parses to " + expectedPair,
      TranslationFile.getLanguageRegion(filename),
      is(expectedPair)
    );
  }

  static List<Arguments> invalidExtractionCases() {
    return Arrays.asList(
      arguments("en_us_extra.json"),
      arguments("en__foo.json"),
      arguments("en_us_extra"),
      arguments("en__foo")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidExtractionCases")
  void testInvalidPartsExtraction(String filename) {
    assertThrows(
      IllegalArgumentException.class,
      () -> TranslationFile.getLanguageRegion(filename),
      "Filenames with more than two components are invalid"
    );
  }

  @Test
  void testInstancePartsExtraction() {
    assertThat(
      "Regular en_us.json parses as an instance the same as statically",
      new TranslationFile(Arrays.asList(new FileSystemResource("en_us.json")))
        .getLanguageRegion(),
      is(equalTo(TranslationFile.getLanguageRegion("en_us.json")))
    );
  }

  @Test
  void testEmptyGetMap() {
    assertThat(
      new TranslationFile(Arrays.asList(new FileSystemResource("invalid.json")))
        .getPatterns()
        .values(),
      is(empty())
    );
  }
}
