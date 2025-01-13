package org.folio.spring.context;

import static java.util.Collections.singleton;

import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExecutionContextBuilder {

  private final FolioEnvironment folioEnvironment;
  private final FolioModuleMetadata moduleMetadata;

  public FolioExecutionContext forSystemUser(SystemUser systemUser, @CheckForNull Supplier<SystemUser> refresher) {
    return new SystemUserExecutionContext(moduleMetadata, systemUser, refresher);
  }

  public FolioExecutionContext buildContext(String tenantId) {
    var okapiUrl = folioEnvironment.getOkapiUrl();
    return new DefaultFolioExecutionContext(
      moduleMetadata,
      Map.of(XOkapiHeaders.URL, singleton(okapiUrl), XOkapiHeaders.TENANT, singleton(tenantId))
    );
  }
}
