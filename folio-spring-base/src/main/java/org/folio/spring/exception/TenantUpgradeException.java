package org.folio.spring.exception;

public class TenantUpgradeException extends RuntimeException {

  public TenantUpgradeException(Throwable cause) {
    super(cause);
  }
}
