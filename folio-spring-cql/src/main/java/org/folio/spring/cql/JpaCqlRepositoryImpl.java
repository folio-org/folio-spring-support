package org.folio.spring.cql;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.data.OffsetRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.support.PageableExecutionUtils;

@Log4j2
@NoRepositoryBean
public class JpaCqlRepositoryImpl<T, I> extends SimpleJpaRepository<T, I> implements JpaCqlRepository<T, I> {

  private final Class<T> domainClass;
  private final EntityManager em;
  private final Cql2JpaCriteria<T> cql2JpaCriteria;

  public JpaCqlRepositoryImpl(JpaEntityInformation<T, I> entityInformation,
                              EntityManager entityManager) {
    super(entityInformation, entityManager);
    this.domainClass = entityInformation.getJavaType();
    this.em = entityManager;
    this.cql2JpaCriteria = new Cql2JpaCriteria<>(domainClass, em);
  }

  public JpaCqlRepositoryImpl(Class<T> domainClass, EntityManager em) {
    super(domainClass, em);
    this.domainClass = domainClass;
    this.em = em;
    this.cql2JpaCriteria = new Cql2JpaCriteria<>(domainClass, em);
  }

  @Override
  public Page<T> findByCQL(String cql, OffsetRequest pageable) {
    var criteria = cql2JpaCriteria.toCollectCriteria(cql);
    List<T> resultList = em
      .createQuery(criteria)
      .setFirstResult((int) pageable.getOffset())
      .setMaxResults(pageable.getPageSize())
      .getResultList();
    return PageableExecutionUtils.getPage(resultList, pageable, () -> count(cql));
  }

  @Override
  public long count(String cql) {
    var criteria = cql2JpaCriteria.toCountCriteria(cql);
    return em.createQuery(criteria).getSingleResult();
  }

}
