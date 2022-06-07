package org.folio.spring.scope;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.logging.FolioLoggingContextHolder;

import org.springframework.core.NamedInheritableThreadLocal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FolioExecutionScopeExecutionContextManager is used to store {@link FolioExecutionContext} in thread local.
 *
 * <p>CAUTION:
 *
 * <p>If current thread that uses FolioExecutionContext creates a new thread,
 * the context should be set in the new thread by calling {@code new FolioExecutionContextSetter(...)}
 * from the new thread.
 *
 * <p>When the execution is finished {@link FolioExecutionContextSetter#close()} should be called.
 *
 * <p>Best practice is try-with-resources that automatically calls {@code close()} in all cases, even on exception:
 *
 * <p><pre>
 * try (var x = FolioExecutionContextSetter(currentFolioExecutionContext) {
 *   //some stuff
 * }
 * </pre>
 */
@UtilityClass
@Log4j2
public class FolioExecutionScopeExecutionContextManager {
  private static final String CONVERSATION_ID_KEY = "conversationId";

  private static final String CONVERSATION_ID_GLOBAL = "00000000-0000-0000-0000-000000000000";

  private static final Map<String, Object> fallBackfolioExecutionScope = new ConcurrentHashMap<>();

  private static final InheritableThreadLocal<FolioExecutionContext> folioExecutionContextHolder =
    new NamedInheritableThreadLocal<>("FolioExecutionContext");

  private static final InheritableThreadLocal<Map<String, Object>> folioExecutionScopeHolder =
    new NamedInheritableThreadLocal<>("FolioExecutionScope");

  /**
   * Store folioExecutionContext as {@link ThreadLocal} variable.
   *
   * <p>The visibility of this method is package-private to enforce using {@link FolioExecutionContextSetter}.
   */
  static void beginFolioExecutionContext(FolioExecutionContext folioExecutionContext) {
    var scopeMap = new ConcurrentHashMap<String, Object>();
    scopeMap.put(CONVERSATION_ID_KEY, UUID.randomUUID().toString());
    folioExecutionScopeHolder.set(scopeMap);
    folioExecutionContextHolder.set(folioExecutionContext);
    FolioLoggingContextHolder.putFolioExecutionContext(folioExecutionContext);
  }

  /**
   * Remove FolioExecutionContext from the {@link ThreadLocal} variable.
   *
   * <p>The visibility of this method is package-private to enforce using {@link FolioExecutionContextSetter}.
   */
  static void endFolioExecutionContext() {
    folioExecutionContextHolder.remove();
    folioExecutionScopeHolder.remove();
    FolioLoggingContextHolder.removeFolioExecutionContext();
  }

  /**
   * Retrieve FolioExecutionContext from {@link ThreadLocal} variable.
   */
  static FolioExecutionContext getFolioExecutionContext() {
    return folioExecutionContextHolder.get();
  }

  static String getConversationIdForScope() {
    return (String) getFolioExecutionScope().getOrDefault(CONVERSATION_ID_KEY, CONVERSATION_ID_GLOBAL);
  }

  static Map<String, Object> getFolioExecutionScope() {
    Map<String, Object> folioExecutionScope = folioExecutionScopeHolder.get();
    if (folioExecutionScope == null) {
      log.warn("FolioExecutionScope is not set up. Fallback to default FolioExecutionScope.");
      return fallBackfolioExecutionScope;
    }
    return folioExecutionScope;
  }
}
