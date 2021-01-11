package org.folio.spring.config;

import javax.sql.DataSource;
import org.folio.spring.FolioExecutionContext;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class DataSourceSchemaAdvisorBeanPostProcessor implements BeanPostProcessor {

  private final FolioExecutionContext folioExecutionContext;

  public static final String DATASOURCE_BEAN_NAME = "dataSource";

  public DataSourceSchemaAdvisorBeanPostProcessor(FolioExecutionContext folioExecutionContext) {
    this.folioExecutionContext = folioExecutionContext;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (DATASOURCE_BEAN_NAME.equals(beanName)) {
      DataSource dataSource;
      if (bean instanceof ScopedProxyFactoryBean) {
        if (((ScopedProxyFactoryBean) bean).getObject() instanceof DataSource) {
          dataSource = (DataSource) ((ScopedProxyFactoryBean) bean).getObject();
        } else {
          throw new IllegalStateException(
            "Bean with dataSource name should be instance of DataSource");
        }
      } else {
        dataSource = (DataSource) bean;
      }
      return new DataSourceFolioWrapper(dataSource, folioExecutionContext);
    } else {
      return bean;
    }
  }
}
