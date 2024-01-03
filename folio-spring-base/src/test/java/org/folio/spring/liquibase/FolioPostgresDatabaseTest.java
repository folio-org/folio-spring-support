package org.folio.spring.liquibase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import liquibase.database.DatabaseConnection;
import liquibase.exception.DatabaseException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class FolioPostgresDatabaseTest {

  private final FolioPostgresDatabase database = new FolioPostgresDatabase();

  @Mock
  private DatabaseConnection connection;

  @BeforeEach
  void setUp() {
    database.setConnection(connection);
  }

  @Test
  void testPriority() {
    var actual = database.getPriority();
    assertEquals(2, actual);
  }

  @Test
  void testRollbackFailedWithException() throws DatabaseException {
    doThrow(DatabaseException.class).when(connection).rollback();
    assertThrows(DatabaseException.class, database::rollback);
  }

  @Test
  void testRollbackSucceed() throws DatabaseException {
    doNothing().when(connection).rollback();
    assertDoesNotThrow(database::rollback);
  }
}
