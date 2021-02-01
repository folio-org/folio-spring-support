package org.folio.spring.config;

import org.folio.spring.exception.NotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class DefaultExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(Exception ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }

  @ExceptionHandler({DataIntegrityViolationException.class, ConstraintViolationException.class})
  public ResponseEntity<String> handleConstraintViolationException(Exception ex) {
    /*
     * In the case of Constraint Violation exceptions, we must reach the main cause of the wrapped in each other exceptions
     * It will be either SQLException or platform-specific exception like PSQLException and it should contain
     * all the details related to the original database error/exception
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
  public ResponseEntity<String> handleNotFoundException(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
  }
}
