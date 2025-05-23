package org.folio.spring.i18n.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.folio.spring.i18n.config.TranslationConfiguration;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@UnitTest
class TranslationServiceResolutionTest {

  private TranslationService getService(String path) {
    TranslationConfiguration translationConfiguration = new TranslationConfiguration(
      List.of("/test-translations/test-" + path + "/"),
      Locale.ENGLISH
    );

    return new TranslationService(
      new PathMatchingResourcePatternResolver(),
      translationConfiguration
    );
  }

  static List<Arguments> getTranslationCases() {
    return Arrays.asList(
      arguments(Locale.CANADA, "mod-foo.en_base_only", "[mod-foo] In en base!"),
      arguments(Locale.CANADA, "mod-foo.en_ca_only", "[mod-foo] In en_ca!"),
      arguments(Locale.CANADA, "mod-foo.en_only", "[mod-foo] In en_ca!"),
      // fallback to best matching language
      arguments(
        Locale.of("es", "sp"),
        "mod-foo.es_only",
        "[mod-foo] In es base!"
      ),
      arguments(Locale.UK, "mod-foo.en_base_only", "[mod-foo] In en base!"),
      arguments(Locale.UK, "mod-foo.en_ca_only", "mod-foo.en_ca_only")
    );
  }

  @ParameterizedTest
  @MethodSource("getTranslationCases")
  void testGetTranslationPresent(Locale locale, String key, String expected) {
    TranslationService service = getService("multiple");

    var translation = service.getTranslation(locale, null);
    assertNotNull(translation);
    assertThat(translation.get(key), is(expected));
  }

  @Test
  void testGetTranslationMissing() {
    TranslationService service = getService("multiple");
    assertThat(service.getTranslation(Locale.CHINA, null), is(nullValue()));
  }

  @Test
  void testDefaultLocale() {
    TranslationService service = new TranslationService(
      new PathMatchingResourcePatternResolver(),
      new TranslationConfiguration(
        List.of("/test-translations/test-normal/"),
        Locale.of("test", "")
      )
    );

    assertThat(service.getCurrentLocale(), is(Locale.of("test", "")));

    assertThat(
      service.getFallbackTranslation().getLocale(),
      is(Locale.of("test", ""))
    );
  }

  @Test
  void testDefaultLocaleFallback() {
    TranslationService service = getService("multiple");

    Locale.setDefault(Locale.of("test", ""));
    assertThat(
      service.getFallbackTranslation().getLocale(),
      is(Locale.ENGLISH)
    );
  }

  @Test
  void testDefaultLocaleException() {
    TranslationService service = getService("normal");

    // only available are test.json, so FR_FR and EN_US don't match
    Locale.setDefault(Locale.FRANCE);
    assertThrows(
      IllegalStateException.class,
      service::getFallbackTranslation,
      "No available translations causes an IllegalStateException"
    );
  }

  static List<Arguments> bestTranslationCases() {
    return Arrays.asList(
      arguments(Arrays.asList(Locale.US, Locale.FRANCE), Locale.US),
      arguments(Arrays.asList(Locale.FRANCE, Locale.US), Locale.FRANCE),
      arguments(Arrays.asList(Locale.CHINESE, Locale.US), Locale.US),
      // region is preferred when possible
      arguments(Arrays.asList(Locale.CANADA, Locale.US), Locale.CANADA),
      arguments(Arrays.asList(Locale.US, Locale.CANADA), Locale.US),
      // we don't have en_test, but we'd rather pick the user's first requested language
      // over a better fitting secondary language
      arguments(
        Arrays.asList(Locale.of("en", "test"), Locale.FRANCE),
        Locale.of("en", "test")
      ),
      // test default (Locale.ENGLISH in TranslationConfiguration)
      arguments(singletonList(Locale.CHINESE), Locale.ENGLISH),
      arguments(emptyList(), Locale.ENGLISH)
    );
  }

  @ParameterizedTest
  @MethodSource("bestTranslationCases")
  void testBestTranslation(
    List<Locale> desiredLocales,
    Locale expectedResolution
  ) {
    TranslationService service = getService("multiple");

    assertThat(
      service.getBestTranslation(desiredLocales).getLocale(),
      is(expectedResolution)
    );
  }

