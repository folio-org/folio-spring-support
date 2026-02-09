package org.folio.spring.logging;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

@Plugin(name = "folio", category = StrLookup.CATEGORY)
public class FolioLoggingContextLookup implements StrLookup {

  /**
   * Lookup value by key.
   *
   * @param key the name of logging variable, {@code null} key isn't allowed
   * @return value for key or *empty string* if there is no such key
   */
  @Override
  public String lookup(String key) {
    return lookup(null, key);
  }

  /**
   * Lookup value by key from ThreadContext.
   *
   * @param event LogEvent (not used, context is read from ThreadContext)
   * @param key the name of logging variable, {@code null} key isn't allowed
   * @return value for key or *empty string* if there is no such key
   */
  @Override
  public String lookup(LogEvent event, String key) {
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    String value = ThreadContext.get(key);
    return value != null ? value : EMPTY;
  }
}
