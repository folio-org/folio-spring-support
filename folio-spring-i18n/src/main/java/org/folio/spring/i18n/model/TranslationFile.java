package org.folio.spring.i18n.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;

/**
 * A class to represent a file that contains translation information.  This file will have a name
 * identifying its associated locale with its contents being an ICU dictionary.
 *
 * <p>This class also contains several utility methods for parsing filenames and aggregating
 * {@code TranslationFile} objects.</p>
 */
@Log4j2
public record TranslationFile(List<Resource> resources) {
  /**
   * The String denoting that a language or country is unknown from {@link TranslationFile#getLanguageRegion()}.
   */
  public static final String UNKNOWN_PART = "*";

  private static final int MAX_FILENAME_PARTS = 2;
  private static final String JSON_FILE_SUFFIX = ".json";

  /**
   * Get the map of patterns from this file.
   *
   * @return the map of patterns -&gt; ICU format strings
   */
  public Map<String, String> getPatterns() {
    Map<String, String> result = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();

    for (Resource resource : resources) {
      try {
        Map<String, String> moduleMap = objectMapper.readValue(
          resource.getInputStream(),
          new TypeReference<>() {}
        );

        log.info("-------------------------");
        log.info("URL {}", resource.getURL());
        log.info("URL path {}", resource.getURL().getPath());
        log.info("Path.of(URL parent) {}", Path.of(resource.getURL().getPath()));

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
   * Get the parts of this filename as a {@link LanguageRegionPair LanguageRegionPair}.
   *
   * <p>If one (or either) is unknown, {@code UNKNOWN_PART} will be returned.</p>
   *
   * @return a {@link LanguageRegionPair LanguageRegionPair}
   */
  public LanguageRegionPair getLanguageRegion() {
    // Spring's getFilename returns only the last part, so no need to split the path
    return getLanguageRegion(resources.get(0).getFilename());
  }

  /**
   * Get the parts of a filename as a {@link LanguageRegionPair LanguageRegionPair}.
   * These will be converted to lowercase, if applicable.
   *
   * <p>If one (or either) is unknown, {@code UNKNOWN_PART} will be returned.</p>
   *
   * @param filename the filename to parse
   * @return a {@link LanguageRegionPair LanguageRegionPair}
   */
  public static LanguageRegionPair getLanguageRegion(String filename) {
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
      throw log.throwing(
        new IllegalArgumentException(
          String.format(
            "Filename %s was split into more than %d parts",
            filename,
            MAX_FILENAME_PARTS
          )
        )
      );
    }

    if (parts.length == 0) {
      return result;
    }

    if (parts[0].isBlank() && parts.length > 1) {
      throw log.throwing(
        new IllegalArgumentException(
          "TranslationFile filename " + filename + " has a region but no language"
        )
      );
    }

    if (!parts[0].isBlank()) {
      result = result.withLanguage(parts[0].toLowerCase());
    }

    if (parts.length == 1) {
      return result;
    }

    if (!parts[1].isBlank()) {
      result = result.withRegion(parts[1].toLowerCase());
    }

    return result;
  }
}
