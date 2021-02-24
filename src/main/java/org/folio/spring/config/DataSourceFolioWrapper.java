package org.folio.spring.config;

import static org.folio.spring.utils.TenantUtil.isValidTenantName;

import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DataSourceFolioWrapper extends DelegatingDataSource {
  private final FolioExecutionContext folioExecutionContext;

  public DataSourceFolioWrapper(DataSource targetDataSource, FolioExecutionContext folioExecutionContext) {
    super(targetDataSource);
    this.folioExecutionContext = folioExecutionContext;
  }

  private Connection prepareConnectionSafe(Connection connection) throws SQLException {
    if (connection != null) {

      var schemaName = "public";
      var tenantId = folioExecutionContext.getTenantId();
      if (StringUtils.isNotBlank(tenantId)) {
        if (!isValidTenantName(tenantId)) {
          throw new IllegalArgumentException("Invalid tenant name: " + tenantId);
        }
        schemaName = folioExecutionContext.getFolioModuleMetadata().getDBSchemaName(tenantId) + ", public";
      }
      try (var statement = connection.prepareStatement(String.format("SET search_path = %s;", schemaName))) {
        statement.execute();
      }

      return connection;
    }
    return null;
  }

  @Override
  public Connection getConnection() throws SQLException {
    return prepareConnectionSafe(obtainTargetDataSource().getConnection());
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    return prepareConnectionSafe(obtainTargetDataSource().getConnection(username, password));
  }
}
