package org.folio.spring.controller;

import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import javax.validation.Valid;
import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController("defaultTenantController")
@RequestMapping(value = "/_/")
@ConditionalOnMissingBean(name = "folioTenantController")
@RequiredArgsConstructor
public class TenantController implements TenantApi {
  private final TenantService tenantService;

  @Override
  public ResponseEntity<String> postTenant(String tenant, @Valid TenantAttributes tenantAttributes) {
    log.info("Initializing [{}] tenant...", tenant);
    try {
      tenantService.createTenant(tenant);
    } catch (LiquibaseException e) {
      log.error("Liquibase error", e);
      return status(HttpStatus.INTERNAL_SERVER_ERROR).body("Liquibase error: " + e.getMessage());
    }

    return ok().body("true");
  }

  @Override
  public ResponseEntity<Void> deleteTenant(String tenant) {
    log.info("Destroying [{}] tenant...", tenant);

    tenantService.deleteTenant(tenant);

    log.info("Tenant [{}] destroyed successfully", tenant);
    return noContent().build();
  }

  @Override
  public ResponseEntity<String> getTenant(String tenant) {
    log.info("Executing get [{}] tenant...", tenant);

    return ok(String.valueOf(tenantService.tenantExists(tenant)));
  }
}
