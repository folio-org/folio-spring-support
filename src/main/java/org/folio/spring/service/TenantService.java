package org.folio.spring.service;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.sql.ResultSet;
import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.exception.TenantUpgradeException;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
@Lazy
public class TenantService {

  protected static final String DESTROY_SQL =
    "DROP SCHEMA IF EXISTS %1$s CASCADE; DROP ROLE IF EXISTS %1$s";
  protected static final String EXIST_SQL =
    "SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=?)";

  protected final JdbcTemplate jdbcTemplate;
  protected final FolioExecutionContext context;
  protected final FolioSpringLiquibase folioSpringLiquibase;

  /*
   * Because of the liquibase.Scope implementation for the SpringLiquibase it is not possible to run several SpringLiquibase executions simultaneously.
   * That is why this method must be synchronized.
   */
  public synchronized void createOrUpdateTenant(TenantAttributes tenantAttributes) {
    beforeTenantUpdate(tenantAttributes);

    if (folioSpringLiquibase != null) {
      beforeLiquibaseUpdate(tenantAttributes);

      folioSpringLiquibase.setDefaultSchema(getSchemaName());
      log.info(
        "About to start liquibase update for tenant [{}]",
        context.getTenantId()
      );

      try {
        folioSpringLiquibase.performLiquibaseUpdate();
      } catch (LiquibaseException e) {
        throw new TenantUpgradeException(e);
      }

      log.info(
        "Liquibase update for tenant [{}] executed successfully",
        context.getTenantId()
      );

      afterLiquibaseUpdate(tenantAttributes);
    }

    afterTenantUpdate(tenantAttributes);
  }

  /**
   * @throws NotFoundException when tenant not found.
   */
  public void deleteTenant(TenantAttributes tenantAttributes) {
    if (tenantExists()) {
      beforeTenantDeletion(tenantAttributes);

      log.info("Removing [{}] tenant...", context.getTenantId());
      jdbcTemplate.execute(String.format(DESTROY_SQL, getSchemaName()));

      afterTenantDeletion(tenantAttributes);
    }
  }

  /**
   * Check if the tenant exists (by way of its database schema)
   * @return if the tenant's database schema exists
   */
  protected boolean tenantExists() {
    return isTrue(
      jdbcTemplate.query(
        EXIST_SQL,
        (ResultSet resultSet) -> resultSet.next() && resultSet.getBoolean(1),
        getSchemaName()
      )
    );
  }

  /**
   * The name of the tenant's schema, based on the tenant and current module names.  This schema
   * is not guaranteed exist: this is just what it would be named.
   * @return the schema's name
   */
  protected String getSchemaName() {
    return context
      .getFolioModuleMetadata()
      .getDBSchemaName(context.getTenantId());
  }

  /**
   * Load any applicable reference data
   */
  public void loadReferenceData() {
    log.warn(
      "A tenant was created with loadReference=true, however, no reference data was created"
    );
    log.warn("Please extend TenantService and implement loadReferenceData");
  }

  /**
   * Load any applicable sample data
   */
  public void loadSampleData() {
    log.warn(
      "A tenant was created with loadSample=true, however, no reference data was created"
    );
    log.warn("Please extend TenantService and implement loadSampleData");
  }

  /**
   * Custom logic to be ran before a tenant is created or updated
   */
  protected void beforeTenantUpdate(TenantAttributes tenantAttributes) {
    // implementation up to modules
  }

  /**
   * Custom logic to be ran before a tenant's database is created with liquibase
   */
  protected void beforeLiquibaseUpdate(TenantAttributes tenantAttributes) {
    // implementation up to modules
  }

  /**
   * Custom logic to be ran after a tenant's database is created with liquibase
   */
  protected void afterLiquibaseUpdate(TenantAttributes tenantAttributes) {
    // implementation up to modules
  }

  /**
   * Custom logic to be ran after a tenant is created or updated
   */
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    // implementation up to modules
  }

  /**
   * Custom logic to be ran before a tenant is deleted
   */
  protected void beforeTenantDeletion(TenantAttributes tenantAttributes) {
    // implementation up to modules
  }

  /**
   * Custom logic to be ran after a tenant is deleted
   */
  protected void afterTenantDeletion(TenantAttributes tenantAttributes) {
    // implementation up to modules
  }
}
