package org.folio.spring.liquibase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.folio.spring.exception.LiquibaseMigrationException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class LiquibaseMigrationLockServiceTest {

  private static final String LOCK_TABLE = "custom_lock_table";

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement preparedStatement;

  @Mock
  private ResultSet resultSet;

  private LiquibaseMigrationLockService service;

  @BeforeEach
  void setUp() throws Exception {
    when(dataSource.getConnection()).thenReturn(connection);

    service = new LiquibaseMigrationLockService(
      dataSource,
      LOCK_TABLE
    );
  }

  @Test
  void isMigrationRunning_returnsTrue_whenLockTableDoesNotExist() throws Exception {
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenThrow(new java.sql.SQLException("relation does not exist"));

    var result = service.isMigrationRunning();

    assertTrue(result);
  }

  @Test
  void isMigrationRunning_returnsTrue_whenLockTableIsNotInitialized() throws Exception {
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(1)).thenReturn(0);

    var result = service.isMigrationRunning();

    assertTrue(result);
  }

  @Test
  void isMigrationRunning_returnsTrue_whenActiveLockExists() throws Exception {
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(1)).thenReturn(1);
    when(resultSet.getInt(2)).thenReturn(1);

    var result = service.isMigrationRunning();

    assertTrue(result);
  }

  @Test
  void isMigrationRunning_returnsFalse_whenNoActiveLockExists() throws Exception {
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(1)).thenReturn(1);
    when(resultSet.getInt(2)).thenReturn(0);

    var result = service.isMigrationRunning();

    assertFalse(result);
  }

  @Test
  void isMigrationRunning_usesConfiguredLockTableName_withoutSchemaLookup() throws Exception {
    var sql = "SELECT COUNT(*) AS total_rows, COUNT(*) FILTER (WHERE locked = true) AS locked_rows "
      + "FROM custom_lock_table";
    when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(1)).thenReturn(1);
    when(resultSet.getInt(2)).thenReturn(0);

    service.isMigrationRunning();
  }

  @Test
  void isMigrationRunning_usesDefaultLockTable_whenConfiguredLockTableIsBlank() throws Exception {
    service = new LiquibaseMigrationLockService(dataSource, null);

    var sql = "SELECT COUNT(*) AS total_rows, COUNT(*) FILTER (WHERE locked = true) AS locked_rows "
      + "FROM databasechangeloglock";
    when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getInt(1)).thenReturn(1);
    when(resultSet.getInt(2)).thenReturn(0);

    var result = service.isMigrationRunning();

    assertFalse(result);
  }

  @Test
  void isMigrationRunning_throwsException_whenMigrationStateCannotBeDetermined() throws Exception {
    when(connection.prepareStatement(anyString())).thenThrow(new IllegalStateException("boom"));

    assertThrows(LiquibaseMigrationException.class, () -> service.isMigrationRunning());
  }

  @Test
  void isMigrationRunning_throwsException_whenConnectionCannotBeObtained() throws Exception {
    when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("db down"));

    service = new LiquibaseMigrationLockService(
      dataSource,
      LOCK_TABLE
    );

    assertThrows(LiquibaseMigrationException.class, () -> service.isMigrationRunning());
  }
}
