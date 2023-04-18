package org.folio.spring.cql.repo;

import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.cql.domain.Person;

public interface PersonRepository extends JpaCqlRepository<Person, Integer> {}
