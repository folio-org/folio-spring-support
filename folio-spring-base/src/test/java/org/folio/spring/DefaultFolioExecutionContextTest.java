package org.folio.spring;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.integration.XOkapiHeaders.REQUEST_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class DefaultFolioExecutionContextTest {

  private final FolioModuleMetadata moduleMetadata = new FolioModuleMetadata() {

    @Override public String getModuleName() {
      return "test";
    }

    @Override public String getDBSchemaName(String tenantId) {
      return "tenant_test";
    }
  };

  @Test
  void testExtractionFromHeadersWhenAllNeededHeadersExists() {
    var userId = "ad162b38-1291-4437-8948-9d13eeced9f6";
    var requestId = "heL9F";
    var token = "valid-token";
    var tenant = "tenant";
    var url = "http://okapi.com";

    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(TENANT, singleton(tenant));
    headers.put(URL, singleton(url));
    headers.put(TOKEN, singleton(token));
    headers.put(USER_ID, singleton(userId));
    headers.put(REQUEST_ID, singleton(requestId));
    headers.put("Accept", singleton("application/json"));

    var actual = new DefaultFolioExecutionContext(moduleMetadata, headers);

    Consumer<DefaultFolioExecutionContext> contextRequirements = context -> {
      assertThat(context.getFolioModuleMetadata()).isEqualTo(moduleMetadata);
      assertThat(context.getAllHeaders()).isEqualTo(headers);
      assertThat(context.getOkapiHeaders()).containsOnlyKeys(TENANT, URL, TOKEN, USER_ID, REQUEST_ID);
      assertThat(context.getOkapiUrl()).isEqualTo(url);
      assertThat(context.getToken()).isEqualTo(token);
      assertThat(context.getTenantId()).isEqualTo(tenant);
      assertThat(context.getUserId()).isEqualTo(UUID.fromString(userId));
      assertThat(context.getRequestId()).isEqualTo(requestId);
    };

    assertThat(actual).satisfies(contextRequirements);
  }

  @Test
  void testExtractionFromHeadersWhenTokenHeaderNotExists() {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(TENANT, singleton("tenant"));
    headers.put(URL, singleton("http://okapi.com"));

    var actual = new DefaultFolioExecutionContext(moduleMetadata, headers);

    Consumer<DefaultFolioExecutionContext> contextRequirements = context -> {
      assertThat(context.getOkapiHeaders()).containsOnlyKeys(TENANT, URL);
      assertThat(context.getToken()).isEmpty();
      assertThat(context.getUserId()).isNull();
    };

    assertThat(actual).satisfies(contextRequirements);
  }

}
