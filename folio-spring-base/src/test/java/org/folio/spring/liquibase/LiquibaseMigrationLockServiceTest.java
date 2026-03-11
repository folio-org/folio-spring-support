package org.folio.spring.liquibase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.folio.spring.exception.LiquibaseMigrationException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LiquibaseMigrationLockServiceTest {

  private static final String LOCK_TABLE = "custom_lock_table";

  @Mock
  private JdbcTemplate jdbcTemplate;

  private LiquibaseMigrationLockService service;

  @BeforeEach
  void setUp() {
    service = new LiquibaseMigrationLockService(
      jdbcTemplate,
      LOCK_TABLE
    );
  }

  @Test
  void isMigrationRunning_returnsTrue_whenLockTableDoesNotExist() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class)))
      .thenThrow(new RuntimeException("relation does not exist"));

    var result = service.isMigrationRunning();

    assertTrue(result);
  }

  @Test
  void isMigrationRunning_returnsTrue_whenMissingTableIsInRootCauseMessage() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class)))
      .thenThrow(new RuntimeException("wrapper", new java.sql.SQLException("relation does not exist")));

    var result = service.isMigrationRunning();

    assertTrue(result);
  }

  @Test
  void isMigrationRunning_returnsTrue_whenQueryResultIsNull() {
    mockMigrationRunningQueryResult(null);

    var result = service.isMigrationRunning();

    assertTrue(result);
  }

  @Test
  void isMigrationRunning_returnsTrue_whenQueryResultIsTrue() {
    mockMigrationRunningQueryResult(true);

    var result = service.isMigrationRunning();

    assertTrue(result);
  }

  @Test
  void isMigrationRunning_returnsFalse_whenQueryResultIsFalse() {
    mockMigrationRunningQueryResult(false);

    var result = service.isMigrationRunning();

    assertFalse(result);
  }

  @Test
  void isMigrationRunning_usesConfiguredLockTableName_withoutSchemaLookup() {
    when(jdbcTemplate.queryForObject(argThat(sql -> sql.contains("COUNT(*) = 0")
      && sql.contains("FROM custom_lock_table")
      && sql.contains("WHERE locked = false")), eq(Boolean.class)))
      .thenReturn(false);

    var result = service.isMigrationRunning();

    assertFalse(result);
  }

  @Test
  void isMigrationRunning_usesDefaultLockTable_whenConfiguredLockTableIsBlank() {
    service = new LiquibaseMigrationLockService(jdbcTemplate, null);

    when(jdbcTemplate.queryForObject(argThat(sql -> sql.contains("COUNT(*) = 0")
      && sql.contains("FROM databasechangeloglock")
      && sql.contains("WHERE locked = false")), eq(Boolean.class)))
      .thenReturn(false);

    var result = service.isMigrationRunning();

    assertFalse(result);
  }

  @Test
  void isMigrationRunning_throwsException_whenMigrationStateCannotBeDetermined() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenThrow(new IllegalStateException("boom"));

    assertThrows(LiquibaseMigrationException.class, () -> service.isMigrationRunning());
  }

  @Test
  void isMigrationRunning_throwsException_whenAllExceptionMessagesAreNull() {
    when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class)))
      .thenThrow(new RuntimeException((String) null, new java.sql.SQLException((String) null)));

    assertThrows(LiquibaseMigrationException.class, () -> service.isMigrationRunning());
  }

  @Test
  void isMigrationRunning_rethrowsLiquibaseMigrationException_withoutWrapping() {
    var migrationException = new LiquibaseMigrationException("already wrapped");
    when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenThrow(migrationException);

    service = new LiquibaseMigrationLockService(jdbcTemplate, LOCK_TABLE);

    var exception = assertThrows(LiquibaseMigrationException.class, () -> service.isMigrationRunning());

    assertSame(migrationException, exception);
  }

  private void mockMigrationRunningQueryResult(Boolean migrationRunning) {
    when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(migrationRunning);
  }
}
