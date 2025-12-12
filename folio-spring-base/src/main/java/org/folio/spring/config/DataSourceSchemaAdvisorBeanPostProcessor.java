package org.folio.spring.config;

import static org.folio.spring.config.properties.FolioDatabaseEnvs.DB_CHARSET;
import static org.folio.spring.config.properties.FolioDatabaseEnvs.DB_CONNECTIONRELEASEDELAY;
import static org.folio.spring.config.properties.FolioDatabaseEnvs.DB_MAXPOOLSIZE;
import static org.folio.spring.config.properties.FolioDatabaseEnvs.DB_MAX_LIFETIME;
import static org.folio.spring.config.properties.FolioDatabaseEnvs.DB_MIN_IDLE_CONNECTIONS;
import static org.folio.spring.config.properties.FolioDatabaseEnvs.DB_QUERYTIMEOUT;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.jspecify.annotations.NonNull;
import org.folio.spring.FolioModuleMetadata;
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
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (bean instanceof HikariDataSource hikariDataSource) {
      DB_MAX_LIFETIME.findLong().ifPresent(maxLifeTime -> {
        log.info("Setting HikariCP max lifetime to: {} ms", maxLifeTime);
        hikariDataSource.setMaxLifetime(maxLifeTime);
      });

      DB_MAXPOOLSIZE.findLong().ifPresent(maxPoolSize -> {
        log.info("Setting HikariCP maximum pool size to: {}", maxPoolSize);
        hikariDataSource.setMaximumPoolSize(maxPoolSize.intValue());
      });

      DB_QUERYTIMEOUT.findLong().ifPresent(queryTimeout -> {
        log.info("Setting HikariCP statement timeout to: {} ms", queryTimeout);
        hikariDataSource.setConnectionInitSql(String.format("SET statement_timeout = %d", queryTimeout));
      });

      DB_CONNECTIONRELEASEDELAY.findLong().ifPresent(idleTimeout -> {
        log.info("Setting HikariCP idle timeout to: {} ms", idleTimeout);
        hikariDataSource.setIdleTimeout(idleTimeout);
      });

      DB_MIN_IDLE_CONNECTIONS.findLong().ifPresent(minIdleConnection -> {
        log.info("Setting HikariCP minimum idle connections to: {}", minIdleConnection);
        hikariDataSource.setMinimumIdle(minIdleConnection.intValue());
      });

      DB_CHARSET.findString().ifPresent(dbCharset -> {
        log.info("Setting HikariCP character encoding to: {}", dbCharset);
        hikariDataSource.addDataSourceProperty("characterEncoding", dbCharset);
      });

      log.info("Setting HikariCP ApplicationName to: {}", moduleMetadata.getModuleName());
      hikariDataSource.addDataSourceProperty("ApplicationName", moduleMetadata.getModuleName());
    }

    return bean;
  }

  private IllegalStateException unknownDatasourceException() {
    return new IllegalStateException(
      "Bean with dataSource name should be instance of DataSource or FactoryBean");
  }
}
