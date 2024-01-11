package org.folio.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import org.folio.spring.exception.NotFoundException;
import org.folio.spring.testing.type.UnitTest;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@UnitTest
class DefaultExceptionHandlerTest {

  private static final String EXCEPTION_MSG = "Exception";

  private final DefaultExceptionHandler handler = new DefaultExceptionHandler();

  @Test
  void shouldReturnBadRequestResponseForIllegalArgumentException() {
    assertThat(handler.handleIllegalArgumentException(new IllegalArgumentException(EXCEPTION_MSG)))
        .satisfies(res -> {
          assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
          assertEquals(EXCEPTION_MSG, res.getBody());
        });
  }

  @Test
  void shouldReturnNotFoundResponseForNotFoundException() {
    assertThat(handler.handleNotFoundException(new NotFoundException(EXCEPTION_MSG)))
        .satisfies(res -> {
          assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
          assertEquals(EXCEPTION_MSG, res.getBody());
        });
  }

  @Test
  void shouldReturnBadRequestResponseForConstraintViolationException() {
    assertThat(
      handler.handleConstraintViolationException(new ConstraintViolationException(EXCEPTION_MSG, null, "not null")))
        .satisfies(res -> {
          assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
          assertEquals(EXCEPTION_MSG, res.getBody());
          assertEquals(MediaType.TEXT_PLAIN, res.getHeaders().getContentType());
        });
  }

  @Test
  void shouldReturnBadRequestResponseWithRootCauseForConstraintViolationException() {
    ConstraintViolationException ex = new ConstraintViolationException(EXCEPTION_MSG, new SQLException("SQL Exception"),
        "not null");

    assertThat(handler.handleConstraintViolationException(ex))
        .satisfies(res -> {
          assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
          assertEquals("SQL Exception", res.getBody());
          assertEquals(MediaType.TEXT_PLAIN, res.getHeaders().getContentType());
        });
  }

}
