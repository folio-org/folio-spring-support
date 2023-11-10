package org.folio.spring.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Locale;
import org.folio.spring.config.TranslationConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class TranslationServiceResolutionTest {

  private TranslationService getService(String path) {
    TranslationConfiguration translationConfiguration = new TranslationConfiguration(
      "/test-translations/test-" + path + "/",
      Locale.ENGLISH
    );

    return new TranslationService(
      new PathMatchingResourcePatternResolver(),
      translationConfiguration
    );
  }

  @Test
  void testGetTranslationPresent() {
    TranslationService service = getService("multiple");

    assertThat(service.getTranslation(Locale.CHINA, null), is(nullValue()));

    assertThat(
      service.getTranslation(Locale.CANADA, null).get("mod-foo.en_base_only"),
      is("[mod-foo] In en base!")
    );
    assertThat(
      service.getTranslation(Locale.CANADA, null).get("mod-foo.en_ca_only"),
      is("[mod-foo] In en_ca!")
    );
    assertThat(
      service.getTranslation(Locale.CANADA, null).get("mod-foo.en_only"),
      is("[mod-foo] In en_ca!")
    );

    assertThat(
      service
        .getTranslation(new Locale("es", "sp"), null)
        .get("mod-foo.es_only"),
      is("[mod-foo] In es base!")
    );

    assertThat(
      service.getTranslation(Locale.UK, null).get("mod-foo.en_base_only"),
      is("[mod-foo] In en base!")
    );
    assertThat(
      service.getTranslation(Locale.UK, null).get("mod-foo.en_ca_only"),
      is("mod-foo.en_ca_only")
    );
  }

  @Test
  void testDefaultLocale() {
    TranslationService service = new TranslationService(
      new PathMatchingResourcePatternResolver(),
      new TranslationConfiguration(
        "/test-translations/test-normal/",
        new Locale("test", "")
      )
    );

    assertThat(service.getCurrentLocale(), is(new Locale("test", "")));

    assertThat(
      service.getDefaultTranslation().getLocale(),
      is(new Locale("test", ""))
    );
  }

  @Test
  void testDefaultLocaleFallback() {
    TranslationService service = getService("multiple");

    Locale.setDefault(new Locale("test", ""));
    assertThat(service.getDefaultTranslation().getLocale(), is(Locale.ENGLISH));
  }

  @Test
  void testDefaultLocaleException() {
    TranslationService service = getService("normal");

    // only available are test.json, so FR_FR and EN_US don't match
    Locale.setDefault(Locale.FRANCE);
    assertThrows(
      IllegalStateException.class,
      () -> service.getDefaultTranslation(),
      "No available translations causes an IllegalStateException"
    );
  }

  @Test
  void testBestTranslation() {
    TranslationService service = getService("multiple");

    assertThat(
      service
        .getBestTranslation(Arrays.asList(Locale.US, Locale.FRANCE))
        .getLocale(),
      is(Locale.US)
    );
    assertThat(
      service
        .getBestTranslation(Arrays.asList(Locale.FRANCE, Locale.US))
        .getLocale(),
      is(Locale.FRANCE)
    );
    assertThat(
      service
        .getBestTranslation(Arrays.asList(Locale.CHINESE, Locale.US))
        .getLocale(),
      is(Locale.US)
    );
    assertThat(
      service.getBestTranslation(Arrays.asList(Locale.CHINESE)).getLocale(),
      is(Locale.ENGLISH) // test default (Locale.ENGLISH above)
    );
    assertThat(
      service.getBestTranslation(Arrays.asList()).getLocale(),
      is(Locale.ENGLISH) // test default (Locale.ENGLISH above)
    );

    assertThat(
      service.format("mod-foo.foo"),
      is("[mod-foo] en {test}") // en is test default (Locale.ENGLISH above)
    );
  }
}
