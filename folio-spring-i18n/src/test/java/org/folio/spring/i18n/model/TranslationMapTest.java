package org.folio.spring.i18n.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
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

  static final TranslationFile FILE_EN_CA;
  static final TranslationFile FILE_EN_BASE;
  static final TranslationFile FILE_FR_FR;

  static final ZoneId UTC = ZoneId.of("UTC");
  static final ZoneId EST = ZoneId.of("America/New_York");
  static final ZoneId UTC_MINUS_12 = ZoneId.of("Etc/GMT+12"); // the sign is inverted intentionally
  static final ZoneId UTC_PLUS_14 = ZoneId.of("Etc/GMT-14"); // the sign is inverted intentionally

  static {
    ResourcePatternResolver rr = new PathMatchingResourcePatternResolver();
    FILE_EN_CA =
      new TranslationFile(
        Arrays.asList(
          rr.getResource("classpath:/test-translations/test-multiple/mod-bar/en_ca.json"),
          rr.getResource("classpath:/test-translations/test-multiple/mod-foo/en_ca.json")
        )
      );
    FILE_EN_BASE =
      new TranslationFile(
        Arrays.asList(
          rr.getResource("classpath:/test-translations/test-multiple/mod-bar/en.json"),
          rr.getResource("classpath:/test-translations/test-multiple/mod-foo/en.json")
        )
      );
    FILE_FR_FR =
      new TranslationFile(
        Arrays.asList(
          rr.getResource("classpath:/test-translations/test-multiple/mod-bar/fr_fr.json"),
          rr.getResource("classpath:/test-translations/test-multiple/mod-foo/fr_fr.json")
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
    TranslationMap englishBase = new TranslationMap(Locale.FRANCE, FILE_EN_BASE);
    TranslationMap englishCa = new TranslationMap(Locale.FRANCE, FILE_EN_CA, englishBase);
    TranslationMap france = new TranslationMap(Locale.FRANCE, FILE_FR_FR, englishCa);

    assertThat(france.format(UTC, "mod-foo.foo", "test", "bar"), is("[mod-foo] fr_fr bar"));
    assertThat(france.format(UTC, "mod-bar.foo", "test", "bar"), is("[mod-bar] fr_fr bar"));

    assertThat(france.format(UTC, "mod-foo.en_only"), is("[mod-foo] In en_ca!"));
    assertThat(france.format(UTC, "mod-foo.en_base_only"), is("[mod-foo] In en base!"));
    assertThat(france.format(UTC, "mod-bar.en_base_only"), is("[mod-bar] In en base!"));
    assertThat(france.format(UTC, "thisDoesNotExist"), is("thisDoesNotExist"));
    assertThat(france.format(UTC, "mod-foo.thisDoesNotExist"), is("mod-foo.thisDoesNotExist"));
  }

  @Test
  void testHasKey() {
    TranslationMap englishBase = new TranslationMap(Locale.FRANCE, FILE_EN_BASE);
    TranslationMap france = new TranslationMap(Locale.FRANCE, FILE_FR_FR, englishBase);

    // present directly
    assertThat(france.hasKey("mod-foo.foo"), is(true));
    assertThat(englishBase.hasKey("mod-foo.en_base_only"), is(true));

    // present by fallback only
    assertThat(france.hasKey("mod-foo.en_base_only"), is(true));

    // not present
    assertThat(france.hasKey("thisDoesNotExist"), is(false));
    assertThat(france.hasKey("mod-foo.thisDoesNotExist"), is(false));
  }

  @Test
  void testComplexFormat() {
    TranslationMap map = new TranslationMap(Locale.ENGLISH, FILE_EN_BASE);

    assertThat(
      map.format(
        UTC,
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
        UTC,
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
    assertThat(map.format(UTC, "mod-foo.days", "count", i), is(expected));
  }

  @Test
  void testBadFormatArgCount() {
    TranslationMap map = new TranslationMap(Locale.ENGLISH, FILE_EN_BASE);

    assertThrows(
      IllegalArgumentException.class,
      () -> map.format(UTC, "test", "non-matched key"),
      "Every key must have a value"
    );
  }

  static Stream<Arguments> temporalDates() {
    // input value, output time zone, expected output with format date/medium
    return Stream.of(
      // any time on Jan 1 should still be printed as Jan 1 in UTC
      Arguments.of(Instant.parse("2024-01-01T00:00:00+00:00"), Locale.ENGLISH, UTC, "Jan 1, 2024"),
      Arguments.of(Instant.parse("2024-01-01T16:00:00+00:00"), Locale.ENGLISH, UTC, "Jan 1, 2024"),
      Arguments.of(Instant.parse("2024-01-01T23:59:59+00:00"), Locale.ENGLISH, UTC, "Jan 1, 2024"),
      // EST is 5 hours behind UTC, so 4am on Jan 1 in UTC is Dec 31 in EST
      Arguments.of(Instant.parse("2024-01-01T00:00:00+00:00"), Locale.ENGLISH, EST, "Dec 31, 2023"),
      Arguments.of(Instant.parse("2024-01-01T04:00:00+00:00"), Locale.ENGLISH, EST, "Dec 31, 2023"),
      Arguments.of(Instant.parse("2024-01-01T05:00:00+00:00"), Locale.ENGLISH, EST, "Jan 1, 2024"),
      Arguments.of(Instant.parse("2024-01-01T23:59:59+00:00"), Locale.ENGLISH, EST, "Jan 1, 2024"),
      // we should respect other languages' terms
      Arguments.of(Instant.parse("2024-01-01T23:59:59+00:00"), Locale.FRENCH, UTC, "1 janv. 2024"),
      // test other Temporals
      // LocalDate
      Arguments.of(LocalDate.of(2024, 1, 1), Locale.ENGLISH, UTC, "Jan 1, 2024"),
      Arguments.of(LocalDate.of(2024, 1, 1), Locale.ENGLISH, EST, "Jan 1, 2024"),
      Arguments.of(LocalDate.of(2024, 1, 1), Locale.ENGLISH, UTC_MINUS_12, "Jan 1, 2024"),
      Arguments.of(LocalDate.of(2024, 1, 1), Locale.ENGLISH, UTC_PLUS_14, "Jan 1, 2024"),
      // LocalDateTime
      Arguments.of(LocalDateTime.of(2024, 1, 1, 0, 0), Locale.ENGLISH, UTC, "Jan 1, 2024"),
      // OffsetDateTime
      Arguments.of(LocalDateTime.of(2024, 1, 1, 0, 0).atOffset(ZoneOffset.UTC), Locale.ENGLISH, UTC, "Jan 1, 2024"),
      Arguments.of(LocalDateTime.of(2024, 1, 1, 0, 0).atOffset(ZoneOffset.UTC), Locale.ENGLISH, EST, "Dec 31, 2023"),
      // ZonedDateTime
      Arguments.of(LocalDateTime.of(2024, 1, 1, 0, 0).atZone(UTC), Locale.ENGLISH, UTC, "Jan 1, 2024"),
      Arguments.of(LocalDateTime.of(2024, 1, 1, 0, 0).atZone(UTC), Locale.ENGLISH, EST, "Dec 31, 2023")
    );
  }

  @ParameterizedTest
  @MethodSource("temporalDates")
  void testTemporalAsDate(Temporal input, Locale locale, ZoneId timezone, String expected) {
    assertThat(TranslationMap.formatString(locale, timezone, "{test, date, medium}", "test", input), is(expected));
  }

  static Stream<Arguments> temporalTimes() {
    // input value, output time zone, expected output with format time/medium
    return Stream.of(
      // any time on Jan 1 should still be printed as Jan 1 in UTC
      Arguments.of(Instant.parse("2024-01-01T00:00:00+00:00"), Locale.ENGLISH, UTC, "12:00:00 AM"),
      Arguments.of(Instant.parse("2024-01-01T16:00:00+00:00"), Locale.ENGLISH, UTC, "4:00:00 PM"),
      Arguments.of(Instant.parse("2024-01-01T23:59:59+00:00"), Locale.ENGLISH, UTC, "11:59:59 PM"),
      // EST is 5 hours behind UTC
      Arguments.of(Instant.parse("2024-01-01T00:00:00+00:00"), Locale.ENGLISH, EST, "7:00:00 PM"),
      Arguments.of(Instant.parse("2024-01-01T04:00:00+00:00"), Locale.ENGLISH, EST, "11:00:00 PM"),
      Arguments.of(Instant.parse("2024-01-01T05:00:00+00:00"), Locale.ENGLISH, EST, "12:00:00 AM"),
      Arguments.of(Instant.parse("2024-01-01T23:59:59+00:00"), Locale.ENGLISH, EST, "6:59:59 PM"),
      // we should respect other locales' formatting/number systems (e.g. 24H)
      Arguments.of(Instant.parse("2024-01-01T23:59:59+00:00"), Locale.FRENCH, UTC, "23:59:59"),
      Arguments.of(Instant.parse("2024-01-01T23:59:59+00:00"), new Locale("ar"), UTC, "١١:٥٩:٥٩ م"),
      // test other Temporals
      // LocalDate
      Arguments.of(LocalDate.of(2024, 1, 1), Locale.ENGLISH, UTC, "12:00:00 AM"),
      Arguments.of(LocalDate.of(2024, 1, 1), Locale.ENGLISH, EST, "12:00:00 AM"),
      // LocalDateTime
      Arguments.of(LocalDateTime.of(2024, 1, 1, 5, 0), Locale.ENGLISH, UTC, "5:00:00 AM"),
      // LocalTime
      Arguments.of(LocalTime.of(5, 0), Locale.ENGLISH, UTC, "5:00:00 AM"),
      Arguments.of(LocalTime.of(5, 0), Locale.ENGLISH, EST, "5:00:00 AM"),
      // OffsetDateTime
      Arguments.of(LocalDateTime.of(2024, 1, 1, 2, 0).atOffset(ZoneOffset.UTC), Locale.ENGLISH, UTC, "2:00:00 AM"),
      Arguments.of(LocalDateTime.of(2024, 1, 1, 2, 0).atOffset(ZoneOffset.UTC), Locale.ENGLISH, EST, "9:00:00 PM"),
      // OffsetTime
      Arguments.of(OffsetTime.of(2, 0, 0, 0, ZoneOffset.UTC), Locale.ENGLISH, UTC, "2:00:00 AM"),
      // ZonedDateTime
      Arguments.of(LocalDateTime.of(2024, 1, 1, 2, 0).atZone(UTC), Locale.ENGLISH, UTC, "2:00:00 AM"),
      Arguments.of(LocalDateTime.of(2024, 1, 1, 2, 0).atZone(UTC), Locale.ENGLISH, EST, "9:00:00 PM")
    );
  }

  @ParameterizedTest
  @MethodSource("temporalTimes")
  void testTemporalAsTime(Temporal input, Locale locale, ZoneId timezone, String expected) {
    assertThat(TranslationMap.formatString(locale, timezone, "{test, time, medium}", "test", input), is(expected));
  }
}
