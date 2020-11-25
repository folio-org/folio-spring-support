package org.folio.spring.scope;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.filter.FolioExecutionScopeFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

import static org.folio.spring.scope.FolioExecution.FOLIO_EXECUTION;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getFolioExecutionContext;

@Configuration
public class FolioExecutionScopeConfig {
  private final EmptyFolioExecutionContextHolder emptyFolioExecutionContextHolder;

  @Autowired
  public FolioExecutionScopeConfig(EmptyFolioExecutionContextHolder emptyFolioExecutionContextHolder) {
    this.emptyFolioExecutionContextHolder = emptyFolioExecutionContextHolder;
  }

  @Bean
  public static CustomScopeRegistryBeanFactoryPostProcessor customScopeRegistryBeanFactoryPostProcessor() {
    return new CustomScopeRegistryBeanFactoryPostProcessor();
  }

  @Bean
  public static EmptyFolioExecutionContextHolder emptyFolioExecutionContext(@Autowired FolioModuleMetadata folioModuleMetadata) {
    return new EmptyFolioExecutionContextHolder(folioModuleMetadata);
  }

  @Bean
  public FolioExecutionScopeFilter folioExecutionScopeFilter(@Autowired FolioModuleMetadata folioModuleMetadata) {
    return new FolioExecutionScopeFilter(folioModuleMetadata);
  }

  @Bean
  @Scope(value = FOLIO_EXECUTION, proxyMode = ScopedProxyMode.INTERFACES)
  public FolioExecutionContext folioExecutionContext() {
    var folioExecutionContext = getFolioExecutionContext();
    return folioExecutionContext != null ? folioExecutionContext : emptyFolioExecutionContextHolder.getEmptyFolioExecutionContext();
  }

}
