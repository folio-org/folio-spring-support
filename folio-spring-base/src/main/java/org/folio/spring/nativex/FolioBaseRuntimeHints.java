package org.folio.spring.nativex;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;

import java.io.Serializable;
import java.util.List;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioPostgresDatabase;
import org.folio.spring.logging.FolioLoggingContextLookup;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;

/**
 * GraalVM native-image reachability hints for {@code folio-spring-base}.
 *
 * <p>Wired into the context via {@code @ImportRuntimeHints} on
 * {@link org.folio.spring.config.FolioSpringConfiguration} (an auto-configuration entry point), so
 * every consuming application inherits these hints automatically during Spring AOT processing.</p>
 */
public class FolioBaseRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    registerFolioExecutionContextProxy(hints);
    registerLog4j2Plugin(hints);
    registerLiquibaseDatabase(hints);
  }

  /**
   * {@code FolioExecutionScopeConfig#folioExecutionContext()} is a scoped bean declared with
   * {@link org.springframework.context.annotation.ScopedProxyMode#INTERFACES}, which produces a JDK
   * dynamic proxy over the {@link FolioExecutionContext} interface plus Spring's scoped-proxy
   * infrastructure interfaces. Spring AOT normally registers this proxy automatically; this is the
   * explicit, refactor-safe safety net, using the exact interface set (and order) that Spring's
   * {@code ScopedProxyFactoryBean} produces at runtime.
   *
   * <p>Reflective <em>invocation</em> of the proxy's methods — which the scoped-proxy machinery performs
   * via {@code Method.invoke} on every accessor call (e.g. {@code getTenantId()}) — cannot be expressed
   * through the {@link RuntimeHints} API ({@code ProxyHints} registers proxy creation only, not method
   * invocation on a synthesized proxy). It is therefore shipped as raw reachability metadata in
   * {@code META-INF/native-image/org.folio/folio-spring-base/reachability-metadata.json}. Without it,
   * every request fails natively with {@code MissingReflectionRegistrationError} on the first
   * {@code FolioExecutionContext} accessor.
   */
  private void registerFolioExecutionContextProxy(RuntimeHints hints) {
    hints.proxies().registerJdkProxy(
        TypeReference.of(FolioExecutionContext.class),
        TypeReference.of(ScopedObject.class),
        TypeReference.of(Serializable.class),
        TypeReference.of(AopInfrastructureBean.class),
        TypeReference.of(SpringProxy.class),
        TypeReference.of(Advised.class),
        TypeReference.of(DecoratingProxy.class));
  }

  /**
   * {@link FolioLoggingContextLookup} is a custom Log4j2 {@code @Plugin} that Log4j2 instantiates
   * reflectively via its no-arg constructor (discovered through {@code Log4j2Plugins.dat}). The
   * log4j-core annotation processor already emits reflect-config for it under
   * {@code META-INF/native-image/log4j-generated/}; this defensive hint keeps the plugin reachable
   * even if that generated metadata path changes.
   */
  private void registerLog4j2Plugin(RuntimeHints hints) {
    hints.reflection().registerType(FolioLoggingContextLookup.class,
        builder -> builder.withConstructor(List.of(), ExecutableMode.INVOKE));
  }

  /**
   * {@link FolioPostgresDatabase} is a custom Liquibase {@code Database} registered via
   * {@code DatabaseFactory.register(...)}. Construction itself is a direct {@code new}, but Liquibase
   * enumerates registered databases reflectively, so its constructors and methods must stay reachable.
   */
  private void registerLiquibaseDatabase(RuntimeHints hints) {
    hints.reflection().registerType(FolioPostgresDatabase.class,
        builder -> builder.withMembers(INVOKE_PUBLIC_CONSTRUCTORS, INVOKE_DECLARED_METHODS));
  }
}
