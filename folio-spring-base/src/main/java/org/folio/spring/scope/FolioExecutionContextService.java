package org.folio.spring.scope;

import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.SneakyThrows;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.exception.FolioContextExecutionException;
import org.folio.spring.integration.XOkapiHeaders;

/**
 * Service for executing code within a Folio context, setting up tenant and headers.
 */
public class FolioExecutionContextService {

  private final FolioModuleMetadata moduleMetadata;

  /**
   * Constructs a FolioContextExecutionService with the given module metadata.
   *
   * @param moduleMetadata the module metadata to use for context setup
   */
  public FolioExecutionContextService(FolioModuleMetadata moduleMetadata) {
    this.moduleMetadata = moduleMetadata;
  }

  /**
   * Executes the given action within a Folio context for the specified tenant and headers.
   *
   * @param tenantId the tenant identifier
   * @param headers the headers to set in the context
   * @param action the action to execute
   * @param <T> the return type of the action
   * @return the result of the action
   * @throws FolioContextExecutionException if execution fails
   */
  public <T> T execute(String tenantId, Map<String, Collection<String>> headers, Callable<T> action) {
    Map<String, Collection<String>> allHeaders = headers == null ? new HashMap<>() : new HashMap<>(headers);
    allHeaders.put(XOkapiHeaders.TENANT, singleton(tenantId));
    try (var fex = new FolioExecutionContextSetter(moduleMetadata, allHeaders)) {
      return action.call();
    } catch (Exception e) {
      throw new FolioContextExecutionException("Execution for tenant = %s failed.".formatted(tenantId), e);
    }
  }

  /**
   * Executes the given action within a Folio context for the specified tenant and execution context.
   *
   * @param tenantId the tenant identifier
   * @param context the Folio execution context to extract headers from and set in the context
   * @param action the action to execute
   * @param <T> the return type of the action
   * @return the result of the action
   * @throws FolioContextExecutionException if execution fails
   */
  @SneakyThrows
  public <T> T execute(String tenantId, FolioExecutionContext context, Callable<T> action) {
    return execute(tenantId, context.getAllHeaders(), action);
  }

  /**
   * Executes the given runnable action within a Folio context for the specified tenant and headers.
   *
   * @param tenantId the tenant identifier
   * @param headers the headers to set in the context
   * @param action the runnable action to execute
   * @throws FolioContextExecutionException if execution fails
   */
  public void execute(String tenantId, Map<String, Collection<String>> headers, Runnable action) {
    execute(tenantId, headers, () -> {
      action.run();
      return null;
    });
  }

  /**
   * Executes the given runnable action within a Folio context for the specified tenant and execution context.
   *
   * @param tenantId the tenant identifier
   * @param context the Folio execution context to extract headers from and set in the context
   * @param action the runnable action to execute
   * @throws FolioContextExecutionException if execution fails
   */
  public void execute(String tenantId, FolioExecutionContext context, Runnable action) {
    execute(tenantId, context, () -> {
      action.run();
      return null;
    });
  }
}
