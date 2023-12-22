package org.folio.spring.scope;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.utils.RequestUtils;

/**
 * Stores a {@link FolioExecutionContext} in a {@link ThreadLocal} variable and removes it on {@link #close()}.
 *
 * <p>Implementing {@link AutoCloseable} triggers a
 * <a href="https://sonarcloud.io/organizations/folio-org/rules?q=autoclosable&open=java%3AS2095">
 * blocking bug report in Sonar</a> on missing {@link #close()}.
 */
public class FolioExecutionContextSetter implements AutoCloseable {
  /**
   * Stores the {@link FolioExecutionContext} in a {@link ThreadLocal} variable.
   *
   * <p>Call {@link #close()} after use to remove it from the {@link ThreadLocal} variable so
   * that any later usage of the thread cannot accidently use a wrong {@link FolioExecutionContext}
   * and unit tests fail if storing {@link FolioExecutionContext} is forgotten.
   *
   * <p>Best practice is try-with-resources that automatically calls {@link #close()} in all cases, even
   * on thrown exception:
   *
   * <pre>
   * try (var x = new FolioExecutionContextSetter(currentFolioExecutionContext)) {
   *   // some stuff
   * }
   * </pre>
   */
  public FolioExecutionContextSetter(FolioExecutionContext folioExecutionContext) {
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(folioExecutionContext);
  }

  /**
   * Create a {@link DefaultFolioExecutionContext} from the {@code folioModuleMetadata} and the {@code httpHeaders},
   * and store it in a {@link ThreadLocal} variable.
   *
   * <p>Call {@link #close()} after use to remove it from the {@link ThreadLocal} variable so
   * that any later usage of the thread cannot accidently use a wrong {@link FolioExecutionContext}
   * and unit tests fail if storing {@link FolioExecutionContext} is forgotten.
   *
   * <p>Best practice is try-with-resources that automatically calls {@link #close()} in all cases, even
   * on thrown exception:
   *
   * <pre>
   * try (var x = new FolioExecutionContextSetter(folioModuleMetadata, httpHeaders)) {
   *   // some stuff
   * }
   * </pre>
   *
   * @param httpHeaders where to take the tenant id from
   */
  public FolioExecutionContextSetter(FolioModuleMetadata folioModuleMetadata,
                                     Map<String, Collection<String>> httpHeaders) {
    this(new DefaultFolioExecutionContext(folioModuleMetadata, httpHeaders));
  }

  /**
   * Create a {@link DefaultFolioExecutionContext} from the {@code folioModuleMetadata} and the
   * {@code httpServletRequest}, and store it in a {@link ThreadLocal} variable.
   *
   * <p>Call {@link #close()} after use to remove it from the {@link ThreadLocal} variable so
   * that any later usage of the thread cannot accidently use a wrong {@link FolioExecutionContext}
   * and unit tests fail if storing {@link FolioExecutionContext} is forgotten.
   *
   * <p>Best practice is try-with-resources that automatically calls {@link #close()} in all cases, even
   * on thrown exception:
   *
   * <pre>
   * try (var x = new FolioExecutionContextSetter(folioModuleMetadata, httpServletRequest)) {
   *   // some stuff
   * }
   * </pre>
   *
   * @param httpServletRequest where to take the tenant id from
   */
  public FolioExecutionContextSetter(FolioModuleMetadata folioModuleMetadata, HttpServletRequest httpServletRequest) {
    this(folioModuleMetadata, RequestUtils.getHttpHeadersFromRequest(httpServletRequest));
  }

  /**
   * Remove {@link FolioExecutionContext} from the {@link ThreadLocal} variable.
   */
  @Override
  public void close() {
    FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
  }
}
