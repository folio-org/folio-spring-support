package org.folio.spring.context;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.springframework.stereotype.Component;

@Deprecated(since = "10.0.0", forRemoval = true)
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
    return buildContext(tenantId, emptyMap());
  }

  public FolioExecutionContext buildContext(String tenantId, Map<String, Collection<String>> headers) {
    return buildContext(tenantId, null, headers);
  }

  /**
   * Uses userId from provided headers if present, otherwise uses the provided userId.
   * */
  public FolioExecutionContext buildContext(String tenantId, UUID userId, Map<String, Collection<String>> headers) {
    var okapiUrl = folioEnvironment.getOkapiUrl();
    var contextHeaders = new HashMap<>(Maps.filterValues(MapUtils.emptyIfNull(headers), CollectionUtils::isNotEmpty));
    contextHeaders.put(XOkapiHeaders.URL, singleton(okapiUrl));
    contextHeaders.put(XOkapiHeaders.TENANT, singleton(tenantId));
    if (userId != null) {
      contextHeaders.computeIfAbsent(XOkapiHeaders.USER_ID, k -> singleton(userId.toString()));
    }
    return new DefaultFolioExecutionContext(
      moduleMetadata,
      contextHeaders
    );
  }
}
