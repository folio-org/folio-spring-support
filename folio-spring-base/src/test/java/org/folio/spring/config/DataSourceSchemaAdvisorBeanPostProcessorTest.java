package org.folio.spring.config;

import static org.folio.spring.config.DataSourceSchemaAdvisorBeanPostProcessor.DATASOURCE_BEAN_NAME;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.FactoryBean;

@UnitTest
class DataSourceSchemaAdvisorBeanPostProcessorTest {

  private DataSourceSchemaAdvisorBeanPostProcessor postProcessor;

  @BeforeEach
  void setUp() {
    var folioExecutionContext = mock(FolioExecutionContext.class);
    postProcessor = new DataSourceSchemaAdvisorBeanPostProcessor(folioExecutionContext);
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
}
