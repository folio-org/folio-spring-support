package org.folio.spring.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;

class ExecutionContextBuilderTest {

  private final ExecutionContextBuilder builder =
      new ExecutionContextBuilder(mock(org.folio.spring.FolioModuleMetadata.class));

  @org.junit.jupiter.api.Test
  void canCreateSystemUserContextForSystemUser() {
    var userId = java.util.UUID.randomUUID();
    var systemUser = SystemUser.builder()
        .token(new UserToken("token", java.time.Instant.EPOCH)).username("username")
        .okapiUrl("okapi").tenantId("tenant")
        .userId(userId.toString())
        .build();
    var context = builder.forSystemUser(systemUser);

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getOkapiUrl()).isEqualTo("okapi");
    assertThat(context.getUserId()).isEqualTo(userId);

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull().hasSize(4);
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }

  @org.junit.jupiter.api.Test
  void canCreateSystemUserContextForMessageHeaders() {
    var userId = java.util.UUID.randomUUID();
    var messageHeaders = new org.springframework.messaging.MessageHeaders(java.util.Map.of(
        org.folio.spring.integration.XOkapiHeaders.TOKEN, "token".getBytes(),
        org.folio.spring.integration.XOkapiHeaders.TENANT, "tenant".getBytes(),
        org.folio.spring.integration.XOkapiHeaders.URL, "okapi".getBytes(),
        org.folio.spring.integration.XOkapiHeaders.USER_ID, userId.toString().getBytes(),
        org.folio.spring.integration.XOkapiHeaders.REQUEST_ID, "request".getBytes()));
    var context = builder.forMessageHeaders(messageHeaders);

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getOkapiUrl()).isEqualTo("okapi");
    assertThat(context.getRequestId()).isEqualTo("request");
    assertThat(context.getUserId()).isEqualTo(userId);

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull().hasSize(5);
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }

  @org.junit.jupiter.api.Test
  void canCreateContextWithNullValues() {
    var systemUser = SystemUser.builder()
        .token(null).username("username")
        .okapiUrl(null).tenantId(null)
        .userId(null)
        .build();

    var context = builder.forSystemUser(systemUser);

    assertThat(context.getTenantId()).isNull();
    assertThat(context.getToken()).isNull();
    assertThat(context.getOkapiUrl()).isNull();

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull().isEmpty();
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }
}
