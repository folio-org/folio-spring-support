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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@UnitTest
class ExecutionContextBuilderTest {

  private static final String TEST_TENANT_ID = "test-tenant";
  private static final String TEST_OKAPI_URL = "http://okapi:9130";

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
    when(folioEnvironment.getOkapiUrl()).thenReturn(TEST_OKAPI_URL);

    var context = builder.buildContext(TEST_TENANT_ID);

    assertThat(context.getTenantId()).isEqualTo(TEST_TENANT_ID);
    assertThat(context.getToken()).isEqualTo(EMPTY);
    assertThat(context.getOkapiUrl()).isEqualTo(TEST_OKAPI_URL);
    assertThat(context.getRequestId()).isNullOrEmpty();

    assertThat(context.getAllHeaders()).hasSize(2);
    assertThat(context.getOkapiHeaders()).hasSize(2);
    assertThat(context.getFolioModuleMetadata()).isEqualTo(folioModuleMetadata);
  }

  @Test
  void buildContext_withHeaders() {
    var userId = UUID.randomUUID().toString();
    var headers = Map.<String, Collection<String>>of(
      XOkapiHeaders.USER_ID, List.of(userId)
    );

    when(folioEnvironment.getOkapiUrl()).thenReturn(TEST_OKAPI_URL);

    var context = builder.buildContext(TEST_TENANT_ID, headers);

    assertThat(context.getTenantId()).isEqualTo(TEST_TENANT_ID);
    assertThat(context.getOkapiUrl()).isEqualTo(TEST_OKAPI_URL);
    assertThat(context.getOkapiHeaders())
      .containsEntry(XOkapiHeaders.USER_ID, List.of(userId))
      .containsEntry(XOkapiHeaders.URL, Set.of(TEST_OKAPI_URL))
      .containsEntry(XOkapiHeaders.TENANT, Set.of(TEST_TENANT_ID));
  }

  @NullAndEmptySource
  @ParameterizedTest
  void buildContext_withNoHeaders(Map<String, Collection<String>> headers) {
    when(folioEnvironment.getOkapiUrl()).thenReturn(TEST_OKAPI_URL);

    var context = builder.buildContext(TEST_TENANT_ID, headers);

    assertThat(context.getTenantId()).isEqualTo(TEST_TENANT_ID);
    assertThat(context.getOkapiUrl()).isEqualTo(TEST_OKAPI_URL);
    assertThat(context.getOkapiHeaders())
      .hasSize(2)
      .containsEntry(XOkapiHeaders.URL, Set.of(TEST_OKAPI_URL))
      .containsEntry(XOkapiHeaders.TENANT, Set.of(TEST_TENANT_ID));
  }

  @Test
  void buildContext_withBothUserIdParameterAndHeadersContainingUserId_shouldUseHeadersUserId() {
    var userIdFromParameter = UUID.randomUUID();
    var userIdFromHeaders = UUID.randomUUID().toString();
    var headers = Map.<String, Collection<String>>of(
      XOkapiHeaders.USER_ID, List.of(userIdFromHeaders)
    );

    when(folioEnvironment.getOkapiUrl()).thenReturn(TEST_OKAPI_URL);

    var context = builder.buildContext(TEST_TENANT_ID, userIdFromParameter, headers);

    assertThat(context.getTenantId()).isEqualTo(TEST_TENANT_ID);
    assertThat(context.getOkapiUrl()).isEqualTo(TEST_OKAPI_URL);
    assertThat(context.getUserId()).isEqualTo(UUID.fromString(userIdFromHeaders));
    assertThat(context.getOkapiHeaders())
      .containsEntry(XOkapiHeaders.USER_ID, List.of(userIdFromHeaders))
      .containsEntry(XOkapiHeaders.URL, Set.of(TEST_OKAPI_URL))
      .containsEntry(XOkapiHeaders.TENANT, Set.of(TEST_TENANT_ID));
  }

  @Test
  void buildContext_withOnlyUserIdParameter_shouldUseParameterUserId() {
    var userId = UUID.randomUUID();
    var headers = Map.<String, Collection<String>>of();

    when(folioEnvironment.getOkapiUrl()).thenReturn(TEST_OKAPI_URL);

    var context = builder.buildContext(TEST_TENANT_ID, userId, headers);

    assertThat(context.getTenantId()).isEqualTo(TEST_TENANT_ID);
    assertThat(context.getOkapiUrl()).isEqualTo(TEST_OKAPI_URL);
    assertThat(context.getUserId()).isEqualTo(userId);
    assertThat(context.getOkapiHeaders())
      .containsEntry(XOkapiHeaders.USER_ID, Set.of(userId.toString()))
      .containsEntry(XOkapiHeaders.URL, Set.of(TEST_OKAPI_URL))
      .containsEntry(XOkapiHeaders.TENANT, Set.of(TEST_TENANT_ID));
  }

  @Test
  void buildContext_withOnlyHeadersContainingUserId_shouldUseHeadersUserId() {
    var userIdFromHeaders = UUID.randomUUID().toString();
    var headers = Map.<String, Collection<String>>of(
      XOkapiHeaders.USER_ID, List.of(userIdFromHeaders)
    );

    when(folioEnvironment.getOkapiUrl()).thenReturn(TEST_OKAPI_URL);

    var context = builder.buildContext(TEST_TENANT_ID, null, headers);

    assertThat(context.getTenantId()).isEqualTo(TEST_TENANT_ID);
    assertThat(context.getOkapiUrl()).isEqualTo(TEST_OKAPI_URL);
    assertThat(context.getUserId()).isEqualTo(UUID.fromString(userIdFromHeaders));
    assertThat(context.getOkapiHeaders())
      .containsEntry(XOkapiHeaders.USER_ID, List.of(userIdFromHeaders))
      .containsEntry(XOkapiHeaders.URL, Set.of(TEST_OKAPI_URL))
      .containsEntry(XOkapiHeaders.TENANT, Set.of(TEST_TENANT_ID));
  }
}
