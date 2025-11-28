package org.folio.spring.config;

import lombok.extern.log4j.Log4j2;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.exception.TenantUpgradeException;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Log4j2
@ControllerAdvice
public class DefaultExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<@NonNull String> handleIllegalArgumentException(Exception ex) {
    logExceptionHandling(ex);
    return ResponseEntity.badRequest().body(ex.getMessage());
  }

  @ExceptionHandler({DataIntegrityViolationException.class, ConstraintViolationException.class})
  public ResponseEntity<@NonNull String> handleConstraintViolationException(Exception ex) {
    logExceptionHandling(ex);
    /*
     * In the case of Constraint Violation exceptions, we must reach the main cause of the wrapped
     * in each other exceptions. It will be either SQLException or platform-specific exception like PSQLException
     * and it should contain all the details related to the original database error/exception
     */
    Throwable e = ex;
    var cause = ex.getCause();
    while (cause != null && cause != e) {
      e = cause;
      cause = e.getCause();
    }

    return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body(e.getMessage());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<@NonNull String> handleNotFoundException(NotFoundException ex) {
    logExceptionHandling(ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(TenantUpgradeException.class)
  public ResponseEntity<@NonNull String> handleTenantUpdateException(TenantUpgradeException ex) {
    logExceptionHandling(ex);
    return ResponseEntity.badRequest().body("Liquibase error: " + ex.getMessage());
  }

  private static void logExceptionHandling(Exception exception) {
    log.warn("Handling exception", exception);
  }
}
