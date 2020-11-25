package org.folio.spring.controller;

import liquibase.exception.LiquibaseException;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Log4j2
@RestController("defaultTenantController")
@RequestMapping(value = "/_/")
@ConditionalOnMissingBean(name = "folioTenantController")
public class TenantController implements TenantApi {

  private final FolioSpringLiquibase folioSpringLiquibase;

  private final FolioExecutionContext context;

  @Autowired
  public TenantController(FolioSpringLiquibase folioSpringLiquibase,
                          FolioExecutionContext context) {
    this.folioSpringLiquibase = folioSpringLiquibase;
    this.context = context;
  }

  @Override
  public ResponseEntity<String> postTenant(@Valid TenantAttributes tenantAttributes) {
    if (folioSpringLiquibase != null) {
      var tenantId = context.getTenantId();

      var schemaName = context.getFolioModuleMetadata().getDBSchemaName(tenantId);

      folioSpringLiquibase.setDefaultSchema(schemaName);
      try {
        folioSpringLiquibase.performLiquibaseUpdate();
      } catch (LiquibaseException e) {
        e.printStackTrace();
        log.error("Liquibase error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Liquibase error: " + e.getMessage());
      }
    }
    return ResponseEntity.ok().body("true");
  }
}
