package org.folio.spring.service;

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
    return systemUserService != null
      ? contextBuilder.forSystemUser(systemUserService.getAuthedSystemUser(tenantId), () -> systemUserService.getAuthedSystemUser(tenantId))
      : contextBuilder.buildContext(tenantId);
  }

  @Autowired(required = false)
  public void setSystemUserService(SystemUserService systemUserService) {
    this.systemUserService = systemUserService;
  }
}
