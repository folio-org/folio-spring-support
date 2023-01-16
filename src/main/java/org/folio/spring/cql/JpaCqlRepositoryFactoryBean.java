package org.folio.spring.cql;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class JpaCqlRepositoryFactoryBean<T extends JpaRepository<S, ID>, S, ID extends Serializable>
  extends JpaRepositoryFactoryBean<T, S, ID> {

  public JpaCqlRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
    super(repositoryInterface);
  }

  @Override
  protected RepositoryFactorySupport createRepositoryFactory(EntityManager em) {
    return new JpaCqlRepositoryFactory(em);
  }

  private static class JpaCqlRepositoryFactory extends JpaRepositoryFactory {

    public JpaCqlRepositoryFactory(EntityManager em) {
      super(em);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
      Class<?> repositoryInterface = metadata.getRepositoryInterface();
      if (JpaCqlRepository.class.isAssignableFrom(repositoryInterface)) {
        return JpaCqlRepositoryImpl.class;
      } else {
        return SimpleJpaRepository.class;
      }
    }
  }
}
