package org.folio.spring.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;

@Log4j2
@UtilityClass
public class LoggingUtils {

  public static void logSensitive(Level level, String message) {
    log.log(level, "SENSITIVE! " + message);
  }
}
