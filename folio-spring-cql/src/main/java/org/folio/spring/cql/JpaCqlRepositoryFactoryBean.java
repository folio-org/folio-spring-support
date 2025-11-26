package org.folio.spring.cql;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class JpaCqlRepositoryFactoryBean<T extends JpaRepository<S, I>, S, I extends Serializable>
  extends JpaRepositoryFactoryBean<T, S, I> {

  public JpaCqlRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
    super(repositoryInterface);
  }

  @Override
  protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {
    return new JpaCqlRepositoryFactory(em);
  }

  private static class JpaCqlRepositoryFactory extends JpaRepositoryFactory {

    private final EntityManager entityManager;

    JpaCqlRepositoryFactory(EntityManager em) {
      super(em);
      this.entityManager = em;
    }

    @Override
    protected RepositoryComposition.RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {
      RepositoryComposition.RepositoryFragments fragments = super.getRepositoryFragments(metadata);
      
      if (JpaCqlRepository.class.isAssignableFrom(metadata.getRepositoryInterface())) {
        JpaEntityInformation<?, Serializable> entityInformation = 
          getEntityInformation(metadata.getDomainType());
        Object customImplementation = new JpaCqlRepositoryImpl<>(entityInformation, entityManager);
        
        fragments = fragments.append(RepositoryComposition.RepositoryFragments.just(customImplementation));
      }
      
      return fragments;
    }
  }
}
