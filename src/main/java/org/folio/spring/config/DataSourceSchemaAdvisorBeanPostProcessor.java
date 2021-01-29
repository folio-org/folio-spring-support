package org.folio.spring.config;

import javax.sql.DataSource;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

@Log4j2
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
      if (bean instanceof FactoryBean) {
        if (((ScopedProxyFactoryBean) bean).getObject() instanceof DataSource) {
          try {
            dataSource = (DataSource) ((FactoryBean) bean).getObject();
          } catch (Exception e) {
            throw unknownDatasourceException();
          }
        } else {
          throw unknownDatasourceException();
        }
      } else {
        dataSource = (DataSource) bean;
      }
      return new DataSourceFolioWrapper(dataSource, folioExecutionContext);
    } else {
      return bean;
    }
  }

  private IllegalStateException unknownDatasourceException() {
    return new IllegalStateException(
      "Bean with dataSource name should be instance of DataSource or FactoryBean");
  }

}
