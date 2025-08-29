package org.folio.spring.exception;

/**
 * Exception thrown when an error occurs during Folio context execution.
 */
public class FolioContextExecutionException extends RuntimeException {

  /**
   * Constructs a new FolioContextExecutionException with the specified detail message.
   *
   * @param message the detail message
   */
  public FolioContextExecutionException(String message) {
    super(message);
  }

  /**
   * Constructs a new FolioContextExecutionException with the specified cause.
   *
   * @param cause the cause of the exception
   */
  public FolioContextExecutionException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs a new FolioContextExecutionException with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of the exception
   */
  public FolioContextExecutionException(String message, Throwable cause) {
    super(message, cause);
  }
}
