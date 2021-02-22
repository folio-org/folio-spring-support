package org.folio.spring.service;

import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.endFolioExecutionContext;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.folio.spring.AsyncFolioExecutionContext;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;

@AllArgsConstructor
public class TenantScopedExecutionService {
  private final FolioModuleMetadata moduleMetadata;
  private final Optional<SystemUserService> systemUserServiceOptional;

  /**
   * Executes given job tenant scoped. If a system user is configured, then
   * it is used for as {@link FolioExecutionContext} implementation, otherwise
   * an {@link AsyncFolioExecutionContext} is used.
   *
   * @param tenantId - The tenant name.
   * @param job      - Job to be executed in tenant scope.
   * @param <T>      - Optional return value for the job.
   * @return Result of job.
   * @throws RuntimeException - Wrapped exception from the job.
   */
  @SneakyThrows
  public <T> T executeTenantScoped(String tenantId, ThrowableSupplier<T> job) {
    try {
      beginFolioExecutionContext(folioExecutionContext(tenantId));
      return job.get();
    } finally {
      endFolioExecutionContext();
    }
  }

  private FolioExecutionContext folioExecutionContext(String tenantId) {
    return systemUserServiceOptional.isPresent()
      ? DefaultFolioExecutionContext.forSystemUser(moduleMetadata,
      systemUserServiceOptional.get().getSystemUserParameters(tenantId))
      : new AsyncFolioExecutionContext(tenantId, moduleMetadata);
  }

  @FunctionalInterface
  public interface ThrowableSupplier<T> {
    T get() throws Exception;
  }
}
