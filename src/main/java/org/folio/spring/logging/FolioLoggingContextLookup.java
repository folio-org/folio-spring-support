package org.folio.spring.logging;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

import org.folio.spring.FolioExecutionContext;

@Plugin(name = "folio", category = StrLookup.CATEGORY)
public class FolioLoggingContextLookup implements StrLookup {

  private static final String TENANT_ID_LOGGING_VAR_NAME = "tenantid";
  private static final String REQUEST_ID_LOGGING_VAR_NAME = "requestid";
  private static final String MODULE_ID_LOGGING_VAR_NAME = "moduleid";
  private static final String USER_ID_LOGGING_VAR_NAME = "userid";

  private static final Map<String, Function<FolioExecutionContext, String>> map = new HashMap<>();

  static {
    map.put(TENANT_ID_LOGGING_VAR_NAME, FolioExecutionContext::getTenantId);
    map.put(USER_ID_LOGGING_VAR_NAME, context -> context.getUserId() == null ? EMPTY : context.getUserId().toString());
    map.put(MODULE_ID_LOGGING_VAR_NAME, context -> context.getFolioModuleMetadata().getModuleName());
    map.put(REQUEST_ID_LOGGING_VAR_NAME, FolioExecutionContext::getRequestId);
  }

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
   * Lookup value by key. LogEvent isn't used.
   *
   * @param key the name of logging variable, {@code null} key isn't allowed
   * @return value for key or *empty string* if there is no such key
   */
  @Override
  public String lookup(LogEvent event, String key) {
    var folioExecutionContext = FolioLoggingContextHolder.getFolioExecutionContext();
    if (folioExecutionContext.isEmpty()) {
      return EMPTY;
    }
    if (key == null) {
      throw new IllegalArgumentException("Key cannot be null");
    }
    return map.getOrDefault(key, c -> EMPTY).apply(folioExecutionContext.get());
  }

}
