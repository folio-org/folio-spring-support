package org.folio.spring.liquibase;

import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.exception.LiquibaseMigrationException;

/**
 * Service that determines whether Liquibase migration should be considered in progress. Returns {@code true} when
 * message processing should retry, {@code false} when the database is ready, and throws
 * {@link LiquibaseMigrationException} if the state cannot be determined.
 */
@Log4j2
@RequiredArgsConstructor
public class LiquibaseMigrationLockService {

  public static final String DEFAULT_LOCK_TABLE = "databasechangeloglock";
  private static final String LOCK_QUERY_TEMPLATE =
    "SELECT NOT EXISTS (SELECT 1 FROM %s WHERE locked = false) AS migration_running";

  private final DataSource dataSource;
  private final String lockTable;

  public boolean isMigrationRunning() {
    try (var connection = dataSource.getConnection();
      var statement = connection.prepareStatement(lockQuery())) {
      try (var resultSet = statement.executeQuery()) {
        var migrationRunning = !resultSet.next() || resultSet.getBoolean(1);
        log.debug("Liquibase migration running for table: {}", migrationRunning);
        return migrationRunning;
      }
    } catch (LiquibaseMigrationException e) {
      throw e;
    } catch (SQLException e) {
      if (isMissingLockTable(e)) {
        log.debug("Liquibase lock table is not available yet; treating migration as running");
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
    return exception.getMessage() != null
      && exception.getMessage().toLowerCase(ROOT).contains("does not exist");
  }
}
