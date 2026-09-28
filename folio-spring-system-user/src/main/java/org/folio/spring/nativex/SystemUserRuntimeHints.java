package org.folio.spring.nativex;

import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;

/**
 * GraalVM native-image reachability hints for {@code folio-spring-system-user}.
 *
 * <p>Wired into the context via {@code @ImportRuntimeHints} on
 * {@link org.folio.spring.config.SystemUserConfig} (an auto-configuration entry point). Note this
 * module is {@code @Deprecated(forRemoval = true)}; the hints are provided so the library stays
 * native-correct for consumers that still depend on it (e.g. {@code mod-roles-keycloak}).</p>
 */
public class SystemUserRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    // The three @HttpExchange clients are created imperatively via
    // HttpServiceProxyFactory.createClient(...) in OptionalSystemUserConfig. Unlike declarative
    // @ImportHttpServices registration, imperative creation does NOT get automatic AOT proxy hints,
    // so the JDK dynamic proxies these clients run behind must be registered explicitly. The proxy
    // interface set matches what Spring's ProxyFactory generates for an AOP interface proxy.
    for (var client : new Class<?>[] {AuthnClient.class, UsersClient.class, PermissionsClient.class}) {
      hints.proxies().registerJdkProxy(
          TypeReference.of(client),
          TypeReference.of(SpringProxy.class),
          TypeReference.of(Advised.class),
          TypeReference.of(DecoratingProxy.class));
    }

    // TokenUtils calls io.netty...ServerCookieDecoder.STRICT / Cookie directly (static calls, not
    // reflection), so those types are reachable via static analysis; Netty's own internal reflection
    // is covered by the GraalVM reachability metadata repository. No explicit Netty hint added here.
  }
}
