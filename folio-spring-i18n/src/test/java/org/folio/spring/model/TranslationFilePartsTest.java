package org.folio.spring.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.List;
import org.apache.tomcat.util.buf.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.FileSystemResource;

class TranslationFilePartsTest {

  static List<Arguments> fullPartsExtractionCases() {
    return Arrays.asList(
      arguments("en_us.json", new String[] { "en", "us" }),
      arguments("en_us", new String[] { "en", "us" }),
      arguments("EN_US", new String[] { "en", "us" }),
      arguments("es_419", new String[] { "es", "419" }),
      arguments("es_419.json", new String[] { "es", "419" })
    );
  }

  static List<Arguments> partialPartsExtractionCases() {
    return Arrays.asList(
      arguments("en.json", new String[] { "en", "*" }),
      arguments("en_us_extra", new String[] { "en", "us" }),
      arguments("en_", new String[] { "en", "*" }),
      arguments("en__foo", new String[] { "en", "*" }),
      arguments("_us", new String[] { "*", "us" }),
      arguments("ber", new String[] { "ber", "*" }),
      arguments("ber.json", new String[] { "ber", "*" })
    );
  }

  static List<Arguments> emptyPartsExtractionCases() {
    return Arrays.asList(
      arguments("", new String[] { "*", "*" }),
      arguments("_", new String[] { "*", "*" }),
      arguments(null, new String[] { "*", "*" })
    );
  }

  @ParameterizedTest
  @MethodSource(
    {
      "fullPartsExtractionCases",
      "partialPartsExtractionCases",
      "emptyPartsExtractionCases",
    }
  )
  void testPartsExtraction(String filename, String[] expectedParts) {
    assertThat(
      filename + " parses to {" + StringUtils.join(expectedParts) + "}",
      TranslationFile.getParts(filename),
      is(arrayContaining(expectedParts))
    );
  }

  @Test
  void testInstancePartsExtraction() {
    assertThat(
      "Regular en_us.json parses as an instance the same as statically",
      new TranslationFile(Arrays.asList(new FileSystemResource("en_us.json")))
        .getParts(),
      is(equalTo(TranslationFile.getParts("en_us.json")))
    );
  }

  @Test
  void testEmptyGetMap() {
    assertThat(
      new TranslationFile(Arrays.asList(new FileSystemResource("invalid.json")))
        .getMap()
        .values(),
      is(empty())
    );
  }
}
