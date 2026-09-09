package org.folio.spring.kafka.filtering.entitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantEntitlementServiceTest {

  private static final String MODULE_ID = "mod-foo-1.0.0";
  private static final Set<String> ENABLED_TENANTS = Set.of("tenant-1", "tenant-2");

  @Mock private TenantEntitlementClient tenantEntitlementClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(tenantEntitlementClient);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void constructor_negative_blankModuleId(String blankModuleId) {
    assertThatThrownBy(() -> new TenantEntitlementService(blankModuleId, tenantEntitlementClient))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Module ID must not be blank");
  }

  @Test
  void getModuleId_positive_returnsModuleId() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);

    assertThat(service.getModuleId()).isEqualTo(MODULE_ID);
  }

  @Test
  void getEnabledTenants_positive_returnsTenants() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(ENABLED_TENANTS);

    var result = service.getEnabledTenants();

    assertThat(result).isEqualTo(ENABLED_TENANTS);
  }

  @Test
  void getEnabledTenants_positive_returnsEmptySetForNullResponse() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(null);

    var result = service.getEnabledTenants();

    assertThat(result).isEmpty();
  }
}
