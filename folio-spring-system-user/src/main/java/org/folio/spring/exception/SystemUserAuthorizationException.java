package org.folio.spring.exception;

public class SystemUserAuthorizationException extends RuntimeException {
  public SystemUserAuthorizationException(String message) {
    super(message);
  }

  public SystemUserAuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }
}
