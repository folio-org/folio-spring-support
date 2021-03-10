package org.folio.spring;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

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
    var expectedUserId = UUID.fromString("ad162b38-1291-4437-8948-9d13eeced9f6");
    var expectedUserName = "John Doe";
    var token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
      + "eyJzdWIiOiJKb2huIERvZSIsInVzZXJfaWQiOiJhZDE2MmIzOC0xMjkxLTQ0MzctODk0OC05ZDEzZWVjZWQ5ZjYifQ."
      + "CLC0P0Obed2A3bJc6JOzkYXPbuedvU0lvAY1MFMEGMM";
    var tenant = "tenant";
    var url = "http://okapi.com";

    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(TENANT, singleton(tenant));
    headers.put(URL, singleton(url));
    headers.put(TOKEN, singleton(token));
    headers.put("Accept", singleton("application/json"));

    var actual = new DefaultFolioExecutionContext(moduleMetadata, headers);

    Consumer<DefaultFolioExecutionContext> contextRequirements = context -> {
      assertThat(context.getFolioModuleMetadata()).isEqualTo(moduleMetadata);
      assertThat(context.getAllHeaders()).isEqualTo(headers);
      assertThat(context.getOkapiHeaders()).containsOnlyKeys(TENANT, URL, TOKEN);
      assertThat(context.getOkapiUrl()).isEqualTo(url);
      assertThat(context.getToken()).isEqualTo(token);
      assertThat(context.getTenantId()).isEqualTo(tenant);
      assertThat(context.getUserId()).isEqualTo(expectedUserId);
      assertThat(context.getUserName()).isEqualTo(expectedUserName);
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
      assertThat(context.getUserName()).isEqualTo("NO_USER");
    };

    assertThat(actual).satisfies(contextRequirements);
  }

  @Test
  void testExtractionFromHeadersWhenTokenHeaderIsInvalid() {
    Map<String, Collection<String>> headers = new HashMap<>();
    headers.put(TENANT, singleton("tenant"));
    headers.put(TOKEN, singleton("invalid-token"));
    headers.put(URL, singleton("http://okapi.com"));

    var actual = new DefaultFolioExecutionContext(moduleMetadata, headers);

    Consumer<DefaultFolioExecutionContext> contextRequirements = context -> {
      assertThat(context.getOkapiHeaders()).containsOnlyKeys(TENANT, URL, TOKEN);
      assertThat(context.getToken()).isNotEmpty();
      assertThat(context.getUserId()).isNull();
      assertThat(context.getUserName()).isEqualTo("NO_USER");
    };

    assertThat(actual).satisfies(contextRequirements);
  }
}