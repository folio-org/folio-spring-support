package org.folio.spring.kafka.filtering.entitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
  void getEnabledTenants_positive_fetchesOnceAndCachesResult() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(ENABLED_TENANTS);

    var first = service.getEnabledTenants();
    var second = service.getEnabledTenants();

    assertThat(first).isEqualTo(ENABLED_TENANTS);
    assertThat(second).isEqualTo(ENABLED_TENANTS);
    verify(tenantEntitlementClient, times(1)).lookupTenantsByModuleId(MODULE_ID);
  }

  @Test
  void getEnabledTenants_positive_returnsEmptySetForNullResponse() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(null);

    var result = service.getEnabledTenants();

    assertThat(result).isEmpty();
  }

  @Test
  void refresh_positive_replacesCachedResult() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID))
      .thenReturn(ENABLED_TENANTS)
      .thenReturn(Set.of("tenant-3"));

    service.getEnabledTenants();
    var refreshed = service.refresh();

    assertThat(refreshed).containsExactly("tenant-3");
    assertThat(service.getEnabledTenants()).containsExactly("tenant-3");
    verify(tenantEntitlementClient, times(2)).lookupTenantsByModuleId(MODULE_ID);
  }

  @Test
  void applyEntitlementEvent_positive_addsTenantOnEntitle() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(Set.of("tenant-1"));
    service.getEnabledTenants();

    service.applyEntitlementEvent(new EntitlementEvent(EntitlementEvent.Type.ENTITLE, MODULE_ID, "tenant-2"));

    assertThat(service.getEnabledTenants()).containsExactlyInAnyOrder("tenant-1", "tenant-2");
    verify(tenantEntitlementClient, times(1)).lookupTenantsByModuleId(MODULE_ID);
  }

  @Test
  void applyEntitlementEvent_positive_addsTenantOnUpgrade() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(Set.of("tenant-1"));
    service.getEnabledTenants();

    service.applyEntitlementEvent(new EntitlementEvent(EntitlementEvent.Type.UPGRADE, MODULE_ID, "tenant-2"));

    assertThat(service.getEnabledTenants()).containsExactlyInAnyOrder("tenant-1", "tenant-2");
    verify(tenantEntitlementClient, times(1)).lookupTenantsByModuleId(MODULE_ID);
  }

  @Test
  void applyEntitlementEvent_positive_removesTenantOnRevoke() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(ENABLED_TENANTS);
    service.getEnabledTenants();

    service.applyEntitlementEvent(new EntitlementEvent(EntitlementEvent.Type.REVOKE, MODULE_ID, "tenant-1"));

    assertThat(service.getEnabledTenants()).containsExactly("tenant-2");
    verify(tenantEntitlementClient, times(1)).lookupTenantsByModuleId(MODULE_ID);
  }

  @Test
  void applyEntitlementEvent_positive_ignoresEventForOtherModule() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(ENABLED_TENANTS);
    service.getEnabledTenants();

    service.applyEntitlementEvent(new EntitlementEvent(EntitlementEvent.Type.REVOKE, "mod-bar-1.0.0", "tenant-1"));

    assertThat(service.getEnabledTenants()).isEqualTo(ENABLED_TENANTS);
    verify(tenantEntitlementClient, times(1)).lookupTenantsByModuleId(MODULE_ID);
  }

  @Test
  void applyEntitlementEvent_positive_ignoredWhenCacheNotYetPopulated() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);

    service.applyEntitlementEvent(new EntitlementEvent(EntitlementEvent.Type.ENTITLE, MODULE_ID, "tenant-1"));

    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(ENABLED_TENANTS);
    assertThat(service.getEnabledTenants()).isEqualTo(ENABLED_TENANTS);
    verify(tenantEntitlementClient, times(1)).lookupTenantsByModuleId(MODULE_ID);
  }
}
