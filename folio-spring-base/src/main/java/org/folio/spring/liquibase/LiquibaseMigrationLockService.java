package org.folio.spring.liquibase;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.folio.spring.exception.LiquibaseMigrationException;

/**
 * Service that determines whether Liquibase migration should be considered in progress. Returns {@code true} when
 * message processing should retry, {@code false} when the database is ready, and throws
 * {@link LiquibaseMigrationException} if the state cannot be determined.
 */
@RequiredArgsConstructor
public class LiquibaseMigrationLockService {

  public static final String DEFAULT_LOCK_TABLE = "databasechangeloglock";
  private static final String LOCK_QUERY_TEMPLATE =
    "SELECT COUNT(*) AS total_rows, COUNT(*) FILTER (WHERE locked = true) AS locked_rows FROM %s";

  private final DataSource dataSource;
  private final String lockTable;

  public boolean isMigrationRunning() {
    try (var connection = dataSource.getConnection();
      var statement = connection.prepareStatement(lockQuery())) {
      try (var resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return true;
        }

        if (resultSet.getInt(1) == 0) {
          return true;
        }

        return resultSet.getInt(2) > 0;
      }
    } catch (LiquibaseMigrationException e) {
      throw e;
    } catch (SQLException e) {
      if (isMissingLockTable(e)) {
        return true;
      }
      throw new LiquibaseMigrationException("Failed to determine Liquibase migration state", e);
    } catch (RuntimeException e) {
      throw new LiquibaseMigrationException("Failed to determine Liquibase migration state", e);
    }
  }

  private String lockQuery() {
    return LOCK_QUERY_TEMPLATE.formatted(defaultIfBlank(lockTable, DEFAULT_LOCK_TABLE));
  }

  private boolean isMissingLockTable(SQLException exception) {
    return exception.getMessage() != null && exception.getMessage().toLowerCase().contains("does not exist");
  }
}
