package org.folio.spring.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class FolioContextExecutionExceptionTest {

  @Test
  void testConstructorWithMessage() {
    String message = "Test message";
    FolioContextExecutionException ex = new FolioContextExecutionException(message);
    assertEquals(message, ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void testConstructorWithCause() {
    Throwable cause = new RuntimeException("Cause");
    FolioContextExecutionException ex = new FolioContextExecutionException(cause);
    assertEquals(cause, ex.getCause());
    assertTrue(ex.getMessage().contains(cause.toString()));
  }

  @Test
  void testConstructorWithMessageAndCause() {
    String message = "Test message";
    Throwable cause = new RuntimeException("Cause");
    FolioContextExecutionException ex = new FolioContextExecutionException(message, cause);
    assertEquals(message, ex.getMessage());
    assertEquals(cause, ex.getCause());
  }
}

