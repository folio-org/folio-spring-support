package org.folio.spring.scope;

import lombok.Getter;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;

@Getter
public class EmptyFolioExecutionContextHolder {

  private final EmptyFolioExecutionContext emptyFolioExecutionContext;

  public EmptyFolioExecutionContextHolder(FolioModuleMetadata folioModuleMetadata) {
    this.emptyFolioExecutionContext = new EmptyFolioExecutionContext(folioModuleMetadata);
  }

  private record EmptyFolioExecutionContext(FolioModuleMetadata getFolioModuleMetadata)
    implements FolioExecutionContext { }
}
