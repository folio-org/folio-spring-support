package org.folio.spring.systemuser;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.context.ExecutionContextBuilder;
import org.folio.spring.model.SystemUser;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.folio.spring.service.SystemUserService;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemUserScopedExecutionServiceTest {

  private static final String TENANT_ID = "test";

  @InjectMocks
  private SystemUserScopedExecutionService systemUserScopedExecutionService;
  @Mock
  private ExecutionContextBuilder contextBuilder;
  @Mock
  private SystemUserService systemUserService;
  @Mock
  private FolioExecutionContext folioExecutionContext;

  @BeforeEach
  void setUp() {
    systemUserScopedExecutionService.setSystemUserService(systemUserService);
  }

  @Test
  void executeSystemUserScoped_positive() {
    var systemUser = SystemUser.builder().build();
    when(systemUserService.getAuthedSystemUser(TENANT_ID)).thenReturn(systemUser);
    when(contextBuilder.forSystemUser(systemUser)).thenReturn(new DefaultFolioExecutionContext(null, emptyMap()));

    var actual = systemUserScopedExecutionService.executeSystemUserScoped(TENANT_ID, () -> "result");

    assertThat(actual).isEqualTo("result");
  }

  @Test
  void executeSystemUserScoped_positive_systemUserServiceIsNull() {
    when(contextBuilder.buildContext(TENANT_ID)).thenReturn(new DefaultFolioExecutionContext(null, emptyMap()));

    systemUserScopedExecutionService.setSystemUserService(null);
    var actual = systemUserScopedExecutionService.executeSystemUserScoped(TENANT_ID, () -> "result");

    assertThat(actual).isEqualTo("result");
  }

  @Test
  void executeAsyncSystemUserScoped_positive() {
    var systemUser = SystemUser.builder().build();
    when(systemUserService.getAuthedSystemUser(TENANT_ID)).thenReturn(systemUser);
    when(contextBuilder.forSystemUser(systemUser)).thenReturn(new DefaultFolioExecutionContext(null, emptyMap()));
    var runnableMock = mock(Runnable.class);

    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT_ID, runnableMock);

    verify(runnableMock).run();
  }

  @Test
  void executeAsyncSystemUserScoped_positive_systemUserServiceIsNull() {
    when(contextBuilder.buildContext(TENANT_ID)).thenReturn(new DefaultFolioExecutionContext(null, emptyMap()));
    var runnableMock = mock(Runnable.class);

    systemUserScopedExecutionService.setSystemUserService(null);
    systemUserScopedExecutionService.executeAsyncSystemUserScoped(TENANT_ID, runnableMock);

    verify(runnableMock).run();
    verifyNoInteractions(systemUserService);
  }

  @Test
  void executeSystemUserScopedFromContext_positive() {
    var systemUser = SystemUser.builder().build();
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(systemUserService.getAuthedSystemUser(TENANT_ID)).thenReturn(systemUser);
    when(contextBuilder.forSystemUser(systemUser)).thenReturn(new DefaultFolioExecutionContext(null, emptyMap()));

    var actual = systemUserScopedExecutionService.executeSystemUserScoped(() -> "result");

    assertThat(actual).isEqualTo("result");
  }

  @Test
  void executeSystemUserScopedFromContext_positive_systemUserServiceIsNull() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(contextBuilder.buildContext(TENANT_ID)).thenReturn(new DefaultFolioExecutionContext(null, emptyMap()));

    systemUserScopedExecutionService.setSystemUserService(null);
    var actual = systemUserScopedExecutionService.executeSystemUserScoped(() -> "result");

    assertThat(actual).isEqualTo("result");
  }

  @Test
  void executeSystemUserScoped_negative_throwsException() {
    var systemUser = SystemUser.builder().build();
    when(systemUserService.getAuthedSystemUser(TENANT_ID)).thenReturn(systemUser);
    when(contextBuilder.forSystemUser(systemUser)).thenReturn(new DefaultFolioExecutionContext(null, emptyMap()));

    Callable<Object> callable = () -> {
      throw new Exception("error");
    };

    assertThatThrownBy(() -> systemUserScopedExecutionService.executeSystemUserScoped(TENANT_ID, callable))
      .isInstanceOf(Exception.class)
      .hasMessage("error");
  }
}
