package org.folio.spring.i18n.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  // 125 -> sonar insists the below line is code
  // 5852 -> sonar detected the regex as potentially vulnerable to ReDoS (backtracking)
  //         this is not an issue as the regex is only used on known input from the module's classpath
  // accepts form of {something}[{something}/]{moduleName}/{anything}]{optional trailing}
  @SuppressWarnings({"java:S125", "java:S5852"})
  private static final Pattern MODULE_NAME_FROM_DESCRIPTION_PATTERN = Pattern.compile(
    "^.*\\[(?:.*[\\/\\\\])?(?<moduleName>[^\\/\\\\]+)[\\/\\\\].*\\].*$"
  );

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
        String moduleName = getModuleNameFromResourceDescription(
          resource.getDescription()
        );

        Map<String, String> moduleMap = objectMapper.readValue(
          resource.getInputStream(),
          new TypeReference<>() {}
        );

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
          "TranslationFile filename " +
          filename +
          " has a region but no language"
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

  /**
   * Extract the module name from a Spring Resource's description.
   * These descriptions take the form of "file [/foo/bar/translations/mod-modulename/en.json]"
   * or "class path resource [translations/mod-modulename/something.json]".  From this we extract "mod-modulename".
   *
   * @param description the resource description
   * @return the module name
   */
  public static String getModuleNameFromResourceDescription(
    String description
  ) {
    // description is of the form "file [/foo/bar/translations/mod-modulename/en.json]"
    // or of the form "class path resource [translations/mod-modulename/something.json]"
    // extract mod-modulename via regex

    Matcher matcher = MODULE_NAME_FROM_DESCRIPTION_PATTERN.matcher(description);
    if (!matcher.matches()) {
      throw log.throwing(
        new IllegalArgumentException(
          "Could not extract module name from resource description " +
          description
        )
      );
    }
    return matcher.group("moduleName");
  }
}
