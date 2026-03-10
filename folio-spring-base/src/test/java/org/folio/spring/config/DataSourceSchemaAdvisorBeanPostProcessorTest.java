package org.folio.spring.config;

import static org.folio.spring.config.DataSourceSchemaAdvisorBeanPostProcessor.DATASOURCE_BEAN_NAME;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.FactoryBean;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DataSourceSchemaAdvisorBeanPostProcessorTest {

  @InjectMocks private DataSourceSchemaAdvisorBeanPostProcessor postProcessor;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private FolioExecutionContext folioExecutionContext;

  @AfterEach
  void tearDown() {
    System.clearProperty("DB_MAX_LIFETIME");
    System.clearProperty("DB_QUERYTIMEOUT");
    System.clearProperty("DB_CONNECTIONRELEASEDELAY");
    System.clearProperty("DB_MINSHAREDPOOLSIZE");
    System.clearProperty("DB_MAXSHAREDPOOLSIZE");
    System.clearProperty("DB_CHARSET");
  }

  @Test
  void postProcessAfterInitialization_withDataSourceBeanName_shouldWrapDataSource() {
    var dataSource = mock(DataSource.class);
    var result = postProcessor.postProcessAfterInitialization(dataSource, DATASOURCE_BEAN_NAME);

    assertInstanceOf(DataSourceFolioWrapper.class, result);
  }

  @Test
  void postProcessAfterInitialization_withFactoryBean_shouldWrapDataSource() throws Exception {
    var dataSource = mock(DataSource.class);
    var factoryBean = mock(FactoryBean.class);
    when(factoryBean.getObject()).thenReturn(dataSource);

    var result = postProcessor.postProcessAfterInitialization(factoryBean, DATASOURCE_BEAN_NAME);

    assertInstanceOf(DataSourceFolioWrapper.class, result);
  }

  @Test
  void postProcessAfterInitialization_withUnknownBeanName_shouldReturnOriginalBean() {
    var bean = new Object();
    var result = postProcessor.postProcessAfterInitialization(bean, "unknownBean");

    assertSame(bean, result);
  }

  @Test
  void postProcessAfterInitialization_withFactoryBeanNotReturningDataSource_shouldThrowException() throws Exception {
    var factoryBean = mock(FactoryBean.class);
    when(factoryBean.getObject()).thenReturn(new Object());

    assertThrows(IllegalStateException.class, () ->
      postProcessor.postProcessAfterInitialization(factoryBean, DATASOURCE_BEAN_NAME));
  }

  @Test
  void postProcessAfterInitialization_withNonDataSourceBean_shouldThrowException() {
    var bean = new Object();

    assertThrows(ClassCastException.class, () ->
      postProcessor.postProcessAfterInitialization(bean, DATASOURCE_BEAN_NAME));
  }

  @Test
  void postProcessBeforeInitialization_defaultValues() {
    var bean = mock(HikariDataSource.class);
    when(folioModuleMetadata.getModuleName()).thenReturn("mod-test");

    var result = postProcessor.postProcessBeforeInitialization(bean, DATASOURCE_BEAN_NAME);
    assertSame(bean, result);

    verify(bean).setMinimumIdle(0);
    verify(bean).setMaxLifetime(1800000L);
    verify(bean).setIdleTimeout(60000L);
    verify(bean).addDataSourceProperty("ApplicationName", "mod-test");
    verify(bean).addDataSourceProperty("characterEncoding", "UTF-8");
  }

  @Test
  void postProcessBeforeInitialization_customValues() {
    System.setProperty("DB_MAX_LIFETIME", "10000");
    System.setProperty("DB_MAXSHAREDPOOLSIZE", "25");
    System.setProperty("DB_QUERYTIMEOUT", "2000");
    System.setProperty("DB_CONNECTIONRELEASEDELAY", "3000");
    System.setProperty("DB_MINSHAREDPOOLSIZE", "5");
    System.setProperty("DB_CHARSET", "Windows-1252");

    var bean = mock(HikariDataSource.class);
    when(folioModuleMetadata.getModuleName()).thenReturn("mod-test");

    var result = postProcessor.postProcessBeforeInitialization(bean, DATASOURCE_BEAN_NAME);
    assertSame(bean, result);

    verify(bean).setMaximumPoolSize(25);
    verify(bean).setMinimumIdle(5);
    verify(bean).setConnectionInitSql("SET statement_timeout = 2000");
    verify(bean).setMaxLifetime(10000L);
    verify(bean).setIdleTimeout(3000L);
    verify(bean).addDataSourceProperty("ApplicationName", "mod-test");
    verify(bean).addDataSourceProperty("characterEncoding", "Windows-1252");
  }

  @Test
  void postProcessBeforeInitialization_invalidLongValue() {
    System.setProperty("DB_MAXSHAREDPOOLSIZE", "test");

    var bean = mock(HikariDataSource.class);
    when(folioModuleMetadata.getModuleName()).thenReturn("mod-test");

    var result = postProcessor.postProcessBeforeInitialization(bean, DATASOURCE_BEAN_NAME);
    assertSame(bean, result);

    verify(bean, never()).setMaximumPoolSize(anyInt());
    verify(bean).setMinimumIdle(0);
    verify(bean).setMaxLifetime(1800000L);
    verify(bean).setIdleTimeout(60000L);
    verify(bean).addDataSourceProperty("ApplicationName", "mod-test");
    verify(bean).addDataSourceProperty("characterEncoding", "UTF-8");
  }

  @Test
  void postProcessBeforeInitialization_invalidBean() {
    var bean = mock(DataSource.class);

    var result = postProcessor.postProcessBeforeInitialization(bean, DATASOURCE_BEAN_NAME);
    assertSame(bean, result);
    verifyNoInteractions(bean);
  }
}
