package org.folio.spring.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.spring.client.AuthnClient;
import org.folio.spring.client.PermissionsClient;
import org.folio.spring.client.UsersClient;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.DecoratingProxy;

@UnitTest
class SystemUserRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  SystemUserRuntimeHintsTest() {
    new SystemUserRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registersHttpClientJdkProxies() {
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(
        AuthnClient.class, SpringProxy.class, Advised.class, DecoratingProxy.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(
        UsersClient.class, SpringProxy.class, Advised.class, DecoratingProxy.class)).accepts(hints);
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(
        PermissionsClient.class, SpringProxy.class, Advised.class, DecoratingProxy.class)).accepts(hints);
  }
}
