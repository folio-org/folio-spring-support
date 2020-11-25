package org.folio.spring.scope;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static org.folio.spring.scope.FolioExecution.FOLIO_EXECUTION;

public class CustomScopeRegistryBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    beanFactory.registerScope(FOLIO_EXECUTION, new FolioExecutionScopeConfigurer());
  }

}
