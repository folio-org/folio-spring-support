package org.folio.spring.testing.extension.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import lombok.SneakyThrows;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.extension.DatabaseCleanup;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.TestContextManager;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DatabaseCleanupExtensionTest {

  private @Mock ApplicationContext applicationContext;
  private @Mock FolioModuleMetadata folioModuleMetadata;
  private @Mock JdbcTemplate jdbcTemplate;
  private @Captor ArgumentCaptor<String> captor;

  @SneakyThrows
  @Test
  void testDeletingDataFromTables() {
    ExtensionContext context = mock(ExtensionContext.class);
    ExtensionContext.Store store = mock(ExtensionContext.Store.class);
    TestContextBootstrapper testContextBootstrapper = mock(TestContextBootstrapper.class);
    TestContext testContext = new StubTestContext();
    when(testContextBootstrapper.buildTestContext()).thenReturn(testContext);
    TestContextManager testContextManager = new TestContextManager(testContextBootstrapper);
    when(context.getRoot()).thenReturn(context);
    when(context.getStore(any())).thenReturn(store);
    when(context.getRequiredTestClass()).thenAnswer(i -> TestClass.class);
    when(context.getRequiredTestMethod()).thenReturn(TestClass.class.getDeclaredMethod("testMethod"));
    when(store.getOrComputeIfAbsent(any(), any(), any())).thenReturn(testContextManager);
    when(applicationContext.getBean(JdbcTemplate.class)).thenReturn(jdbcTemplate);
    when(applicationContext.getBean(FolioModuleMetadata.class)).thenReturn(folioModuleMetadata);
    when(folioModuleMetadata.getModuleName()).thenReturn("mod-test");
    when(folioModuleMetadata.getDBSchemaName(anyString())).thenAnswer(in -> in.getArgument(0) + "-mod-test");
    when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of("tenant1", "tenant2"));
    doNothing().when(jdbcTemplate).execute(captor.capture());

    new DatabaseCleanupExtension().afterEach(context);

    List<String> allValues = captor.getAllValues();
    assertThat(allValues).containsExactly(
      "delete from tenant1-mod-test.table1;",
      "delete from tenant1-mod-test.table2;",
      "delete from tenant2-mod-test.table1;",
      "delete from tenant2-mod-test.table2;"
    );
  }

  static class TestClass {

    @DatabaseCleanup(tables = {"table1", "table2"})
    void testMethod() {

    }
  }

  class StubTestContext implements TestContext {

    @Override
    public ApplicationContext getApplicationContext() {
      return applicationContext;
    }

    @Override
    public Class<?> getTestClass() {
      return TestClass.class;
    }

    @Override
    public Object getTestInstance() {
      return new TestClass();
    }

    @SneakyThrows
    @Override
    public Method getTestMethod() {
      return TestClass.class.getDeclaredMethod("testMethod");
    }

    @Override
    public Throwable getTestException() {
      return null;
    }

    @Override
    public void markApplicationContextDirty(DirtiesContext.HierarchyMode hierarchyMode) {

    }

    @Override
    public void updateState(Object testInstance, Method testMethod, Throwable testException) {

    }

    @Override
    public void setAttribute(String name, Object value) {

    }

    @Override
    public Object getAttribute(String name) {
      return null;
    }

    @Override
    public Object removeAttribute(String name) {
      return null;
    }

    @Override
    public boolean hasAttribute(String name) {
      return false;
    }

    @Override
    public String[] attributeNames() {
      return new String[0];
    }
  }
}
