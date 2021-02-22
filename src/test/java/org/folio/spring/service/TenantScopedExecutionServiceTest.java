package org.folio.spring.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.domain.SystemUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantScopedExecutionServiceTest {
  public static final String TENANT = "tenant";
  @Mock
  private FolioModuleMetadata moduleMetadata;
  @Mock
  private SystemUserService systemUserService;

  @Test
  @SuppressWarnings("all")
  void shouldUseSystemUserContextWhenPresent() {
    var service = new TenantScopedExecutionService(moduleMetadata,
      Optional.of(systemUserService));

    when(systemUserService.getSystemUserParameters(TENANT))
      .thenReturn(new SystemUser());

    var job = mock(TenantScopedExecutionService.ThrowableSupplier.class);

    service.executeTenantScoped(TENANT, job);

    verify(systemUserService).getSystemUserParameters(TENANT);
  }

  @Test
  @SuppressWarnings("all")
  void shouldUseAsyncUserContextWhenSystemUserNotPresent() throws Exception {
    var service = new TenantScopedExecutionService(moduleMetadata, Optional.empty());
    var job = mock(TenantScopedExecutionService.ThrowableSupplier.class);

    service.executeTenantScoped(TENANT, job);

    verifyNoInteractions(systemUserService);
    verify(job, times(1)).get();
  }
}
