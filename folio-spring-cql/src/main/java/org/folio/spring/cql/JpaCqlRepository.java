package org.folio.spring.cql;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface JpaCqlRepository<T, I> extends JpaRepository<T, I> {

  Page<T> findByCql(String cql, Pageable pageable);

  Page<T> findByCqlIgnoreCase(String cql, Pageable pageable, boolean ignoreCase);

  long count(String cql);

  long countIgnoreCase(String cql, boolean ignoreCase);
}
