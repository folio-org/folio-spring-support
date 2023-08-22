package org.folio.spring.cql.repo;

import org.folio.spring.cql.domain.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface PersonCustomRepository {
  static Specification<Person> deletedIs(Boolean deleted) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("deleted"), deleted);
  }

  Page<Person> findByCqlAndDeletedFalse(String cql, Pageable pageable);

  long countDeletedFalse(String cql);
}
