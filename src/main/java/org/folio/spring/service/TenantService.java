package org.folio.spring.service;

import java.util.Optional;
import liquibase.exception.LiquibaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.repository.impl.DbSystemUserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
@Lazy
public class TenantService {
  private static final String DESTROY_SQL = "DROP SCHEMA IF EXISTS %1$s CASCADE; DROP ROLE IF EXISTS %1$s";
  private static final String EXIST_SQL = "SELECT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=?)";

  private final Optional<JdbcTemplate> jdbcTemplateOpt;
  private final FolioExecutionContext context;
  private final Optional<FolioSpringLiquibase> folioSpringLiquibaseOpt;
  private final Optional<SystemUserService> optionalSecurityManagerServiceOpt;

  public void createTenant() throws LiquibaseException {
    if (folioSpringLiquibaseOpt.isPresent()) {
      folioSpringLiquibaseOpt.get().setDefaultSchema(getSchemaName());
      log.info("About to start liquibase update for tenant [{}]",
        context.getTenantId());

      folioSpringLiquibaseOpt.get().performLiquibaseUpdate();

      log.info("Liquibase update for tenant [{}] executed successfully",
        context.getTenantId());
    }

    prepareSystemUser();
  }

  /**
   * @throws NotFoundException when tenant not found.
   */
  public void deleteTenant() {
    if (!tenantExists()) {
      throw new NotFoundException("Tenant does not exist: " + context.getTenantId());
    }

    jdbcTemplateOpt.ifPresent(template -> {
      log.info("Removing [{}] tenant...", context.getTenantId());

      template.execute(String.format(DESTROY_SQL, getSchemaName()));
    });
  }

  public boolean tenantExists() {
    return jdbcTemplateOpt
      .map(template -> template.queryForObject(EXIST_SQL, Boolean.class, getSchemaName()))
      .orElse(false);
  }

  private String getSchemaName() {
    return context.getFolioModuleMetadata().getDBSchemaName(context.getTenantId());
  }

  private void prepareSystemUser() {
    jdbcTemplateOpt.ifPresent(template -> {
      log.info("Creating 'system_user_parameters' table...");

      template.execute(createUserParametersTableQuery());
    });


    optionalSecurityManagerServiceOpt.ifPresentOrElse(service -> {
        log.info("Preparing system user...");

        service.prepareSystemUser();
      }, () -> log.info("Skipping system user creation"));
  }

  private String createUserParametersTableQuery() {
    return "CREATE TABLE IF NOT EXISTS " + DbSystemUserRepository.TABLE_NAME +
      "(" +
      "    id          UUID PRIMARY KEY," +
      "    username    VARCHAR(50) NOT NULL," +
      "    password    VARCHAR(50) NOT NULL," +
      "    okapi_token VARCHAR(8000)," +
      "    okapi_url   VARCHAR(100)," +
      "    tenant_id   VARCHAR(100)" +
      ")";
  }
}
