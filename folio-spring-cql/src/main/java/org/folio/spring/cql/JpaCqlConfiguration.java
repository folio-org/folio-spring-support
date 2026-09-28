package org.folio.spring.cql;

import org.folio.spring.cql.nativex.JpaCqlRuntimeHints;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(
  basePackages = "${folio.jpa.repository.base-packages:org.folio}",
  repositoryFactoryBeanClass = JpaCqlRepositoryFactoryBean.class
)
@ImportRuntimeHints(JpaCqlRuntimeHints.class)
public class JpaCqlConfiguration {

}
