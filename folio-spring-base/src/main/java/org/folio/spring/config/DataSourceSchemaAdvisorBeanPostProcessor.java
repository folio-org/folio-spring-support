package org.folio.spring.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.config.properties.FolioDatabaseEnvs;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

@Log4j2
public class DataSourceSchemaAdvisorBeanPostProcessor implements BeanPostProcessor {

  public static final String DATASOURCE_BEAN_NAME = "dataSource";

  private final FolioModuleMetadata moduleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  public DataSourceSchemaAdvisorBeanPostProcessor(FolioExecutionContext folioExecutionContext,
    FolioModuleMetadata moduleMetadata) {
    this.moduleMetadata = moduleMetadata;
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

  @Override
  public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
    if (bean instanceof HikariDataSource hikariDataSource) {
      FolioDatabaseEnvs.DB_MAX_LIFETIME.findLong().ifPresent(maxLifeTime -> {
        log.info("HikariCP:: max lifetime set to: {} ms", maxLifeTime);
        hikariDataSource.setMaxLifetime(maxLifeTime);
      });

      FolioDatabaseEnvs.DB_MAXSHAREDPOOLSIZE.findLong().ifPresent(sharedPoolSize -> {
        log.info("HikariCP:: maximum pool size set to: {}", sharedPoolSize);
        hikariDataSource.setMaximumPoolSize(sharedPoolSize.intValue());
      });

      FolioDatabaseEnvs.DB_QUERYTIMEOUT.findLong().ifPresent(queryTimeout -> {
        log.info("HikariCP:: statement timeout set to: {} ms", queryTimeout);
        hikariDataSource.setConnectionInitSql(String.format("SET statement_timeout = %d", queryTimeout));
      });

      FolioDatabaseEnvs.DB_CONNECTIONRELEASEDELAY.findLong().ifPresent(idleTimeout -> {
        log.info("HikariCP:: idle timeout set to: {} ms", idleTimeout);
        hikariDataSource.setIdleTimeout(idleTimeout);
      });

      FolioDatabaseEnvs.DB_MINSHAREDPOOLSIZE.findLong().ifPresent(minIdleConnection -> {
        log.info("HikariCP:: minimum idle set to: {}", minIdleConnection);
        hikariDataSource.setMinimumIdle(minIdleConnection.intValue());
      });

      FolioDatabaseEnvs.DB_CHARSET.findString().ifPresent(dbCharset -> {
        log.info("HikariCP:: character encoding set to: {}", dbCharset);
        hikariDataSource.addDataSourceProperty("characterEncoding", dbCharset);
      });

      log.info("HikariCP:: ApplicationName set to: {}", moduleMetadata.getModuleName());
      hikariDataSource.addDataSourceProperty("ApplicationName", moduleMetadata.getModuleName());
    }

    return bean;
  }

  private IllegalStateException unknownDatasourceException() {
    return new IllegalStateException(
      "Bean with dataSource name should be instance of DataSource or FactoryBean");
  }
}
