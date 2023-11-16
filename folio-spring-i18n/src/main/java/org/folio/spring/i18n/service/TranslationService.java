package org.folio.spring.i18n.service;

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
import org.folio.spring.i18n.config.TranslationConfiguration;
import org.folio.spring.i18n.model.TranslationFile;
import org.folio.spring.i18n.model.TranslationFile.LanguageRegionPair;
import org.folio.spring.i18n.model.TranslationMap;
import org.folio.spring.i18n.model.TranslationMatchQuality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
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
@ComponentScan
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class TranslationService {

  /**
   * The location of all translations in the classpath. Allows for format matching UI modules:
   *  /translations/{module-name}/{locale}.json
   */
  private static final String TRANSLATIONS_CLASSPATH = "classpath:%s*/*.json";

  /**
   * A map from language code to a map from country to the JSON resource containing {key to ICU format string mappings}.
   * Will be populated the first time any translation is requested.
   * Note that files are not read here, only enumerated.  Look at the other maps for that information
   *
   * <p>This can be null - if this is the case, the map has not been constructed yet.</p>
   * <p>You should call getFileMap instead of accessing this directly.</p>
   *
   * <p>
   *  Language code ({@link TranslationFile#UNKNOWN_PART TranslationFile#UNKNOWN_PART}) will be used if unknown
   * </p>
   */
  @Nullable
  protected Map<String, Map<String, TranslationFile>> translationFileFromLanguageCountryMap =
    null;

  /**
   * A map from locales to translations, filled in on-demand as locales are presented.
   */
  protected Map<Locale, TranslationMap> localeTranslations = new HashMap<>();

  private final ResourcePatternResolver resourceResolver;
  private final TranslationConfiguration configuration;

  /**
   * Get a map from language -&gt; country -&gt; {@link TranslationFile TranslationFile}.
   * The files are pulled from the filesystem based on the provided
   * {@link TranslationConfiguration TranslationConfiguration}.
   *
   * @return the map of languages to countries to {@link TranslationFile TranslationFile}s
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
   * @param fallback the default translation that should be used as a fallback; can be null when
   *   creating the primary fallback translation
   * @return the best translation available for the current locale, potentially null if the
   *   default is null.  It checks for match of language and country, then language, then returns
   *   default if neither are available
   */
  @Nullable
  public TranslationMap getTranslation(
    Locale locale,
    @Nullable TranslationMap fallback
  ) {
    return localeTranslations.computeIfAbsent(
      locale,
      (Locale missingLocale) -> {
        log.info("Cache miss on {}; loading map", missingLocale);

        if (
          this.getFileMap()
            .containsKey(missingLocale.getLanguage().toLowerCase())
        ) {
          Map<String, TranslationFile> languageMap =
            this.getFileMap().get(missingLocale.getLanguage().toLowerCase());

          TranslationFile baseFile = languageMap.get(
            TranslationFile.UNKNOWN_PART
          );
          TranslationMap baseMap = new TranslationMap(
            missingLocale,
            baseFile,
            fallback
          );

          if (
            languageMap.containsKey(missingLocale.getCountry().toLowerCase())
          ) {
            return new TranslationMap(
              missingLocale,
              languageMap.get(missingLocale.getCountry().toLowerCase()),
              baseMap
            );
          }

          return baseMap;
        }

        return fallback;
      }
    );
  }

  /**
   * Get the best {@link TranslationMap TranslationMap} associated with the given locale.
   *
   * @param locale the locale to find a TranslationMap for
   * @return the best translation available for the current locale(s), including the fallback locale
   *   from {@link TranslationConfiguration TranslationConfiguration}, if necessary
   */
  public TranslationMap getTranslation(Locale locale) {
    return getTranslation(locale, getFallbackTranslation().withLocale(locale));
  }

  /**
   * Find a TranslationMap for the fallback locale -- used to initialize for {@code getDefaultTranslation}.
   *
   * @return the best applicable translation
   * @throws IllegalStateException if no translation can be found
   */
  protected TranslationMap resolveFallbackTranslation() {
    TranslationMap foundDefault = getTranslation(
      configuration.getFallbackLocale(),
      null
    );
    if (foundDefault == null) {
      throw new IllegalStateException(
        String.format(
          "No translations are sufficient for the default locale (%s)",
          configuration.getFallbackLocale()
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
  public TranslationMap getFallbackTranslation() {
    // computeIfAbsent does not work due to the resolver potentially filling multiple keys
    if (
      !this.localeTranslations.containsKey(configuration.getFallbackLocale())
    ) {
      this.localeTranslations.put(
        configuration.getFallbackLocale(),
        this.resolveFallbackTranslation()
      );
    }
    return this.localeTranslations.get(configuration.getFallbackLocale());
  }

  /**
   * Get all of the locales for the current request.  This will respect the Accept-Language header
   * and quality values, as applicable, with highest quality first.
   *
   * @return a List of all Locale objects.  Invalid and wildcard Locales will likely return a Locale
   *   with empty string/fields or, in some cases, the fallback Locale
   *   (from {@link TranslationConfiguration TranslationConfiguration}).
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language">MDN docs</a>
   */
  public List<Locale> getCurrentLocales() {
    try {
      return Collections.list(
        (
          (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()
        ).getRequest()
          .getLocales()
      );
    } catch (IllegalStateException e) {
      log.warn(
        "The current request contains no locale information: {}",
        e.getMessage()
      );
      return new ArrayList<>();
    }
  }

  /**
   * Get the best single locale.  Either the first from the request or the default.
   *
   * @return a single Locale object.  Invalid. missing, and wildcard Locales sent to the server will likely
   *   return a Locale with empty string/fields or, in some cases, the fallback Locale
   *   (from {@link TranslationConfiguration TranslationConfiguration})
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Language">MDN docs</a>
   */
  public Locale getCurrentLocale() {
    return getCurrentLocales()
      .stream()
      .findFirst()
      .orElse(configuration.getFallbackLocale());
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
   * the fallback/default TranslationMap.
   *
   * @param locales the ordered list of locales to consider
   * @return the best TranslationMap
   */
  public TranslationMap getBestTranslation(Collection<Locale> locales) {
    // return the first one that is a good match
    return locales
      .stream()
      .map(this::getTranslation)
      .filter(m -> m.getQuality() != TranslationMatchQuality.NO_MATCH)
      .findFirst()
      .orElseGet(this::getFallbackTranslation);
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
        .stream(
          resourceResolver.getResources(
            String.format(
              TRANSLATIONS_CLASSPATH,
              configuration.getTranslationDirectory()
            )
          )
        )
        .filter(Resource::isReadable)
        .collect(Collectors.groupingBy(Resource::getFilename));

      List<TranslationFile> files = localeGroups
        .values()
        .stream()
        .map(TranslationFile::new)
        .toList();

      log.info("Got translation files: " + files);

      return files;
    } catch (IOException e) {
      throw log.throwing(
        new IllegalStateException("Could not retrieve translation files", e)
      );
    }
  }

  /**
   * Build the map of language -&gt; country -&gt; {@link TranslationFile TranslationFile}.
   * The files are pulled from the filesystem based on the provided
   * {@link TranslationConfiguration TranslationConfiguration}.
   *
   * @return the map of languages to countries to {@link TranslationFile TranslationFile}s
   */
  protected Map<String, Map<String, TranslationFile>> buildLanguageCountryPatternMap() {
    log.info("Building translation file map");

    Map<String, Map<String, TranslationFile>> languageCountryPatternMap = new HashMap<>();

    getAvailableTranslationFiles()
      .forEach((TranslationFile file) -> {
        LanguageRegionPair parts = file.getParts();
        Map<String, TranslationFile> regionMap = languageCountryPatternMap.computeIfAbsent(
          parts.language(),
          key -> new HashMap<String, TranslationFile>()
        );
        regionMap.put(parts.region(), file);

        // fill in default, e.g. en-us.json being the only en file will set en-* = en-us
        // if en.json exists later, it will be replaced above
        regionMap.putIfAbsent(TranslationFile.UNKNOWN_PART, file);
      });

    return languageCountryPatternMap;
  }
}
