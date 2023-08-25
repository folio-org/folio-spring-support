package org.folio.spring.context;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.junit.jupiter.api.Test;

class ExecutionContextBuilderTest {

  private final ExecutionContextBuilder builder =
      new ExecutionContextBuilder(mock(org.folio.spring.FolioModuleMetadata.class));

  @Test
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

  @Test
  void canCreateContextWithNullValues() {
    var systemUser = SystemUser.builder()
        .token(null).username("username")
        .okapiUrl(null).tenantId(null)
        .userId(null)
        .build();

    var context = builder.forSystemUser(systemUser);

    assertThat(context.getTenantId()).isEqualTo(EMPTY);
    assertThat(context.getToken()).isEqualTo(EMPTY);
    assertThat(context.getOkapiUrl()).isEqualTo(EMPTY);

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull().isEmpty();
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }
}
