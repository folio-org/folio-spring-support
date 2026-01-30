package org.folio.spring.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.jspecify.annotations.NonNull;
import org.springframework.jdbc.datasource.DelegatingDataSource;

@Log4j2
public class DataSourceFolioWrapper extends DelegatingDataSource {
  private static final Pattern NON_WORD_CHARACTERS = Pattern.compile("\\W");

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
        if (NON_WORD_CHARACTERS.matcher(tenantId).find()) {
          throw new IllegalArgumentException("Invalid tenant name: " + tenantId);
        }
        schemaName = folioExecutionContext.getFolioModuleMetadata().getDBSchemaName(tenantId) + ", public";
      }
      try (var statement = connection.prepareStatement(String.format("SET search_path = %s;", schemaName))) {
        log.trace("Changing search_path to {}", schemaName);
        statement.execute();
      }

      return connection;
    }
    return null;
  }

  @Override
  public @NonNull Connection getConnection() throws SQLException {
    return prepareConnectionSafe(obtainTargetDataSource().getConnection());
  }

  @Override
  public @NonNull Connection getConnection(String username, String password) throws SQLException {
    return prepareConnectionSafe(obtainTargetDataSource().getConnection(username, password));
  }
}
