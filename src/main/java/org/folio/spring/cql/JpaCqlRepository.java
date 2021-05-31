package org.folio.spring.cql;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import org.folio.spring.data.OffsetRequest;

@NoRepositoryBean
public interface JpaCqlRepository<T, ID> extends JpaRepository<T, ID> {

  Page<T> findByCQL(String cql, OffsetRequest offset);

  long count(String cql);
}
