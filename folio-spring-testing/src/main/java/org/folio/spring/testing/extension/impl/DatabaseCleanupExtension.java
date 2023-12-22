package org.folio.spring.testing.extension.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * JUnit 5 extension for database cleanup after test execution.
 * This extension implements {@link AfterEachCallback} to clear specified tables in the database.
 */
public class DatabaseCleanupExtension implements AfterEachCallback {

  /**
   * Clears specified tables in the database after each test execution based on the {@link DatabaseCleanup} annotation.
   *
   * @param context The extension context
   */
  @Override
  public void afterEach(ExtensionContext context) {
    Optional.ofNullable(context.getRequiredTestMethod().getAnnotation(DatabaseCleanup.class))
      .or(() -> Optional.ofNullable(context.getRequiredTestClass().getAnnotation(DatabaseCleanup.class)))
      .ifPresent(annotation -> clearTables(annotation.tables(), annotation.tenants(), context));
  }

  @SuppressWarnings("java:S2077")
  private void clearTables(String[] tableNames, String[] tenants, ExtensionContext context) {
    var applicationContext = SpringExtension.getApplicationContext(context);
    var jdbcTemplate = applicationContext.getBean(JdbcTemplate.class);
    var folioModuleMetadata = applicationContext.getBean(FolioModuleMetadata.class);
    if (tenants == null || tenants.length == 0) {
      String query = "select nspname from pg_catalog.pg_namespace where nspname LIKE '%_"
        + folioModuleMetadata.getModuleName().replace('-', '_') + "';";
      var existedTenants = jdbcTemplate.query(query,
        (rs, rowNum) -> mapToTenant(rs, folioModuleMetadata.getModuleName()));
      tenants = existedTenants.toArray(new String[0]);
    }
    var fullTableNames = getFullTableNames(tableNames, tenants, folioModuleMetadata);
    for (String fullTableName : fullTableNames) {
      jdbcTemplate.execute("delete from " + fullTableName + ";");
    }
  }

  @NotNull
  private String[] getFullTableNames(String[] tableNames, String[] tenants, FolioModuleMetadata folioModuleMetadata) {
    var fullTableNames = new ArrayList<String>();
    for (String tenant : tenants) {
      for (String tableName : tableNames) {
        fullTableNames.add(getTableName(tableName, tenant, folioModuleMetadata));
      }
    }
    return fullTableNames.toArray(new String[0]);
  }

  private String mapToTenant(ResultSet rs, String moduleName) throws SQLException {
    String nsTenant = rs.getString("nspname");
    String suffix = "_" + moduleName;
    int tenantNameLength = nsTenant.length() - suffix.length();
    return nsTenant.substring(0, tenantNameLength);
  }

  private String getTableName(String tableName, String tenantId, FolioModuleMetadata metadata) {
    return metadata.getDBSchemaName(tenantId) + "." + tableName;
  }
}
