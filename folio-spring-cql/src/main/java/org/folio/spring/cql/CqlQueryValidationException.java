package org.folio.spring.cql;

/**
 * The CQL query provided does not appear to be valid or feature is not supported.
 */
public class CqlQueryValidationException extends RuntimeException {

  public CqlQueryValidationException(Exception e) {
    super(e);
  }

  public CqlQueryValidationException(String s) {
    super(s);
  }
}
