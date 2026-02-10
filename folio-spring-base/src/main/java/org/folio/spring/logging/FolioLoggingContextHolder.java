package org.folio.spring.logging;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.apache.logging.log4j.ThreadContext;
import org.folio.spring.FolioExecutionContext;

public final class FolioLoggingContextHolder {

  private static final String TENANT_ID_KEY = "tenantId";
  private static final String REQUEST_ID_KEY = "requestId";
  private static final String MODULE_ID_KEY = "moduleId";
  private static final String USER_ID_KEY = "userId";

  private FolioLoggingContextHolder() {
  }

  public static void putFolioExecutionContext(FolioExecutionContext folioExecutionContext) {
    if (folioExecutionContext == null) {
      cleanupContext();
      return;
    }
    ThreadContext.put(TENANT_ID_KEY, folioExecutionContext.getTenantId());
    ThreadContext.put(REQUEST_ID_KEY, folioExecutionContext.getRequestId());
    var metadata = folioExecutionContext.getFolioModuleMetadata();
    ThreadContext.put(MODULE_ID_KEY, metadata != null ? metadata.getModuleName() : EMPTY);
    ThreadContext.put(USER_ID_KEY,
      folioExecutionContext.getUserId() == null ? EMPTY : folioExecutionContext.getUserId().toString());
  }

  public static void removeFolioExecutionContext(FolioExecutionContext folioExecutionContextToRestore) {
    if (folioExecutionContextToRestore == null) {
      cleanupContext();
    } else {
      putFolioExecutionContext(folioExecutionContextToRestore);
    }
  }

  private static void cleanupContext() {
    ThreadContext.remove(TENANT_ID_KEY);
    ThreadContext.remove(REQUEST_ID_KEY);
    ThreadContext.remove(MODULE_ID_KEY);
    ThreadContext.remove(USER_ID_KEY);
  }
}
