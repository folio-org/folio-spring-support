package org.folio.spring.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import lombok.With;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;

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
    Map<String, String> result = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();

    for (Resource resource : resources) {
      try {
        Map<String, String> moduleMap = objectMapper.readValue(
          resource.getInputStream(),
          new TypeReference<Map<String, String>>() {}
        );

        String moduleName = Path
          .of(resource.getURL().getPath())
          .getParent()
          .getFileName()
          .toString();

        moduleMap.forEach((key, value) ->
          result.put(moduleName + "." + key, value)
        );
      } catch (IOException e) {
        log.error(
          "Could not open/read translation file {}; will use fallback instead",
          resource.getDescription(),
          e
        );
      }
    }

    return result;
  }

  /**
   * Get the parts of this filename as an array of two elements: [language, country].
   *
   * <p>If one (or either) is unknown, {@code UNKNOWN_PART} will be returned.</p>
   *
   * @return LanguageRegionPair
   */
  public LanguageRegionPair getParts() {
    // Spring's getFilename returns only the last part, so no need to split the path
    return getParts(resources.get(0).getFilename());
  }

  /**
   * Get the parts of a filename as an array of two elements: [language, country].
   * These will be converted to lowercase, if applicable.
   *
   * <p>If one (or either) is unknown, {@code UNKNOWN_PART} will be returned.</p>
   *
   * @param filename the filename to parse
   * @return LanguageRegionPair
   */
  public static LanguageRegionPair getParts(String filename) {
    LanguageRegionPair result = new LanguageRegionPair(
      UNKNOWN_PART,
      UNKNOWN_PART
    );

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
      result = result.withLanguage(parts[0].toLowerCase());
    }

    if (parts.length == 1) {
      return result;
    }

    if (!parts[1].isEmpty()) {
      result = result.withRegion(parts[1].toLowerCase());
    }

    return result;
  }

  @With
  public record LanguageRegionPair(String language, String region) {}
}
