package org.folio.spring.cql;

import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface JpaCqlRepository<T, I> extends JpaRepository<T, I> {

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  Page<T> findByCQL(String cql, OffsetRequest offset);

  long count(String cql);
}
