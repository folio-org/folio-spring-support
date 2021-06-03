package org.folio.spring.cql;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
  basePackages = "${folio.jpa.repository.base-packages:org.folio.*}",
  repositoryFactoryBeanClass = JpaCqlRepositoryFactoryBean.class
)
public class JpaCqlConfiguration {

}
