package org.folio.tenant.settings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.settings.config.TenantSettingsProperties;
import org.folio.tenant.settings.exception.TenantSettingsUnauthorizedOperationException;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * Service for checking user permissions for tenant settings operations.
 * Validates permissions from request headers against required permissions.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class TenantSettingsPermissionChecker {

  private static final TypeReference<List<String>> STRING_LIST_TYPE_REF = new TypeReference<>() { };
  private static final String SETTING_COLLECTION_PERMISSION = "%s.config.groups.settings.%s.collection.get";
  private static final String SETTING_ITEM_PATCH_PERMISSION = "%s.config.groups.settings.%s.%s.item.patch";

  private final TenantSettingsProperties properties;
  private final FolioExecutionContext context;
  private final ObjectMapper objectMapper;

  /**
   * Checks if the user has permission to access settings for a group.
   *
   * @param group the group identifier
   * @throws TenantSettingsUnauthorizedOperationException if permission check fails
   */
  public void checkPermission(@NonNull String group) {
    var requiredPermission = SETTING_COLLECTION_PERMISSION.formatted(properties.getDomain(), group);
    checkRequiredPermission(requiredPermission);
  }

  /**
   * Checks if the user has permission to update a specific setting.
   *
   * @param group the group identifier
   * @param key the setting key
   * @throws TenantSettingsUnauthorizedOperationException if permission check fails
   */
  public void checkPermission(@NonNull String group, @NonNull String key) {
    var requiredPermission = SETTING_ITEM_PATCH_PERMISSION.formatted(properties.getDomain(), group, key);
    checkRequiredPermission(requiredPermission);
  }

  private void checkRequiredPermission(String requiredPermission) {
    if (properties.isPermissionCheckEnabled()) {
      var permissions = getPermissions();
      if (!permissions.contains(requiredPermission)) {
        throw new TenantSettingsUnauthorizedOperationException(requiredPermission);
      }
    }
  }

  /**
   * Extracts authenticated permissions from request headers.
   */
  private List<String> getPermissions() {
    var okapiHeaders = context.getOkapiHeaders();
    var permissionsHeader = okapiHeaders.get(XOkapiHeaders.PERMISSIONS);
    try {
      if (permissionsHeader != null) {
        var iterator = permissionsHeader.iterator();
        if (iterator.hasNext()) {
          return objectMapper.readValue(iterator.next(), STRING_LIST_TYPE_REF);
        }
      }
      return Collections.emptyList();
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse permissions header: {}", permissionsHeader, e);
      return Collections.emptyList();
    }
  }
}
