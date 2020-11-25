package org.folio.spring.config;

import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"org.folio.spring.controller", "org.folio.spring.config", "org.folio.spring.filter"})
public class FolioSpringConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public FolioModuleMetadata folioModuleMetadata(@Value("${spring.application.name}") String applicationName) {
    var schemaSuffix = StringUtils.isNotBlank(applicationName) ? "_" + applicationName.toLowerCase().replace('-', '_') : "";
    return new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return applicationName;
      }

      /**
       * Schema name MUST in in lowercase
       *
       * @param tenantId
       * @return
       */
      @Override
      public String getDBSchemaName(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
          throw new IllegalArgumentException("tenantId can't be null or empty");
        }
        return tenantId.toLowerCase() + schemaSuffix;
      }
    };
  }

  @Bean
  @Qualifier("dataSourceSchemaAdvisorBeanPostProcessor")
  public BeanPostProcessor dataSourceSchemaAdvisorBeanPostProcessor(@Autowired FolioExecutionContext folioExecutionContext) {
    return new DataSourceSchemaAdvisorBeanPostProcessor(folioExecutionContext);
  }

}