  @Test
  void testDefaultFormat() {
    TranslationService service = getService("multiple");

    // en (Locale.ENGLISH) is test default from TranslationConfiguration
    assertThat(service.format("mod-foo.foo"), is("[mod-foo] en {test}"));
  }

  @Test
  void testDefaultFormatWhenSeveralKeysProvided() {
    TranslationService service = getService("multiple");

    assertThat(service.format(new String[]{"mod-foo.bar", "mod-foo.foo"}), is("[mod-foo] en {test}"));
  }

  @Test
  void testDefaultFormatWhenSeveralKeysProvidedAndNoTranslationExist() {
    TranslationService service = getService("multiple");

    assertThat(service.format(new String[]{"mod-foo.bar", "mod-foo.baz"}), is("mod-foo.bar"));
  }

  @NullAndEmptySource
  @ParameterizedTest
  void testDefaultFormatWhenKeysAreNotProvided(String[] keys) {
    TranslationService service = getService("multiple");

    var exception = assertThrows(IllegalStateException.class, () -> service.format(keys));
    assertThat(exception.getMessage(), is("Keys must be provided for formatting, but provided %s"
      .formatted(Arrays.toString(keys))));
  }

  @Test
  void testAcceptLanguage() {
    TranslationService service = getService("multiple");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPreferredLocales(
      Arrays.asList(Locale.CANADA, Locale.FRANCE, Locale.US)
    );
    RequestContextHolder.setRequestAttributes(
      new ServletRequestAttributes(request)
    );

    assertThat(
      service.format("mod-foo.foo"),
      is("[mod-foo] en_ca {test}") // en is test default (Locale.ENGLISH above)
    );

    request.setPreferredLocales(
      Arrays.asList(Locale.CHINESE, Locale.FRANCE, Locale.US)
    );
    RequestContextHolder.setRequestAttributes(
      new ServletRequestAttributes(request)
    );

    assertThat(
      service.getCurrentLocales(),
      contains(Locale.CHINESE, Locale.FRANCE, Locale.US)
    );
    assertThat(service.getCurrentLocale(), is(Locale.CHINESE));
    // chinese is not provided in translation files, so we fallback
    assertThat(
      service.format("mod-foo.foo"),
      is("[mod-foo] fr_fr {test}") // en is test default (Locale.ENGLISH above)
    );
  }

  @Test
  void testLocale() {
    TranslationService service = getService("multiple");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPreferredLocales(
      singletonList(Locale.ENGLISH)
    );
    RequestContextHolder.setRequestAttributes(
      new ServletRequestAttributes(request)
    );

    assertThat(
      service.format("mod-foo.days", "count", 1_000_000),
      is("1,000,000 days")
    );

    request.setPreferredLocales(
      singletonList(Locale.FRENCH)
    );
    RequestContextHolder.setRequestAttributes(
      new ServletRequestAttributes(request)
    );

    assertThat(
      // https://www.unicode.org/cldr/charts/45/supplemental/language_plural_rules.html#fr
      service.format("mod-foo.days", "count", 1_000_000),
      is("1 000 000 de jours")  // plural {many}
    );
  }

  @Test
  void testListFormat() {
    TranslationService service = getService("multiple");

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setPreferredLocales(singletonList(Locale.US));
    RequestContextHolder.setRequestAttributes(
      new ServletRequestAttributes(request)
    );

    assertThat(
      service.formatList(Arrays.asList("A", "B", "C")),
      is("A, B, and C")
    );

    request = new MockHttpServletRequest();
    request.setPreferredLocales(singletonList(Locale.FRENCH));
    RequestContextHolder.setRequestAttributes(
      new ServletRequestAttributes(request)
    );

    assertThat(
      service.formatList(Arrays.asList("A", "B", "C")),
      is("A, B et C")
    );
  }

  @AfterEach
  void cleanup() {
    RequestContextHolder.resetRequestAttributes();
  }
}
