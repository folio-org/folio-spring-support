package org.folio.spring.data;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.folio.spring.testing.extension.RandomInt;
import org.folio.spring.testing.extension.RandomLong;
import org.folio.spring.testing.extension.impl.RandomParametersExtension;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UnitTest
@ExtendWith(RandomParametersExtension.class)
class OffsetRequestTest {

  private OffsetRequest req;

  @Test
  void shouldCreateNewRequest() {
    req = new OffsetRequest(0, 1, Sort.unsorted());

    assertEquals(0, req.getOffset());
    assertEquals(1, req.getPageSize());
    assertEquals(Sort.unsorted(), req.getSort());
  }

  @Test
  void shouldCreateNewRequestWithDefaultSort() {
    req = new OffsetRequest(0, 1);

    assertEquals(0, req.getOffset());
    assertEquals(1, req.getPageSize());
    assertEquals(OffsetRequest.DEFAULT_SORT, req.getSort());
  }

  @Test
  void shouldNotCreateNewRequestWithNegativeOffset(@RandomLong(max = 0) long offset) {
    assertThrows(IllegalArgumentException.class, () -> new OffsetRequest(offset, 1));
  }

  @ParameterizedTest
  @MethodSource("provideNegativeAndZeroInt")
  void shouldNotCreateNewRequestWithNegativeOrZeroLimit(int limit) {
    assertThrows(IllegalArgumentException.class, () -> new OffsetRequest(0, limit));
  }

  @Test
  void shouldNotCreateNewRequestWithNullSort() {
    assertThrows(NullPointerException.class, () -> new OffsetRequest(0, 1, null));
  }

  @Test
  void shouldCalculatePageNumberFromOffsetAndLimit(
      @RandomLong(min = 0, max = Integer.MAX_VALUE) long offset,
      @RandomInt(min = 1, max = 100) int limit) {
    req = new OffsetRequest(offset, limit);

    assertEquals(offset / limit, req.getPageNumber());
  }

  private static List<Integer> provideNegativeAndZeroInt() {
    return List.of(-nextInt(1, Integer.MAX_VALUE), 0);
  }

  @Nested
  @DisplayName("Navigation when offset greater than limit")
  class NavigationWithOffsetGreater {

    @BeforeEach
    void setUp(
        @RandomLong(min = 100, max = Integer.MAX_VALUE) long offset,
        @RandomInt(min = 1, max = 100) int limit) {

      req = new OffsetRequest(offset, limit);
    }

    @Test
    void shouldReturnNextPage() {
      Pageable next = req.next();

      assertEquals(new OffsetRequest(
          req.getOffset() + req.getPageSize(),
          req.getPageSize(),
          req.getSort()),
        next);
    }

    @Test
    void shouldReturnPreviousPage() {
      Pageable previous = req.previous();

      assertEquals(new OffsetRequest(
              req.getOffset() - req.getPageSize(),
              req.getPageSize(),
              req.getSort()),
          previous);
    }

    @Test
    void shouldReturnPreviousButNotFirst() {
      Pageable previousOrFirst = req.previousOrFirst();

      assertEquals(req.previous(), previousOrFirst);
    }

    @Test
    void shouldReturnFirstPage() {
      Pageable first = req.first();

      assertEquals(new OffsetRequest(
              0,
              req.getPageSize(),
              req.getSort()),
          first);
    }

    @Test
    void shouldReturnSpecifiedPage() {
      var pageNumber = 10;
      Pageable first = req.withPage(pageNumber);

      assertEquals(new OffsetRequest(
          (long) pageNumber * req.getPageSize(),
          req.getPageSize(),
          req.getSort()),
        first);
    }

    @Test
    void shouldHavePreviousPage() {
      assertTrue(req.hasPrevious());
    }
  }

  @Nested
  @DisplayName("Navigation when offset less than limit")
  class NavigationWithOffsetLess {

    @BeforeEach
    void setUp(
        @RandomLong(min = 0, max = 100) long offset,
        @RandomInt(min = 100, max = 1000) int limit) {

      req = new OffsetRequest(offset, limit);
    }

    @Test
    void shouldReturnNextPage() {
      Pageable next = req.next();

      assertEquals(new OffsetRequest(
              req.getOffset() + req.getPageSize(),
              req.getPageSize(),
              req.getSort()),
          next);
    }

    @Test
    void shouldReturnSamePageAsPreviousPage() {
      Pageable previous = req.previous();

      assertEquals(req, previous);
    }

    @Test
    void shouldReturnFirstButNotPrevious() {
      Pageable previousOrFirst = req.previousOrFirst();

      assertEquals(req.first(), previousOrFirst);
    }

    @Test
    void shouldReturnFirstPage() {
      Pageable first = req.first();

      assertEquals(new OffsetRequest(
              0,
              req.getPageSize(),
              req.getSort()),
          first);
    }

    @Test
    void shouldNotHavePreviousPage() {
      assertFalse(req.hasPrevious());
    }
  }
}
