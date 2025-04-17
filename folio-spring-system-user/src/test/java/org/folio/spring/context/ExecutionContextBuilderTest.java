package org.folio.spring.context;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.config.properties.FolioEnvironment;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.model.SystemUser;
import org.folio.spring.model.UserToken;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@UnitTest
class ExecutionContextBuilderTest {

  @InjectMocks private ExecutionContextBuilder builder;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private FolioEnvironment folioEnvironment;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(folioModuleMetadata, folioEnvironment);
  }

  @Test
  void canCreateSystemUserContextForSystemUser() {
    var userId = java.util.UUID.randomUUID();
    var systemUser = SystemUser.builder()
        .token(new UserToken("token", java.time.Instant.EPOCH)).username("username")
        .okapiUrl("okapi").tenantId("tenant")
        .userId(userId.toString())
        .build();
    var context = builder.forSystemUser(systemUser, null);

    assertThat(context.getTenantId()).isEqualTo("tenant");
    assertThat(context.getToken()).isEqualTo("token");
    assertThat(context.getOkapiUrl()).isEqualTo("okapi");
    assertThat(context.getUserId()).isEqualTo(userId);
    assertThat(context.getRequestId()).isNullOrEmpty();

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

    var context = builder.forSystemUser(systemUser, null);

    assertThat(context.getTenantId()).isEqualTo(EMPTY);
    assertThat(context.getToken()).isEqualTo(EMPTY);
    assertThat(context.getOkapiUrl()).isEqualTo(EMPTY);
    assertThat(context.getRequestId()).isNullOrEmpty();

    assertThat(context.getAllHeaders()).isNotNull();
    assertThat(context.getOkapiHeaders()).isNotNull().isEmpty();
    assertThat(context.getFolioModuleMetadata()).isNotNull();
  }

  @Test
  void canCreateContextForDisabledSystemUser() {
    var tenantId = "test-tenant";
    var okapiUrl = "http://okapi:9130";
    when(folioEnvironment.getOkapiUrl()).thenReturn(okapiUrl);

    var context = builder.buildContext(tenantId);

    assertThat(context.getTenantId()).isEqualTo(tenantId);
    assertThat(context.getToken()).isEqualTo(EMPTY);
    assertThat(context.getOkapiUrl()).isEqualTo(okapiUrl);
    assertThat(context.getRequestId()).isNullOrEmpty();

    assertThat(context.getAllHeaders()).hasSize(2);
    assertThat(context.getOkapiHeaders()).hasSize(2);
    assertThat(context.getFolioModuleMetadata()).isEqualTo(folioModuleMetadata);
  }

  @Test
  void buildContext_withHeaders() {
    var tenantId = "test-tenant";
    var userId = UUID.randomUUID().toString();
    var okapiUrl = "http://okapi:9130";
    var headers = Map.<String, Collection<String>>of(
      XOkapiHeaders.USER_ID, List.of(userId)
    );

    when(folioEnvironment.getOkapiUrl()).thenReturn(okapiUrl);

    var context = builder.buildContext(tenantId, headers);

    assertThat(context.getTenantId()).isEqualTo(tenantId);
    assertThat(context.getOkapiUrl()).isEqualTo(okapiUrl);
    assertThat(context.getOkapiHeaders())
      .containsEntry(XOkapiHeaders.USER_ID, List.of(userId))
      .containsEntry(XOkapiHeaders.URL, Set.of(okapiUrl))
      .containsEntry(XOkapiHeaders.TENANT, Set.of(tenantId));
  }

  @Test
  void buildContext_withNoHeaders() {
    var tenantId = "test-tenant";
    var okapiUrl = "http://okapi:9130";

    when(folioEnvironment.getOkapiUrl()).thenReturn(okapiUrl);

    var context = builder.buildContext(tenantId, null);

    assertThat(context.getTenantId()).isEqualTo(tenantId);
    assertThat(context.getOkapiUrl()).isEqualTo(okapiUrl);
    assertThat(context.getOkapiHeaders())
      .hasSize(2)
      .containsEntry(XOkapiHeaders.URL, Set.of(okapiUrl))
      .containsEntry(XOkapiHeaders.TENANT, Set.of(tenantId));
  }
}
