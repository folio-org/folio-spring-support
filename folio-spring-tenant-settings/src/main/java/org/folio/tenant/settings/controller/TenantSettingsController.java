package org.folio.tenant.settings.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tenant.domain.dto.Error;
import org.folio.tenant.domain.dto.Errors;
import org.folio.tenant.domain.dto.Setting;
import org.folio.tenant.domain.dto.SettingCollection;
import org.folio.tenant.domain.dto.SettingGroupCollection;
import org.folio.tenant.domain.dto.SettingUpdateRequest;
import org.folio.tenant.rest.resource.TenantSettingsApi;
import org.folio.tenant.settings.exception.TenantSettingsUnauthorizedOperationException;
import org.folio.tenant.settings.exception.TenantSettingsValidationException;
import org.folio.tenant.settings.service.TenantSettingsPermissionChecker;
import org.folio.tenant.settings.service.TenantSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant settings operations.
 * Provides endpoints for managing configuration groups and settings.
 */
@Log4j2
@RestController
@RequestMapping("/" + "${folio.tenant.settings.domain}")
@RequiredArgsConstructor
public class TenantSettingsController implements TenantSettingsApi {

  private final TenantSettingsService tenantSettingsService;
  private final TenantSettingsPermissionChecker permissionChecker;

  @Override
  public ResponseEntity<SettingGroupCollection> getConfigGroups() {
    var result = tenantSettingsService.getConfigGroups();
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<SettingCollection> getGroupSettings(String groupId) {
    permissionChecker.checkPermission(groupId);
    return tenantSettingsService.getGroupSettings(groupId)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Setting> updateGroupSetting(String groupId, String key,
                                                    SettingUpdateRequest settingUpdateRequest) {
    permissionChecker.checkPermission(groupId, key);
    return tenantSettingsService.updateGroupSetting(groupId, key, settingUpdateRequest)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @ExceptionHandler(TenantSettingsUnauthorizedOperationException.class)
  public ResponseEntity<Errors> handleUnauthorizedOperationException(TenantSettingsUnauthorizedOperationException ex) {
    logExceptionHandling(ex);
    return buildResponseEntity(ex, HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(TenantSettingsValidationException.class)
  public ResponseEntity<Errors> handleUnauthorizedOperationException(TenantSettingsValidationException ex) {
    logExceptionHandling(ex);
    return buildResponseEntity(ex, HttpStatus.UNPROCESSABLE_ENTITY);
  }

  private static ResponseEntity<Errors> buildResponseEntity(Exception ex, HttpStatus status) {
    var error = new Error(ex.getMessage());
    return ResponseEntity.status(status).body(new Errors().errors(List.of(error)));
  }

  private static void logExceptionHandling(Exception exception) {
    log.warn("Handling tenant settings exception", exception);
  }
}
