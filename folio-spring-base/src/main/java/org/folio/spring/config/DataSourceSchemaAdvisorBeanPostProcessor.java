package org.folio.spring.config;

import javax.sql.DataSource;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

@Log4j2
public class DataSourceSchemaAdvisorBeanPostProcessor implements BeanPostProcessor {

  public static final String DATASOURCE_BEAN_NAME = "dataSource";

  private final FolioExecutionContext folioExecutionContext;

  public DataSourceSchemaAdvisorBeanPostProcessor(FolioExecutionContext folioExecutionContext) {
    this.folioExecutionContext = folioExecutionContext;
  }

  @Override
  public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
    if (DATASOURCE_BEAN_NAME.equals(beanName)) {
      DataSource dataSource;
      if (bean instanceof FactoryBean<?> factoryBean) {
        try {
          if (factoryBean.getObject() instanceof DataSource dataSourceObject) {
            dataSource = dataSourceObject;
          } else {
            throw unknownDatasourceException();
          }
        } catch (Exception e) {
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
