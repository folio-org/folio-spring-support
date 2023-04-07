package org.folio.spring.scope;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;


public class EmptyFolioExecutionContextHolder {

  private final EmptyFolioExecutionContext emptyFolioExecutionContext;

  public EmptyFolioExecutionContextHolder(FolioModuleMetadata folioModuleMetadata) {
    this.emptyFolioExecutionContext = new EmptyFolioExecutionContext(folioModuleMetadata);
  }

  public EmptyFolioExecutionContext getEmptyFolioExecutionContext() {
    return emptyFolioExecutionContext;
  }

  private static final class EmptyFolioExecutionContext implements FolioExecutionContext {

    private final FolioModuleMetadata folioModuleMetadata;

    private EmptyFolioExecutionContext(FolioModuleMetadata folioModuleMetadata) {
      this.folioModuleMetadata = folioModuleMetadata;
    }

    @Override
    public FolioModuleMetadata getFolioModuleMetadata() {
      return folioModuleMetadata;
    }
  }
}
