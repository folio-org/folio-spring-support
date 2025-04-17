package org.folio.spring.context;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
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

  /**
   * Creates an execution context for sending requests to FOLIO on behalf of a system user.
   *
   * @param systemUser the user to send requests as
   * @param refresher a supplier which should, upon the {@code systemUser}'s expiration, return a new
   *   {@link SystemUser} with a fresh access token
   */
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

  public FolioExecutionContext buildContext(String tenantId, Map<String, Collection<String>> headers) {
    var okapiUrl = folioEnvironment.getOkapiUrl();
    var contextHeaders = headers == null
      ? new HashMap<String, Collection<String>>()
      : headers.entrySet().stream()
      .filter(entry -> CollectionUtils.isNotEmpty(entry.getValue()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    contextHeaders.put(XOkapiHeaders.URL, singleton(okapiUrl));
    contextHeaders.put(XOkapiHeaders.TENANT, singleton(tenantId));
    return new DefaultFolioExecutionContext(
      moduleMetadata,
      contextHeaders
    );
  }
}
