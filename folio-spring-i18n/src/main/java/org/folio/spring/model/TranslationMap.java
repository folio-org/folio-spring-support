package org.folio.spring.model;

import com.ibm.icu.text.MessageFormat;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
@ToString
@EqualsAndHashCode
public class TranslationMap {

  @Getter
  protected final Locale locale;

  @Getter
  protected final TranslationMatchQuality quality;

  @Getter
  protected final TranslationFile file;

  protected final Map<String, String> patterns;

  @Getter
  @Nullable
  protected final TranslationMap fallback;

  /**
   * Create a TranslationMap for a given locale based off of a file with contents and a given fallback.
   *
   * @param locale the associated locale
   * @param file the {@link TranslationFile TranslationFile} to infer quality from
   * @param patterns the patterns to use
   * @param fallback the TranslationMap to search if a given translation cannot be found
   */
  protected TranslationMap(
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
  public TranslationMap(
    Locale locale,
    TranslationFile file,
    @Nullable TranslationMap fallback
  ) {
    this(locale, file, file.getMap(), fallback);
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
    return new TranslationMap(
      newLocale,
      this.file,
      this.patterns,
      this.fallback
    );
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
      log.error("Could not resolve key {} in any translation", key);
      // fallback to translation key itself
      pattern = key;
    }

    return pattern;
  }

  /**
   * Format an ICU format string (found by its key), supplying a series of named arguments as key
   * value pairs.  For example: {@code format("Hello {name}", "name", parameterValue)}
   *
   * @param key the key of the format string
   * @param args pairs of keys and values to interpolate
   * @return the formatted string
   */
  public String format(String key, Object... args) {
    for (int i = 0; i < args.length; i++) {
      // Convert LocalDate to Date
      // Sadly, ICU formatting strings only support date formats with the old Date class :(
      if (args[i] instanceof LocalDate date) {
        args[i] =
          Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
      }
      // Same for LocalTime
      if (args[i] instanceof LocalTime time) {
        args[i] =
          Date.from(
            time
              .atDate(LocalDate.now())
              .atZone(ZoneId.systemDefault())
              .toInstant()
          );
      }
    }
    return MessageFormat.format(this.get(key), buildMap(args));
  }

  /**
   * Get a Map&lt;String, Object&gt; from a set of String -&gt; Object pairs.
   *
   * @param args pairs of elements, e.g. key1, value1, key2, value2, ...
   * @return the map of each key =&gt; value
   * @throws IllegalArgumentException if an odd number of parameters is passed
   */
  private static Map<String, Object> buildMap(Object... args) {
    Map<String, Object> map = new HashMap<>();

    if (args.length % 2 != 0) {
      throw new IllegalArgumentException(
        "An odd number of parameters were passed to buildMap; even amounts are needed to construct key-value pairs"
      );
    }

    for (int i = 0; i < args.length; i += 2) {
      map.put(args[i].toString(), args[i + 1]);
    }

    return map;
  }
}
