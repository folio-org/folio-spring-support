package org.folio.spring.liquibase;

import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.exception.LiquibaseMigrationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service that determines whether Liquibase migration should be considered in progress. Returns {@code true} when
 * message processing should retry, {@code false} when the database is ready, and throws
 * {@link LiquibaseMigrationException} if the state cannot be determined.
 */
@Log4j2
@RequiredArgsConstructor
public class LiquibaseMigrationLockService {

  private static final String DEFAULT_LOCK_TABLE = "databasechangeloglock";
  private static final String LOCK_QUERY_TEMPLATE = "SELECT COUNT(*) = 0 FROM %s WHERE locked = false";

  private final JdbcTemplate jdbcTemplate;
  private final String lockTable;

  public boolean isMigrationRunning() {
    var resolvedLockTable = resolvedLockTable();
    try {
      var migrationRunning = jdbcTemplate.queryForObject(lockQuery(resolvedLockTable), Boolean.class);
      var result = isNotFalse(migrationRunning);
      log.debug("Liquibase migration running for table '{}': {}", resolvedLockTable, result);
      return result;
    } catch (LiquibaseMigrationException e) {
      throw e;
    } catch (RuntimeException e) {
      if (isMissingLockTable(e)) {
        log.debug("Liquibase lock table '{}' is not available yet; treating migration as running", resolvedLockTable);
        return true;
      }
      throw new LiquibaseMigrationException("Failed to determine Liquibase migration state", e);
    }
  }

  private String resolvedLockTable() {
    return defaultIfBlank(lockTable, DEFAULT_LOCK_TABLE);
  }

  private boolean isMissingLockTable(Throwable exception) {
    for (Throwable current = exception; current != null; current = current.getCause()) {
      var message = current.getMessage();
      if (message != null && message.toLowerCase(ROOT).contains("does not exist")) {
        return true;
      }
    }
    return false;
  }

  private static String lockQuery(String table) {
    return LOCK_QUERY_TEMPLATE.formatted(table);
  }
}
