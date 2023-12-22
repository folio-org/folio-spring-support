package org.folio.spring.scope;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@UnitTest
@SpringBootTest(classes = {
  FolioExecutionScopeExecutionContextManagerTest.TestConfiguration.class,
  FolioExecutionScopeConfig.class},
  properties = {
    "spring.application.name=TestFolioSpringBaseApplication"
  })
class FolioExecutionScopeExecutionContextManagerTest {
  @Autowired
  private FolioExecutionContext folioExecutionContext;

  @Autowired
  private FolioModuleMetadata folioModuleMetadata;

  @Test
  void getRunnableWithFolioContext() {
    Collection<String> headerValueCollection = List.of("dummy-tenanant-1");
    var allHeaders = Map.of(TENANT, headerValueCollection);
    var localFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, allHeaders);
    var task = FolioExecutionScopeExecutionContextManager.getRunnableWithFolioContext(localFolioExecutionContext,
      () -> {
        var tenantId = folioExecutionContext.getTenantId();
        var localTenantId = localFolioExecutionContext.getTenantId();
        assertEquals(tenantId, localTenantId);
      });

    task.run();
  }

  @Test
  void getRunnableWithCurrentFolioContext() {
    Collection<String> headerValueCollection = List.of("dummy-tenant-2");
    var allHeaders = Map.of(TENANT, headerValueCollection);
    var localFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, allHeaders);
    Runnable task;
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(localFolioExecutionContext);
    try {
      task = FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(() -> {
        var tenantId = folioExecutionContext.getTenantId();
        var localTenantId = localFolioExecutionContext.getTenantId();
        assertEquals(tenantId, localTenantId);
        var instance = folioExecutionContext.getInstance();
        assertEquals(localFolioExecutionContext, instance);
      });
    } finally {
      FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
    }
    task.run();
  }

  @Test
  void getRunnableWithCurrentFolioContextWithStack() {
    Collection<String> headerValueCollection = List.of("dummy-tenant-3");
    var allHeaders = Map.of(TENANT, headerValueCollection);
    var localFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, allHeaders);
    Runnable task;
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(localFolioExecutionContext);
    try {
      task = FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext(() -> {
        // push & pop for FolioContext
        getRunnableWithFolioContext();

        var tenantId = folioExecutionContext.getTenantId();
        var localTenantId = localFolioExecutionContext.getTenantId();
        assertEquals(tenantId, localTenantId);
        var instance = folioExecutionContext.getInstance();
        assertEquals(localFolioExecutionContext, instance);
      });
    } finally {
      FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
    }
    task.run();
  }

  @Test
  void testFolioExecutionContextChildValue() {
    FolioExecutionScopeExecutionContextManager.FolioExecutionContextThreadLocal testFolioExecutionContextHolder =
      new FolioExecutionScopeExecutionContextManager.FolioExecutionContextThreadLocal("FolioExecutionContext");
    FolioExecutionContext fec = new DefaultFolioExecutionContext(null, Collections.emptyMap());
    testFolioExecutionContextHolder.get().push(fec);
    Deque<FolioExecutionContext> parent = testFolioExecutionContextHolder.get();
    Deque<FolioExecutionContext> child = testFolioExecutionContextHolder.childValue(parent);

    assertNotEquals(parent, child);
    assertEquals(parent.peek().getInstance(), child.peek().getInstance());
  }

  @Test
  void testFolioExecutionScopeChildValue() {
    FolioExecutionScopeExecutionContextManager.FolioExecutionScopeThreadLocal testFolioExecutionScopeHolder
      = new FolioExecutionScopeExecutionContextManager.FolioExecutionScopeThreadLocal("FolioExecutionScope");
    Map<String, Object> parentScope = new ConcurrentHashMap<>();
    parentScope.put("key1", "value1");
    parentScope.put("key2", "value2");
    testFolioExecutionScopeHolder.get().push(parentScope);
    Deque<Map<String, Object>> parent = testFolioExecutionScopeHolder.get();
    Deque<Map<String, Object>> child = testFolioExecutionScopeHolder.childValue(parent);

    assertNotEquals(parent, child);
    assertEquals(parent.peek(), child.peek());
  }

  @Configuration
  static class TestConfiguration {
    @Bean
    public FolioModuleMetadata folioModuleMetadata(@Value("${spring.application.name}") String applicationName) {
      var schemaSuffix = StringUtils.isNotBlank(applicationName)
        ? "_" + applicationName.toLowerCase().replace('-', '_') : "";

      return new FolioModuleMetadata() {
        @Override
        public String getModuleName() {
          return applicationName;
        }

        @Override
        public String getDBSchemaName(String tenantId) {
          if (StringUtils.isBlank(tenantId)) {
            throw new IllegalArgumentException("tenantId can't be null or empty");
          }
          return tenantId.toLowerCase() + schemaSuffix;
        }
      };
    }
  }
}
