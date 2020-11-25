package org.folio.spring.scope;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;

import java.util.Collection;
import java.util.Map;

public class EmptyFolioExecutionContextHolder {
  private final EmptyFolioExecutionContext emptyFolioExecutionContext;

  public EmptyFolioExecutionContextHolder(FolioModuleMetadata folioModuleMetadata) {
    this.emptyFolioExecutionContext = new EmptyFolioExecutionContext(folioModuleMetadata);
  }

  public EmptyFolioExecutionContext getEmptyFolioExecutionContext() {
    return emptyFolioExecutionContext;
  }

  private static class EmptyFolioExecutionContext implements FolioExecutionContext {
    private final FolioModuleMetadata folioModuleMetadata;

    private EmptyFolioExecutionContext(FolioModuleMetadata folioModuleMetadata) {
      this.folioModuleMetadata = folioModuleMetadata;
    }

    @Override
    public String getTenantId() {
      return null;
    }

    @Override
    public String getOkapiUrl() {
      return null;
    }

    @Override
    public String getToken() {
      return null;
    }

    @Override
    public String getUserName() {
      return null;
    }

    @Override
    public Map<String, Collection<String>> getAllHeaders() {
      return null;
    }

    @Override
    public Map<String, Collection<String>> getOkapiHeaders() {
      return null;
    }

    @Override
    public FolioModuleMetadata getFolioModuleMetadata() {
      return folioModuleMetadata;
    }
  }
}
