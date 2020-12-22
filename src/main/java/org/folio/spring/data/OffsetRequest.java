package org.folio.spring.data;

import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

public class OffsetRequest implements Pageable {

  private final int offset;
  private final int limit;
  private final Sort sort;

  public OffsetRequest(int offset, int limit) {
    this.limit = limit;
    this.offset = offset;
    this.sort = Sort.by(Direction.ASC, "id");
  }

  public OffsetRequest(int offset, int limit, Sort sort) {
    this.limit = limit;
    this.offset = offset;
    this.sort = sort;
  }

  @Override
  public int getPageNumber() {
    return offset / limit;
  }

  @Override
  public int getPageSize() {
    return limit;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetRequest((int) (getOffset() + getPageSize()), getPageSize());
  }

  public Pageable previous() {
    return hasPrevious() ?
      new OffsetRequest((int) (getOffset() - getPageSize()), getPageSize()) : this;
  }

  @Override
  public Pageable previousOrFirst() {
    return hasPrevious() ? previous() : first();
  }

  @Override
  public Pageable first() {
    return new OffsetRequest(0, getPageSize());
  }

  @Override
  public boolean hasPrevious() {
    return offset > limit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    OffsetRequest that = (OffsetRequest) o;
    return offset == that.offset && limit == that.limit && Objects.equals(sort, that.sort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(offset, limit, sort);
  }
}
