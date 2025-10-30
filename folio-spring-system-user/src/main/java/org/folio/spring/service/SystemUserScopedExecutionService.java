package org.folio.spring.service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Deprecated(since = "10.0.0", forRemoval = true)
@RequiredArgsConstructor
public class SystemUserScopedExecutionService {

  private final FolioExecutionContext executionContext;
  private final ExecutionContextBuilder contextBuilder;
  private SystemUserService systemUserService;

  /**
   * Executes given action in scope of system user.
   *
   * @param tenantId - The tenant name.
   * @param action   - Job to be executed in tenant scope.
   * @param <T>      - Optional return value for the action.
   * @return Result of action.
   * @throws RuntimeException - Wrapped exception from the action.
   */
  @SneakyThrows
  public <T> T executeSystemUserScoped(String tenantId, Callable<T> action) {
    try (var fex = new FolioExecutionContextSetter(folioExecutionContext(tenantId))) {
      return action.call();
    }
  }

  /**
   * Executes given action in scope of system user.
   *
   * @param tenantId - The tenant name.
   * @param headers  - Headers to be set in the context.
   * @param action   - Job to be executed in tenant scope.
   * @param <T>      - Optional return value for the action.
   * @return Result of action.
   * @throws RuntimeException - Wrapped exception from the action.
   */
  @SneakyThrows
  public <T> T executeSystemUserScoped(String tenantId, Map<String, Collection<String>> headers, Callable<T> action) {
    try (var fex = new FolioExecutionContextSetter(folioExecutionContext(tenantId, headers))) {
      return action.call();
    }
  }

  /**
   * Executes given action in scope of system user.
   *
   * @param action - Job to be executed in tenant scope.
   * @param <T>    - Optional return value for the action.
   * @return Result of action.
   * @throws RuntimeException - Wrapped exception from the action.
   */
  @SneakyThrows
  public <T> T executeSystemUserScoped(Callable<T> action) {
    try (var fex = new FolioExecutionContextSetter(folioExecutionContext(executionContext.getTenantId()))) {
      return action.call();
    }
  }

  /**
   * Executes given job in scope of system user asynchronously.
   *
   * @param tenantId - The tenant name.
   * @param job      - Job to be executed in tenant scope.
   */
  @Async
  public void executeAsyncSystemUserScoped(String tenantId, Runnable job) {
    try (var fex = new FolioExecutionContextSetter(folioExecutionContext(tenantId))) {
      job.run();
    }
  }

  private FolioExecutionContext folioExecutionContext(String tenantId) {
    return folioExecutionContext(tenantId, null);
  }

  /**
   * Build execution context for system user of given tenant if system user service is available.
   * Otherwise, build execution context using provided tenantId, headers.
   * Passes userId from current execution context in case it's missing in provided headers.
   * */
  private FolioExecutionContext folioExecutionContext(String tenantId, Map<String, Collection<String>> headers) {
    if (systemUserService == null) {
      return contextBuilder.buildContext(tenantId, executionContext.getUserId(), headers);
    }
    return contextBuilder.forSystemUser(systemUserService.getAuthedSystemUser(tenantId),
                  () -> systemUserService.getAuthedSystemUser(tenantId));
  }

  @Autowired(required = false)
  public void setSystemUserService(SystemUserService systemUserService) {
    this.systemUserService = systemUserService;
  }
}
