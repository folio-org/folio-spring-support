package org.folio.spring.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class LiquibaseMigrationExceptionTest {

  @Test
  void testConstructorWithMessage() {
    String message = "Test message";
    LiquibaseMigrationException exception = new LiquibaseMigrationException(message);

    assertEquals(message, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testConstructorWithCause() {
    Throwable cause = new RuntimeException("Cause");
    LiquibaseMigrationException exception = new LiquibaseMigrationException(cause);

    assertSame(cause, exception.getCause());
    assertTrue(exception.getMessage().contains(cause.toString()));
  }

  @Test
  void testConstructorWithMessageAndCause() {
    String message = "Test message";
    Throwable cause = new RuntimeException("Cause");
    LiquibaseMigrationException exception = new LiquibaseMigrationException(message, cause);

    assertEquals(message, exception.getMessage());
    assertSame(cause, exception.getCause());
  }
}
