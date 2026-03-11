package org.folio.spring.exception;

/**
 * Exception thrown when Liquibase migration state cannot be determined.
 */
public class LiquibaseMigrationException extends RuntimeException {

  public LiquibaseMigrationException(String message) {
    super(message);
  }

  public LiquibaseMigrationException(Throwable cause) {
    super(cause);
  }

  public LiquibaseMigrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
