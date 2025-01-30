package org.folio.spring.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.SystemUserProperties;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@UnitTest
class SystemUserConfigTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @Test
  void testFallbackPrepareSystemUserServiceDoesNothing() {
    SystemUserProperties systemUserProperties = new SystemUserProperties();
    systemUserProperties.setEnabled(false);

    PrepareSystemUserService systemUserService = new SystemUserConfig()
      .fallbackSystemUserService(folioExecutionContext, systemUserProperties);

    assertDoesNotThrow(systemUserService::setupSystemUser);
  }
}
