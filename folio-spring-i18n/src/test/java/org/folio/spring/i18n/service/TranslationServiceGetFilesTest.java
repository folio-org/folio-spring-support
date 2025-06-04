package org.folio.spring.i18n.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.folio.spring.i18n.config.TranslationConfiguration;
import org.folio.spring.i18n.model.TranslationFile;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@UnitTest
class TranslationServiceGetFilesTest {

  private TranslationService getService(String... paths) {
    TranslationConfiguration translationConfiguration = new TranslationConfiguration(
      Arrays.stream(paths).map(path -> "/test-translations/test-" + path + "/").toList(),
      Locale.ENGLISH
    );

    return new TranslationService(
      new PathMatchingResourcePatternResolver(),
      translationConfiguration
    );
  }

  @Test
  void testMapContents() {
    List<TranslationFile> files = getService("normal")
      .getAvailableTranslationFiles();

    assertThat(
      "test-normal has exactly one TranslationFile (two modules, but only one locale)",
      files,
      hasSize(1)
    );

    Map<String, String> map = files.getFirst().getPatterns();

    assertThat(map, hasEntry("mod-foo.whoami", "foo"));
    assertThat(map, hasEntry("mod-bar.whoami", "bar"));
    assertThat(map, hasEntry("mod-foo.foo-only", "present"));
    assertThat(map, hasEntry("mod-bar.bar-only", "present"));

    // ensure keys are only part of their respective modules
    assertThat(map, not(hasKey("mod-foo.bar-only")));
    assertThat(map, not(hasKey("mod-bar.foo-only")));
  }


  @Test
  void testMapContentsMultipleDirectories() {
    List<TranslationFile> files = getService("normal", "combined")
      .getAvailableTranslationFiles();

    assertThat(
      "test-normal has exactly one TranslationFile (two modules, but only one locale)",
      files,
      hasSize(1)
    );

    Map<String, String> map = files.getFirst().getPatterns();

    assertThat(map, hasEntry("mod-foo.whoami", "foo"));
    assertThat(map, hasEntry("mod-bar.whoami", "bar"));
    assertThat(map, hasEntry("mod-foo.foo-only", "present"));
    assertThat(map, hasEntry("mod-bar.bar-only", "present"));
    assertThat(map, hasEntry("mod-bar.combined-file-only", "present!"));
    assertThat(map, hasEntry("mod-baz.combined-file-only", "baz present!"));
  }

  @Test
  void testEmptyFileList() {
    assertThat(getService("empty").getAvailableTranslationFiles(), is(empty()));
  }

  @Test
  void testMultipleLocales() {
    List<TranslationFile> files = getService("multiple")
      .getAvailableTranslationFiles();

    assertThat(files, hasSize(6)); // two modules, four in common and one locale unique to each
  }

  @Test
  void testLanguageCountryMap() {
    Map<String, Map<String, TranslationFile>> map = getService("multiple")
      .buildLanguageCountryPatternMap();

    // en and en-ca are provided, but us is not
    assertThat(map, hasKey("en"));
    assertThat(map.get("en"), hasKey("*"));
    assertThat(map.get("en"), hasKey("ca"));
    assertThat(map.get("en"), not(hasKey("us")));
    assertThat(
      map.get("en").get("*"),
      is(not(equalTo(map.get("en").get("ca"))))
    );

    // es is provided, so no assumptions about variants should be filled
    assertThat(map, hasKey("es"));
    assertThat(map.get("es"), hasKey("*"));
    assertThat(map.get("es"), not(hasKey("mx")));

    // fr-fr is provided; this should backfill to be fr-*
    assertThat(map, hasKey("fr"));
    assertThat(map.get("fr"), hasKey("*"));
    assertThat(map.get("fr"), hasKey("fr"));
    assertThat(map.get("fr"), not(hasKey("zz")));
    assertThat(map.get("fr").get("*"), is(equalTo(map.get("fr").get("fr"))));

    // each of these are only in one module, but both should be present
    assertThat(map, hasKey("de"));
    assertThat(map, hasKey("it"));

    // not present
    assertThat(map, not(hasKey("zz")));
  }

  @Test
  void testNonexistentDirectoryReturnsEmptyList() {
    TranslationService service = getService("nonexistent");

    var actual = service.getAvailableTranslationFiles();

    assertThat(actual, is(empty()));
  }
}
