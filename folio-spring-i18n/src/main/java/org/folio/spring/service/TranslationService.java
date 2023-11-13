package org.folio.spring.service;

import com.ibm.icu.text.ListFormatter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.config.TranslationConfiguration;
import org.folio.spring.model.TranslationFile;
import org.folio.spring.model.TranslationFile.LanguageRegionPair;
import org.folio.spring.model.TranslationMap;
import org.folio.spring.model.TranslationMatchQuality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * A {@link Service Service} to manage translations as a whole.  In reality, the most important
 * method is simply the format method which will do all of the heavy lifting as needed.  This
 * service will keep track of translation files and locales, caching as possible, in order to
 * serve the user the best possible translation.
 */
@Log4j2
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TranslationService {

  /**
   * The location of all translations in the classpath. Allows for format matching UI modules:
   *  /translations/{module-name}/{locale}.json
   */
  public static final String TRANSLATIONS_CLASSPATH = "classpath:%s*/*.json";

  /**
   * A map from language code -&gt; country -&gt; JSON resource containing {key -&gt; ICU format string}.
   * Will be populated the first time any translation is requested.
   * Note that files are not read here, only enumerated.  Look at the other maps for that information
   *
   * <p>This can be null - if this is the case, the map has not been constructed yet.</p>
   * <p>You should call getFileMap instead of accessing this directly.</p>
   *
   * <p>Language code {@code *} will be used if one is not denoted</p>
   */
  @Nullable
  protected Map<String, Map<String, TranslationFile>> translationFileFromLanguageCountryMap =
    null;

  /**
   * A map from locale -&gt; key -&gt; ICU format string, filled in on-demand
   * as locales are presented.
   */
  protected Map<Locale, TranslationMap> localeTranslations = new HashMap<>();

  private final ResourcePatternResolver resourceResolver;
  private final TranslationConfiguration configuration;

  /**
   * Get a map from language -&gt; country -&gt; pattern resource based on the classpath contents.
   *
   * @return the map of languages to countries to pattern resources
   */
  @Nonnull
  public Map<String, Map<String, TranslationFile>> getFileMap() {
    if (this.translationFileFromLanguageCountryMap == null) {
      this.translationFileFromLanguageCountryMap =
        buildLanguageCountryPatternMap();
    }
    return this.translationFileFromLanguageCountryMap;
  }

  /**
   * Get the best {@link TranslationMap TranslationMap} associated with the current locale.
   *
   * @param locale the locale to find a TranslationMap for
   * @param base the default translation that should be used as a fallback; can be null when
   *   creating the default translation
   * @return the best translation available for the current locale, potentially null if not
   *   default is null.  It checks for match of language and country, then language, then returns
   *   default if neither are available
   */
  @Nullable
  public TranslationMap getTranslation(
    Locale locale,
    @Nullable TranslationMap base
  ) {
    return localeTranslations.computeIfAbsent(
      locale,
      (Locale l) -> {
        log.info("Cache miss on " + locale);

        if (this.getFileMap().containsKey(locale.getLanguage().toLowerCase())) {
          Map<String, TranslationFile> languageMap =
            this.getFileMap().get(locale.getLanguage().toLowerCase());

          TranslationFile baseFile = languageMap.get(
            TranslationFile.UNKNOWN_PART
          );
          TranslationMap baseMap = new TranslationMap(locale, baseFile, base);

          if (languageMap.containsKey(locale.getCountry().toLowerCase())) {
            return new TranslationMap(
              locale,
              languageMap.get(locale.getCountry().toLowerCase()),
              baseMap
            );
          }

          return baseMap;
        }

        return base;
      }
    );
  }

  /**
   * Get the best {@link TranslationMap TranslationMap} associated with the current locale.
   *
   * @param locale the locale to find a TranslationMap for
   * @return the best translation available for the current locale, including the server's default
   */
  public TranslationMap getTranslation(Locale locale) {
    return getTranslation(locale, getDefaultTranslation().withLocale(locale));
  }

  /**
   * Find a TranslationMap for the default locale -- used to initialize for {@code getDefaultTranslations}.
   *
   * @return the best applicable translation
   * @throws IllegalStateException if no translation can be found
   */
  protected TranslationMap resolveDefaultLocale() {
    TranslationMap foundDefault = getTranslation(
      configuration.getDefaultLocale(),
      null
    );
    if (foundDefault == null) {
      foundDefault = getTranslation(Locale.ENGLISH, null);
    }
    if (foundDefault == null) {
      throw new IllegalStateException(
        String.format(
          "No translations are sufficient for the server's default locale (%s) nor %s",
          configuration.getDefaultLocale(),
          Locale.ENGLISH
        )
      );
    }
    return foundDefault;
  }

  /**
   * Get the translation map for the default locale (or en-us, if that is not possible).
   *
   * @return the default locale's translation map
   */
  public TranslationMap getDefaultTranslation() {
    // computeIfAbsent does not work due to the resolver potentially filling multiple keys
    if (
      !this.localeTranslations.containsKey(configuration.getDefaultLocale())
    ) {
      this.localeTranslations.put(
          configuration.getDefaultLocale(),
          this.resolveDefaultLocale()
        );
    }
    return this.localeTranslations.get(configuration.getDefaultLocale());
  }

  /**
   * Get all of the locales for the current request.  This will respect the Accept-Language header
   * and quality values, as applicable, with highest quality first.
   *
   * @return a List of all Locale objects.  Invalid and wildcard Locales will likely return a Locale
   *   with empty string/fields or, in some cases, the server's current Locale.
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language">MDN docs</a>
   */
  @SuppressWarnings("java:S1166")
  public List<Locale> getCurrentLocales() {
    try {
      return Collections.list(
        (
          (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()
        ).getRequest()
          .getLocales()
      );
    } catch (IllegalStateException e) {
      return new ArrayList<>();
    }
  }

  /**
   * Get the best single locale.  Either the first from the request or the server's default.
   *
   * @return a single Locale object.  Invalid and wildcard Locales sent to the server will likely
   *   return a Locale with empty string/fields or, in some cases, the server's current Locale.
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language">MDN docs</a>
   */
  @SuppressWarnings("java:S1166")
  public Locale getCurrentLocale() {
    try {
      return getCurrentLocales().get(0);
    } catch (RuntimeException e) {
      return configuration.getDefaultLocale();
    }
  }

  /**
   * Get the best {@link TranslationMap TranslationMap} for the current language(s).
   * This will respect the Accept-Language header and quality values, as applicable.
   *
   * @return the best TranslationMap
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language"> MDN docs</a>
   */
  public TranslationMap getCurrentTranslation() {
    return this.getBestTranslation(this.getCurrentLocales());
  }

  /**
   * Get the best {@link TranslationMap TranslationMap} for the provides list of Locales.
   * This will return the first one that matches on at least language or, if none match,
   * the default TranslationMap.
   *
   * @param locales the ordered list of locales to consider
   * @return the best TranslationMap
   */
  public TranslationMap getBestTranslation(Iterable<Locale> locales) {
    // return the first one that is a good match
    for (Locale locale : locales) {
      TranslationMap correspondingMap = this.getTranslation(locale);
      if (correspondingMap.getQuality() != TranslationMatchQuality.NO_MATCH) {
        return correspondingMap;
      }
    }

    return this.getDefaultTranslation();
  }

  /**
   * Wraps the {@link TranslationMap#format TranslationMap#format} method on the current translation.
   * Equivalent to {@code getCurrentTranslation().format(...)}.
   *
   * <p>Format an ICU format string (found by its key), supplying a series of named arguments as key
   * value pairs.  For example: {@code format("Hello {name}", "name", parameterValue)}</p>
   *
   * @param key the key of the format string.  You likely want to use a constant from
   *   {@link TranslationKey TranslationKey} rather than hard-coding a string.
   * @param args pairs of keys and values to interpolate
   * @return the formatted string
   */
  public String format(String key, Object... args) {
    return this.getCurrentTranslation().format(key, args);
  }

  /**
   * Format a list of strings into one cohesive string.  For example, in English, {@code [A B C D]}
   * would become {@code "A, B, C, and D"}.
   *
   * @param list a list of items to join
   * @return the joined string
   */
  public String formatList(Collection<?> list) {
    return ListFormatter.getInstance(getCurrentLocale()).format(list);
  }

  /**
   * Get all available translations in the classpath.
   *
   * @return a list of available translation files
   * @throws IllegalStateException if the translation files cannot be found
   */
  public List<TranslationFile> getAvailableTranslationFiles() {
    try {
      Map<String, List<Resource>> localeGroups = Arrays
        .asList(
          resourceResolver.getResources(
            String.format(
              TRANSLATIONS_CLASSPATH,
              configuration.getTranslationDirectory()
            )
          )
        )
        .stream()
        .filter(Resource::isReadable)
        .collect(Collectors.groupingBy(Resource::getFilename));

      List<TranslationFile> files = localeGroups
        .entrySet()
        .stream()
        .map(entry -> new TranslationFile(entry.getValue()))
        .toList();

      log.info("Got translation files: " + files);

      return files;
    } catch (IOException e) {
      log.error("Could not retrieve translation files:", e);
      throw new IllegalStateException(
        "Could not retrieve translation files",
        e
      );
    }
  }

  /**
   * Build the map of language -&gt; country -&gt; pattern resource based on the classpath contents.
   *
   * @return the map of languages to countries to pattern resources
   */
  public Map<String, Map<String, TranslationFile>> buildLanguageCountryPatternMap() {
    log.info("Building translation file map");

    Map<String, Map<String, TranslationFile>> map = new HashMap<>();

    getAvailableTranslationFiles()
      .forEach((TranslationFile file) -> {
        LanguageRegionPair parts = file.getParts();
        map
          .computeIfAbsent(
            parts.language(),
            key -> new HashMap<String, TranslationFile>()
          )
          .put(parts.region(), file);

        // fill in default, e.g. en-us.json being the only en file will set en-* = en-us
        // if en.json exists later, it will be replaced above
        map
          .get(parts.language())
          .putIfAbsent(TranslationFile.UNKNOWN_PART, file);
      });

    return map;
  }
}
