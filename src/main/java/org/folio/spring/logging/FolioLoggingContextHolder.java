package org.folio.spring.logging;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;

import org.folio.spring.FolioExecutionContext;

public final class FolioLoggingContextHolder {

  private FolioLoggingContextHolder() {
  }

  private static final String FOLIO_EXECUTION_CONTEXT_KEY = "FolioExecutionContext";

  public static void putFolioExecutionContext(FolioExecutionContext folioExecutionContext) {
    LogManager.getContext().putObject(FOLIO_EXECUTION_CONTEXT_KEY, folioExecutionContext);
  }

  public static void removeFolioExecutionContext() {
    LogManager.getContext().removeObject(FOLIO_EXECUTION_CONTEXT_KEY);
  }

  public static Optional<FolioExecutionContext> getFolioExecutionContext() {
    return Optional.ofNullable(((FolioExecutionContext) LogManager.getContext().getObject(FOLIO_EXECUTION_CONTEXT_KEY)));
  }
}
