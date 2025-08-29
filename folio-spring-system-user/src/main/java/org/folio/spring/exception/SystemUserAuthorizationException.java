package org.folio.spring.exception;

@Deprecated(since = "10.0.0", forRemoval = true)
public class SystemUserAuthorizationException extends RuntimeException {
  public SystemUserAuthorizationException(String message) {
    super(message);
  }

  public SystemUserAuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }
}
