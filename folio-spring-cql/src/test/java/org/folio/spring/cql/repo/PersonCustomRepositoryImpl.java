package org.folio.spring.cql.repo;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.folio.spring.cql.Cql2JpaCriteria;
import org.folio.spring.cql.domain.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;

public class PersonCustomRepositoryImpl implements PersonCustomRepository {

  private final EntityManager em;
  private final Cql2JpaCriteria<Person> cql2JpaCriteria;

  public PersonCustomRepositoryImpl(EntityManager em) {
    this.em = em;
    this.cql2JpaCriteria = new Cql2JpaCriteria<>(Person.class, em);
  }

  @Override
  public Page<Person> findByCqlAndDeletedFalse(String cql, Pageable pageable) {
    var collectBy = collectByQueryAndDeletedFalse(cql);
    var countBy = countByQueryAndDeletedFalse(cql);
    var criteria = cql2JpaCriteria.toCollectCriteria(collectBy);

    List<Person> resultList = em
      .createQuery(criteria)
      .setFirstResult((int) pageable.getOffset())
      .setMaxResults(pageable.getPageSize())
      .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(countBy));
  }

  @Override
  public long countDeletedFalse(String cql) {
    var countBy = countByQueryAndDeletedFalse(cql);
    return count(countBy);
  }

  private long count(Specification<Person> specification) {
    var criteria = cql2JpaCriteria.toCountCriteria(specification);
    return em.createQuery(criteria).getSingleResult();
  }

  private Specification<Person> collectByQueryAndDeletedFalse(String cqlQuery) {
    return PersonCustomRepository.deletedIs(false).and(cql2JpaCriteria.createCollectSpecification(cqlQuery));
  }

  private Specification<Person> countByQueryAndDeletedFalse(String cqlQuery) {
    return PersonCustomRepository.deletedIs(false).and(cql2JpaCriteria.createCountSpecification(cqlQuery));
  }
}
