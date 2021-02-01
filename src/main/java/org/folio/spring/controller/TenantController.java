package org.folio.spring.controller;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import java.sql.ResultSet;
import javax.validation.Valid;
import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController("defaultTenantController")
@RequestMapping(value = "/_/")
@ConditionalOnMissingBean(name = "folioTenantController")
@RequiredArgsConstructor
public class TenantController implements TenantApi {
  private static final String DESTROY_SQL = "DROP SCHEMA IF EXISTS %1$s CASCADE; DROP ROLE IF EXISTS %1$s";
  private static final String EXIST_SQL = "SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=?)";

  private final FolioSpringLiquibase folioSpringLiquibase;
  private final FolioExecutionContext context;
  private final JdbcTemplate jdbcTemplate;

  @Override
  public ResponseEntity<String> postTenant(String tenant, @Valid TenantAttributes tenantAttributes) {
    log.info("Initializing [{}] tenant...", tenant);

    if (folioSpringLiquibase != null) {
      var tenantId = context.getTenantId();
      var schemaName = context.getFolioModuleMetadata().getDBSchemaName(tenantId);

      folioSpringLiquibase.setDefaultSchema(schemaName);
      try {
        log.info("About to start liquibase update for tenant [{}]", tenant);
        folioSpringLiquibase.performLiquibaseUpdate();
        log.info("Liquibase update for tenant [{}] executed successfully", tenant);
      } catch (LiquibaseException e) {
        log.error("Liquibase error", e);
        return status(HttpStatus.INTERNAL_SERVER_ERROR).body("Liquibase error: " + e.getMessage());
      }
    }
    return ok().body("true");
  }

  @Override
  public ResponseEntity<Void> deleteTenant(String tenant) {
    log.info("Destroying [{}] tenant...", tenant);

    if (!tenantExists(tenant)) {
      log.warn("Tenant [{}] not found", tenant);
      return notFound().build();
    }

    jdbcTemplate.execute(String.format(DESTROY_SQL, getSchemaName(tenant)));

    log.info("Tenant [{}] destroyed successfully", tenant);
    return noContent().build();
  }

  @Override
  public ResponseEntity<String> getTenant(String tenant) {
    log.info("Executing get [{}] tenant...", tenant);

    return ok(String.valueOf(tenantExists(tenant)));
  }

  private boolean tenantExists(String tenant) {
    return isTrue(jdbcTemplate.query(EXIST_SQL,
      (ResultSet resultSet) -> resultSet.next() && resultSet.getBoolean(1),
      getSchemaName(tenant)));
  }

  private String getSchemaName(String tenantId) {
    return context.getFolioModuleMetadata().getDBSchemaName(tenantId);
  }
}
