package org.folio.spring.exception;

public class TenantUpdateException extends RuntimeException{

  public TenantUpdateException(Throwable cause) {
    super(cause);
  }
}
