package org.folio.spring.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.ThreadContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests thread-safety of logging context to ensure no cross-contamination between concurrent requests.
 * This test verifies the fix for context corruption in multithreaded scenarios.
 */
@UnitTest
class FolioLoggingContextMultithreadingTest {

  private final FolioLoggingContextLookup contextLookup = new FolioLoggingContextLookup();

  @AfterEach
  void tearDown() {
    ThreadContext.clearAll();
  }

  @Test
  void shouldIsolateContextBetweenConcurrentThreads() throws InterruptedException {
    int threadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    List<String> contextErrors = Collections.synchronizedList(new ArrayList<>());
    AtomicInteger successCount = new AtomicInteger(0);

    for (int i = 0; i < threadCount; i++) {
      int threadId = i;
      executor.submit(() -> {
        runContextTest(threadId, startLatch, doneLatch, contextErrors, successCount);
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);

    assertTrue(completed, "All threads should complete within timeout");
    assertEquals(threadCount, successCount.get(), "All threads should succeed");
    assertTrue(contextErrors.isEmpty(),
      "No context corruption should occur. Errors: " + String.join("; ", contextErrors));
  }

  @Test
  void shouldRestorePreviousContextAfterNesting() {
    String outerTenant = "outer-tenant";
    String innerTenant = "inner-tenant";
    UUID outerUserId = UUID.randomUUID();
    UUID innerUserId = UUID.randomUUID();

    FolioLoggingContextHolder.putFolioExecutionContext(
      createMockContext(outerTenant, outerUserId.toString(), "outer-req", "outer-mod"));
    assertEquals(outerTenant, contextLookup.lookup("tenantId"));

    FolioLoggingContextHolder.putFolioExecutionContext(
      createMockContext(innerTenant, innerUserId.toString(), "inner-req", "inner-mod"));
    assertEquals(innerTenant, contextLookup.lookup("tenantId"));

    FolioLoggingContextHolder.removeFolioExecutionContext(
      createMockContext(outerTenant, outerUserId.toString(), "outer-req", "outer-mod"));
    assertEquals(outerTenant, contextLookup.lookup("tenantId"));

    FolioLoggingContextHolder.removeFolioExecutionContext(null);
    assertEquals("", contextLookup.lookup("tenantId"));
  }

  @Test
  void shouldHandleRapidContextSwitching() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(2);
    List<String> errors = Collections.synchronizedList(new ArrayList<>());

    Thread thread1 = new Thread(() ->
      rapidContextSwitch("tenant-A", latch, errors));
    Thread thread2 = new Thread(() ->
      rapidContextSwitch("tenant-B", latch, errors));

    thread1.start();
    thread2.start();

    latch.await(5, TimeUnit.SECONDS);

    assertTrue(errors.isEmpty(), "Rapid switching should not cause corruption: " + errors);
  }

  private void runContextTest(int threadId, CountDownLatch startLatch, CountDownLatch doneLatch,
                               List<String> errors, AtomicInteger successCount) {
    try {
      startLatch.await();
      String expectedTenant = "tenant-" + threadId;
      String expectedUser = UUID.randomUUID().toString();
      String expectedRequest = "request-" + threadId;
      String expectedModule = "module-" + threadId;

      FolioLoggingContextHolder.putFolioExecutionContext(
        createMockContext(expectedTenant, expectedUser, expectedRequest, expectedModule));

      for (int i = 0; i < 50; i++) {
        Thread.sleep(1);
        validateContext(threadId, expectedTenant, expectedUser, expectedRequest, expectedModule, errors);
      }

      FolioLoggingContextHolder.removeFolioExecutionContext(null);
      successCount.incrementAndGet();
    } catch (Exception e) {
      errors.add("Thread " + threadId + " exception: " + e.getMessage());
    } finally {
      doneLatch.countDown();
    }
  }

  private void validateContext(int threadId, String expectedTenant, String expectedUser,
                                String expectedRequest, String expectedModule, List<String> errors) {
    final String actualTenant = contextLookup.lookup("tenantId");
    final String actualUser = contextLookup.lookup("userId");
    final String actualRequest = contextLookup.lookup("requestId");
    final String actualModule = contextLookup.lookup("moduleId");

    if (!expectedTenant.equals(actualTenant)) {
      errors.add(String.format("Thread %d: tenant mismatch - expected '%s' but got '%s'",
        threadId, expectedTenant, actualTenant));
    }
    if (!expectedUser.equals(actualUser)) {
      errors.add(String.format("Thread %d: user mismatch - expected '%s' but got '%s'",
        threadId, expectedUser, actualUser));
    }
    if (!expectedRequest.equals(actualRequest)) {
      errors.add(String.format("Thread %d: request mismatch - expected '%s' but got '%s'",
        threadId, expectedRequest, actualRequest));
    }
    if (!expectedModule.equals(actualModule)) {
      errors.add(String.format("Thread %d: module mismatch - expected '%s' but got '%s'",
        threadId, expectedModule, actualModule));
    }
  }

  private void rapidContextSwitch(String tenantPrefix, CountDownLatch latch, List<String> errors) {
    try {
      for (int i = 0; i < 100; i++) {
        String tenant = tenantPrefix + "-" + i;
        String userId = UUID.randomUUID().toString();
        FolioLoggingContextHolder.putFolioExecutionContext(
          createMockContext(tenant, userId, "req-" + i, "mod-" + i));

        String actual = contextLookup.lookup("tenantId");
        if (!tenant.equals(actual)) {
          errors.add(String.format("Rapid switch: expected '%s' but got '%s'", tenant, actual));
        }

        FolioLoggingContextHolder.removeFolioExecutionContext(null);
      }
    } catch (Exception e) {
      errors.add("Rapid switch exception: " + e.getMessage());
    } finally {
      latch.countDown();
    }
  }

  private FolioExecutionContext createMockContext(String tenantId, String userId,
                                                   String requestId, String moduleId) {
    return new FolioExecutionContext() {
      @Override
      public String getTenantId() {
        return tenantId;
      }

      @Override
      public UUID getUserId() {
        return UUID.fromString(userId);
      }

      @Override
      public String getRequestId() {
        return requestId;
      }

      @Override
      public FolioModuleMetadata getFolioModuleMetadata() {
        return new FolioModuleMetadata() {
          @Override
          public String getModuleName() {
            return moduleId;
          }

          @Override
          public String getDBSchemaName(String tenantId) {
            return null;
          }
        };
      }
    };
  }
}
