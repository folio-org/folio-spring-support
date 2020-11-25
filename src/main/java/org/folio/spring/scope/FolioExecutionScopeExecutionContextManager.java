package org.folio.spring.scope;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.springframework.core.NamedInheritableThreadLocal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

  public static void beginFolioExecutionContext(FolioExecutionContext folioExecutionContext) {
    var scopeMap = new ConcurrentHashMap<String, Object>();
    scopeMap.put(CONVERSATION_ID_KEY, UUID.randomUUID().toString());
    folioExecutionScopeHolder.set(scopeMap);
    folioExecutionContextHolder.set(folioExecutionContext);
  }

  public static void endFolioExecutionContext() {
    folioExecutionContextHolder.remove();
    folioExecutionScopeHolder.remove();
  }

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
