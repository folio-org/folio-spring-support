package org.folio.spring.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.sql.ResultSet;
import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
public class TenantService {
  private static final String DESTROY_SQL = "DROP SCHEMA IF EXISTS %1$s CASCADE; DROP ROLE IF EXISTS %1$s";
  private static final String EXIST_SQL = "SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=?)";

  private final JdbcTemplate jdbcTemplate;
  private final FolioExecutionContext context;
  private final FolioSpringLiquibase folioSpringLiquibase;

  public void createTenant(String tenant) throws LiquibaseException {
    if (folioSpringLiquibase != null) {
      var tenantId = context.getTenantId();
      var schemaName = context.getFolioModuleMetadata().getDBSchemaName(tenantId);

      folioSpringLiquibase.setDefaultSchema(schemaName);
      log.info("About to start liquibase update for tenant [{}]", tenant);
      folioSpringLiquibase.performLiquibaseUpdate();
      log.info("Liquibase update for tenant [{}] executed successfully", tenant);
    }
  }

  /**
   * @throws NotFoundException when tenant not found.
   */
  public void deleteTenant(String tenant) {
    if (!tenantExists(tenant)) {
      throw new NotFoundException("Tenant does not exist: " + tenant);
    }

    jdbcTemplate.execute(String.format(DESTROY_SQL, getSchemaName(tenant)));
  }

  public boolean tenantExists(String tenant) {
    return isTrue(jdbcTemplate.query(EXIST_SQL,
      (ResultSet resultSet) -> resultSet.next() && resultSet.getBoolean(1),
      getSchemaName(tenant)));
  }

  private String getSchemaName(String tenantId) {
    return context.getFolioModuleMetadata().getDBSchemaName(tenantId);
  }
}
