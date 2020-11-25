package org.folio.spring.config;

import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;

public class DataSourceSchemaAdvisorBeanPostProcessor implements BeanPostProcessor {
  private final FolioExecutionContext folioExecutionContext;

  public DataSourceSchemaAdvisorBeanPostProcessor(FolioExecutionContext folioExecutionContext) {
    this.folioExecutionContext = folioExecutionContext;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if ("dataSource".equals(beanName)) {
      return new DataSourceFolioWrapper((DataSource) bean, folioExecutionContext);
    } else {
      return bean;
    }
  }
}
