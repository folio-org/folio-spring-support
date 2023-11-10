package org.folio.spring.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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
}
