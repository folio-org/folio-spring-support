package org.folio.spring.cql;

import java.util.List;

import javax.persistence.EntityManager;

import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.support.PageableExecutionUtils;

import org.folio.cql2pgjson.exception.QueryValidationException;
import org.folio.spring.data.OffsetRequest;

@Log4j2
@NoRepositoryBean
public class JpaCqlRepositoryImpl<T, ID> extends SimpleJpaRepository<T, ID> implements JpaCqlRepository<T, ID> {

  private final Class<T> domainClass;
  private final EntityManager em;
  private final Cql2JpaCriteria<T> cql2JPACriteria;

  public JpaCqlRepositoryImpl(JpaEntityInformation<T, ID> entityInformation,
                              EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.domainClass = entityInformation.getJavaType();
    this.em = entityManager;
    this.cql2JPACriteria = new Cql2JpaCriteria<>(domainClass, em);
  }

  public JpaCqlRepositoryImpl(Class<T> domainClass, EntityManager em) {
    super(domainClass, em);
    this.domainClass = domainClass;
    this.em = em;
    cql2JPACriteria = new Cql2JpaCriteria<>(domainClass, em);
  }

  @Override
  public Page<T> findByCQL(String cql, OffsetRequest pageable) {
    try {
      var criteria = cql2JPACriteria.toCollectCriteria(cql);
      List<T> resultList = em
        .createQuery(criteria)
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
      return PageableExecutionUtils.getPage(resultList, pageable, () -> count(cql));
    } catch (QueryValidationException e) {
      log.error("Can not invoke CQL query {} ", cql);
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public long count(String cql) {
    try {
      var criteria = cql2JPACriteria.toCountCriteria(cql);
      return em.createQuery(criteria).getSingleResult();
    } catch (QueryValidationException e) {
      log.error("Can not invoke CQL query {}. Exception {}", cql, e.getMessage());
      throw new IllegalArgumentException(e);
    }
  }

}
