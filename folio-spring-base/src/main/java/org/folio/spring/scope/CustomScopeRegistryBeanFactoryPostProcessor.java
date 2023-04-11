package org.folio.spring.scope;

import static org.folio.spring.scope.FolioExecution.FOLIO_EXECUTION;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class CustomScopeRegistryBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    beanFactory.registerScope(FOLIO_EXECUTION, new FolioExecutionScopeConfigurer());
  }

}
