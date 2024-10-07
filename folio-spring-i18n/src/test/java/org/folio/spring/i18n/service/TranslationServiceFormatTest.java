package org.folio.spring.i18n.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import org.folio.spring.i18n.config.TranslationConfiguration;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@UnitTest
class TranslationServiceFormatTest {

  TranslationService service = new TranslationService(
    new PathMatchingResourcePatternResolver(),
    new TranslationConfiguration("/test-translations/test-empty/", Locale.ENGLISH)
  );

  static Instant JAN_1_MIDNIGHT = Instant.parse("2024-01-01T00:00:00Z");

  @Test
  void testFormatString() {
    assertThat(
      service.formatString(ZoneId.of("UTC"), "test {arg,time,short}", "arg", JAN_1_MIDNIGHT),
      is("test 12:00 AM")
    );

    assertThat(
      service.formatString(ZoneId.of("America/New_York"), "test {arg,time,short}", "arg", JAN_1_MIDNIGHT),
      is("test 7:00 PM")
    );

    assertThat(
      service.formatString(
        Locale.FRANCE,
        ZoneId.of("America/New_York"),
        "test {arg,time,short}",
        "arg",
        JAN_1_MIDNIGHT
      ),
      is("test 19:00")
    );
  }
}
