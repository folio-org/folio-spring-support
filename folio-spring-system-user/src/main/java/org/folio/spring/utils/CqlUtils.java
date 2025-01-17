package org.folio.spring.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.text.translate.LookupTranslator;

@UtilityClass
public class CqlUtils {
  private static final LookupTranslator CQL_TRANSLATOR = new LookupTranslator(Map.of(
    "\\", "\\\\",
    "*", "\\*",
    "?", "\\?",
    "^", "\\^",
    "\"", "\\\""
  ));

  /**
   * Encode a CQL string for use in as a constant in a CQL query. This adds quotes around the value and
   * escapes the five special CQL characters {@code \ * ? ^ "}.
   */
  // CQL_TRANSLATOR.translate throws an IOException iff the writer throws an IOException,
  // but we use a plain StringWriter
  @SneakyThrows(IOException.class)
  public static String encodeCql(String s) {
    StringWriter result = new StringWriter();
    result.append('"');
    CQL_TRANSLATOR.translate(s, result);
    result.append('"');

    return result.toString();
  }
}
