package org.folio.spring.liquibase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.liquibase", name = "enabled", matchIfMissing = true)
@AutoConfigureBefore({LiquibaseAutoConfiguration.class, LiquibaseAutoConfiguration.LiquibaseConfiguration.class})
@AutoConfigureAfter({DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@EnableConfigurationProperties(LiquibaseProperties.class)
public class FolioLiquibaseConfiguration {
  private final LiquibaseProperties properties;

  public FolioLiquibaseConfiguration(LiquibaseProperties properties) {
    this.properties = properties;
  }


  @Bean
  public FolioSpringLiquibase liquibase(@Autowired DataSource dataSource) {

    FolioSpringLiquibase liquibase = new FolioSpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(this.properties.getChangeLog());
    liquibase.setClearCheckSums(this.properties.isClearChecksums());
    liquibase.setContexts(this.properties.getContexts());
    liquibase.setDefaultSchema(this.properties.getDefaultSchema());
    liquibase.setLiquibaseSchema(this.properties.getLiquibaseSchema());
    liquibase.setLiquibaseTablespace(this.properties.getLiquibaseTablespace());
    liquibase.setDatabaseChangeLogTable(this.properties.getDatabaseChangeLogTable());
    liquibase.setDatabaseChangeLogLockTable(this.properties.getDatabaseChangeLogLockTable());
    liquibase.setDropFirst(this.properties.isDropFirst());
    liquibase.setShouldRun(this.properties.isEnabled());
    liquibase.setLabels(this.properties.getLabels());
    liquibase.setChangeLogParameters(this.properties.getParameters());
    liquibase.setRollbackFile(this.properties.getRollbackFile());
    liquibase.setTestRollbackOnUpdate(this.properties.isTestRollbackOnUpdate());
    liquibase.setTag(this.properties.getTag());
    return liquibase;
  }

}
