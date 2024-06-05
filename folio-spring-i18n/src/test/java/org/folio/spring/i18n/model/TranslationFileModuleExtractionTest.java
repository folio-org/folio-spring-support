package org.folio.spring.i18n.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.List;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class TranslationFileModuleExtractionTest {

  static List<Arguments> extractionCases() {
    return Arrays.asList(
      arguments("file [/foo/bar/baz/mod-something/en.json]", "mod-something"),
      arguments("file [foo/bar/baz/mod-something/en.json]", "mod-something"),
      arguments("file [mod-test/en.json]", "mod-test"),
      arguments("file [mod-test/something-else]", "mod-test"),
      arguments("file [mod-foo/mod-bar/mod-test/mod-other]", "mod-test"),
      arguments(
        "class path resource [mod-foo/mod-bar/mod-test/mod-other]",
        "mod-test"
      ),
      arguments("file [C:/Test/translations/mod-test/mod-other]", "mod-test"),
      arguments(
        "file [C:\\Test\\translations\\mod-test\\mod-other]",
        "mod-test"
      ),
      arguments(
        "class path resource [translations\\mod-test\\mod-other]",
        "mod-test"
      )
    );
  }

  @ParameterizedTest
  @MethodSource({ "extractionCases" })
  void testModuleNameExtraction(String description, String expectedModuleName) {
    assertThat(
      description + " parses to " + expectedModuleName,
      TranslationFile.getModuleNameFromResourceDescription(description),
      is(expectedModuleName)
    );
  }

  static List<Arguments> invalidExtractionCases() {
    return Arrays.asList(
      arguments("file []"),
      arguments("not a thing"),
      arguments("[en.json]"),
      arguments("")
    );
  }

  @ParameterizedTest
  @MethodSource("invalidExtractionCases")
  void testInvalidModuleNameExtraction(String filename) {
    assertThrows(
      IllegalArgumentException.class,
      () -> TranslationFile.getModuleNameFromResourceDescription(filename),
      "Filenames with more than two components are invalid"
    );
  }
}
