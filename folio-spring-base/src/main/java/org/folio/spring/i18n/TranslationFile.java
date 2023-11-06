package org.folio.spring.i18n;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * A class to represent a file that contains translation information.  This file will have a name
 * identifying its associated locale with its contents being an ICU dictionary.
 *
 * <p>This class also contains several utility methods for parsing filenames and aggregating
 * {@code TranslationFile} objects.</p>
 */
@Value
@Log4j2
public class TranslationFile {

  /**
   * The String denoting that a language or country is unknown from {@link getParts}.
   */
  public static final String UNKNOWN_PART = "*";

  /**
   * The location of all translations in the classpath. Allows for format matching UI modules:
   *  /translations/{module-name}/{locale}.json
   */
  public static final String TRANSLATIONS_CLASSPATH = "classpath:%s*/*.json";

  private static final int MAX_FILENAME_PARTS = 2;
  private static final String JSON_FILE_SUFFIX = ".json";

  /**
   * The resources backing the translation files.
   */
  protected List<Resource> resources;

  /**
   * Get the map of patterns from this file.
   *
   * @return the map of patterns -&gt; ICU format strings
   */
  public Map<String, String> getMap() {
    Map<String, String> map = new HashMap<>();

    for (Resource resource : resources) {
      try {
        Map<String, String> moduleMap = new ObjectMapper()
          .readValue(
            resource.getInputStream(),
            new TypeReference<Map<String, String>>() {}
          );

        String moduleName = Path
          .of(resource.getFilename())
          .getParent()
          .getFileName()
          .toString();

        for (Map.Entry<String, String> entry : moduleMap.entrySet()) {
          map.put(moduleName + "." + entry.getKey(), entry.getValue());
        }
      } catch (IOException e) {
        log.error(
          "Could not open/read translation file {}; will use fallback instead",
          resource.getDescription(),
          e
        );
      }
    }

    return map;
  }

  /**
   * Get all available translations in the classpath.
   *
   * @return a list of available translation files
   * @throws IllegalStateException if the translation files cannot be found
   */
  public static List<TranslationFile> getAvailableTranslationFiles(
    ResourcePatternResolver resourceResolver,
    TranslationConfiguration configuration
  ) {
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
        .collect(
          Collectors.groupingBy(resource ->
            Path.of(resource.getFilename()).getFileName().toString()
          )
        );

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
   * Get the parts of this filename as an array of two elements: [language, country].
   *
   * <p>If one (or either) is unknown, {@code UNKNOWN_PART} will be returned.</p>
   *
   * @return [language, country]
   */
  public String[] getParts() {
    return getParts(resources.get(0).getFilename());
  }

  /**
   * Get the parts of a filename as an array of two elements: [language, country].
   *
   * <p>If one (or either) is unknown, {@code UNKNOWN_PART} will be returned.</p>
   *
   * @param filename the filename to parse
   * @return [language, country]
   */
  public static String[] getParts(String filename) {
    String[] result = { UNKNOWN_PART, UNKNOWN_PART };

    if (filename == null) {
      return result;
    }

    if (filename.endsWith(JSON_FILE_SUFFIX)) {
      filename =
        filename.substring(0, filename.length() - JSON_FILE_SUFFIX.length());
    }
    String[] parts = filename.split("_");

    if (parts.length > MAX_FILENAME_PARTS) {
      log.warn(
        String.format(
          "Potentially incorrect parsing of translation filename %s into %s: ",
          filename,
          Arrays.asList(parts)
        )
      );
    }

    if (parts.length == 0) {
      return result;
    }

    if (!parts[0].isEmpty()) {
      result[0] = parts[0].toLowerCase();
    }

    if (parts.length == 1) {
      return result;
    }

    if (!parts[1].isEmpty()) {
      result[1] = parts[1].toLowerCase();
    }

    return result;
  }

  /**
   * Build the map of language -&gt; country -&gt; pattern resource based on the classpath contents.
   *
   * @return the map of languages to countries to pattern resources
   */
  public static Map<String, Map<String, TranslationFile>> buildLanguageCountryPatternMap(
    ResourcePatternResolver resourceResolver,
    TranslationConfiguration configuration
  ) {
    log.info("Building translation file map");

    Map<String, Map<String, TranslationFile>> map = new HashMap<>();

    getAvailableTranslationFiles(resourceResolver, configuration)
      .forEach((TranslationFile file) -> {
        String[] parts = file.getParts();
        map
          .computeIfAbsent(
            parts[0],
            key -> new HashMap<String, TranslationFile>()
          )
          .put(parts[1], file);
        // fill in default, e.g. en-us.json being the only en file will set en-* = en-us
        // if en.json exists later, it will be replaced above
        map.get(parts[0]).putIfAbsent(UNKNOWN_PART, file);
      });

    return map;
  }
}
