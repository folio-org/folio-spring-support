package org.folio.spring.i18n.model;

import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.ULocale;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;

/**
 * This object represents the relationship between a locale and a
 * {@link TranslationFile TranslationFile}.  The quality of this relationship is determined by how
 * well the file's locale matches up with the one in use.  This object will also contain fallbacks,
 * such as alternative languages for the user, or the server's default locale, to provide more
 * seamless support for missing translations.
 */
@Log4j2
@Getter
@ToString
@EqualsAndHashCode
public final class TranslationMap {

  private final Locale locale;

  private final TranslationMatchQuality quality;

  private final TranslationFile file;

  private final Map<String, String> patterns;

  @Nullable
  private final TranslationMap fallback;

  /**
   * Create a TranslationMap for a given locale based off of a file with contents and a given fallback.
   *
   * @param locale the associated locale
   * @param file the {@link TranslationFile TranslationFile} to infer quality from
   * @param patterns the patterns to use
   * @param fallback the TranslationMap to search if a given translation cannot be found
   */
  private TranslationMap(
    Locale locale,
    TranslationFile file,
    Map<String, String> patterns,
    @Nullable TranslationMap fallback
  ) {
    this.locale = locale;
    this.quality = TranslationMatchQuality.getQuality(locale, file);
    this.file = file;
    this.patterns = patterns;
    this.fallback = fallback;
  }

  /**
   * Create a TranslationMap for a given locale based off of a file with a given fallback.
   *
   * @param locale the associated locale
   * @param file the {@link TranslationFile TranslationFile} to read from
   * @param fallback the TranslationMap to search if a given translation cannot be found
   */
  public TranslationMap(Locale locale, TranslationFile file, @Nullable TranslationMap fallback) {
    this(locale, file, file.getPatterns(), fallback);
  }

  /**
   * Create a <em>default</em> TranslationMap for a given locale based off of a file.
   * This constructor should only be used for default translation maps; all others
   * should use the other constructor and provide a fallback.
   *
   * @param locale the associated locale
   * @param file the {@link TranslationFile TranslationFile} to read from
   */
  public TranslationMap(Locale locale, TranslationFile file) {
    this(locale, file, null);
  }

  /**
   * Create a new TranslationMap for a given locale.  This is primarily used when cloning
   * existing TranslationMaps for use as fallbacks for another (for caching reasons, this
   * makes more sense than re-reading files each time)
   *
   * @param newLocale the locale for the new TranslationMap to use
   * @return the TranslationMap with {@code newLocale}
   */
  public TranslationMap withLocale(Locale newLocale) {
    return new TranslationMap(newLocale, this.file, this.patterns, this.fallback);
  }

  /**
   * Get the ICU format string associated with a given key for this translation.
   *
   * @param key the key to lookup
   * @return one of the following, with this precedence:
   *   <ol>
   *     <li>the ICU format string for this locale, if it is known</li>
   *     <li>the ICU format string for a locale with a worse {@code TranslationMatchQuality},
   *       such as one without a specific country, recursing through the {@code fallback}</li>
   *     <li>the ICU format string for the server's default locale</li>
   *     <li>the provided key</li>
   *   </ol>
   */
  public String get(String key) {
    String pattern = this.patterns.get(key);

    if (pattern == null && this.fallback != null) {
      pattern = this.fallback.get(key);
    }

    if (pattern == null) {
      log.warn("Could not resolve key {} in any translation", key);
      // fallback to translation key itself
      pattern = key;
    }

    return pattern;
  }

  /**
   * Check if a key exists in the translation map (or a fallback).
   *
   * @param key
   */
  public boolean hasKey(String key) {
    return this.patterns.containsKey(key) || (this.fallback != null && this.fallback.hasKey(key));
  }

  /**
   * Format an ICU format string (found by its key), supplying a series of named arguments as key
   * value pairs.  For example: {@code format("Hello {name}", "name", parameterValue)}
   *
   * @param zone the timezone to use for date formatting
   * @param key the key of the format string
   * @param args pairs of keys and values to interpolate
   * @return the formatted string
   */
  public String format(ZoneId zone, String key, Object... args) {
    return format(zone, new MessageFormat(get(key)), buildArgs(zone, args));
  }

  /**
   * Like {@link #format(ZoneId, String, Object...)}, but uses a message format string rather than looking it up in the map
   *
   * @param zone the timezone to use for date formatting
   * @param format the format string
   * @param args pairs of keys and values to interpolate
   * @return the formatted string
   */
  public String formatString(ZoneId zone, String format, Object... args) {
    return format(zone, new MessageFormat(format), buildArgs(zone, args));
  }

  /** Build associative map to pass to {@link MessageFormat#format(String, Map)} */
  private static Map<String, Object> buildArgs(ZoneId zone, Object... args) {
    if (args.length % 2 != 0) {
      throw new IllegalArgumentException(
        "An odd number of arguments were passed to buildArgs; even amounts are needed to construct key-value pairs"
      );
    }

    Map<String, Object> map = new HashMap<>();

    for (int i = 0; i < args.length; i += 2) {
      // Sadly, ICU formatting strings only support date formats with the old Date class :(

      if (args[i + 1] instanceof Instant instant) {
        args[i + 1] = Date.from(instant);
      }
      if (args[i + 1] instanceof LocalDateTime date) {
        args[i + 1] = Date.from(date.atZone(zone).toInstant());
      }
      if (args[i + 1] instanceof OffsetDateTime date) {
        args[i + 1] = Date.from(date.toInstant());
      }
      if (args[i + 1] instanceof ZonedDateTime date) {
        args[i + 1] = Date.from(date.toInstant());
      }

      // ICU will chop off the time, so the time we use is irrelevant
      if (args[i + 1] instanceof LocalDate date) {
        args[i + 1] = Date.from(date.atStartOfDay(zone).toInstant());
      }

      // ICU will chop off the date, so the date we use is irrelevant
      if (args[i + 1] instanceof LocalTime time) {
        args[i + 1] = Date.from(time.atDate(LocalDate.now()).atZone(zone).toInstant());
      }
      if (args[i + 1] instanceof OffsetTime time) {
        args[i + 1] = Date.from(time.atDate(LocalDate.now()).toInstant());
      }

      map.put(args[i].toString(), args[i + 1]);
    }

    return map;
  }

  /** Format a message */
  private String format(ZoneId zone, MessageFormat message, Map<String, Object> args) {
    message.setLocale(new ULocale("%s@timezone=%s".formatted(this.locale.toString(), zone.toString())));
    return message.format(args);
  }
}
