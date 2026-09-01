package org.folio.spring.kafka.filtering.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy.ACCEPT;
import static org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy.FAIL;
import static org.folio.spring.kafka.filtering.filter.DisabledTenantStrategy.SKIP;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
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
class EnabledTenantMessageFilterStrategyTest {

  private static final String MODULE_ID = "mod-foo-1.0.0";
  private static final String TENANT = "test-tenant";
  private static final String OTHER_TENANT = "other-tenant";
  private static final Set<String> ENABLED_TENANTS = Set.of(TENANT);
  private static final String BLANK_MODULE_ID_MSG = "Module ID must not be blank";

  @Mock
  private TenantEntitlementService tenantEntitlementService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(tenantEntitlementService);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void constructor_negative_blankModuleId(String blankModuleId) {
    assertThatThrownBy(
      () -> new EnabledTenantMessageFilterStrategy<>(blankModuleId, tenantEntitlementService, false, SKIP, SKIP))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(BLANK_MODULE_ID_MSG);
  }

  @Test
  void constructor_negative_nullTenantDisabledStrategy() {
    assertThatThrownBy(
      () -> new EnabledTenantMessageFilterStrategy<>(MODULE_ID, tenantEntitlementService, false, null, SKIP))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_negative_nullAllTenantsDisabledStrategy() {
    assertThatThrownBy(
      () -> new EnabledTenantMessageFilterStrategy<>(MODULE_ID, tenantEntitlementService, false, SKIP, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void filter_positive_enabledTenant_returnsAccepted() {
    var filterStrategy = createFilterStrategy(false, SKIP, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filterStrategy.filter(consumerRecord("key-1", TENANT));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_enabledTenantWithWhitespace_returnsAccepted() {
    var filterStrategy = createFilterStrategy(false, SKIP, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filterStrategy.filter(consumerRecord("key-1", " " + TENANT + " "));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_disabledTenant_acceptStrategy_returnsAccepted() {
    var filterStrategy = createFilterStrategy(false, ACCEPT, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filterStrategy.filter(consumerRecord("key-1", OTHER_TENANT));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_disabledTenant_skipStrategy_returnsFiltered() {
    var filterStrategy = createFilterStrategy(false, SKIP, ACCEPT);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filterStrategy.filter(consumerRecord("key-1", OTHER_TENANT));

    assertThat(result).isTrue();
  }

  @Test
  void filter_negative_disabledTenant_failStrategy_throwsTenantIsDisabledException() {
    var filterStrategy = createFilterStrategy(false, FAIL, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);
    var rec = consumerRecord("key-1", OTHER_TENANT);

    assertThatThrownBy(() -> filterStrategy.filter(rec))
      .isInstanceOf(TenantIsDisabledException.class)
      .hasMessageContaining(OTHER_TENANT)
      .hasMessageContaining(MODULE_ID);
  }

  @Test
  void filter_positive_allTenantsDisabled_acceptStrategy_returnsAccepted() {
    var filterStrategy = createFilterStrategy(false, SKIP, ACCEPT);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(Set.of());

    var result = filterStrategy.filter(consumerRecord("key-1", TENANT));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_allTenantsDisabled_skipStrategy_returnsFiltered() {
    var filterStrategy = createFilterStrategy(false, ACCEPT, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(Set.of());

    var result = filterStrategy.filter(consumerRecord("key-1", TENANT));

    assertThat(result).isTrue();
  }

  @Test
  void filter_negative_allTenantsDisabled_failStrategy_throwsTenantsAreDisabledException() {
    var filterStrategy = createFilterStrategy(false, SKIP, FAIL);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(Set.of());
    var rec = consumerRecord("key-1", TENANT);

    assertThatThrownBy(() -> filterStrategy.filter(rec))
      .isInstanceOf(TenantsAreDisabledException.class)
      .hasMessageContaining(MODULE_ID);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void filter_positive_missingOrBlankTenantHeader_returnsAccepted(String blankTenant) {
    var filterStrategy = createFilterStrategy(false, SKIP, SKIP);

    var result = filterStrategy.filter(consumerRecord("key-1", blankTenant));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_multipleTenantHeaders_usesFirstTenantHeader() {
    var filterStrategy = createFilterStrategy(false, SKIP, SKIP);
    var kafkaRecord = consumerRecord("key-1", TENANT);
    kafkaRecord.headers().add(XOkapiHeaders.TENANT, OTHER_TENANT.getBytes(UTF_8));
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filterStrategy.filter(kafkaRecord);

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_multipleTenantHeaders_usesFirstNonBlankTenantHeader() {
    var filterStrategy = createFilterStrategy(false, SKIP, SKIP);
    var kafkaRecord = consumerRecord("key-1", " ");
    kafkaRecord.headers().add(XOkapiHeaders.TENANT, TENANT.getBytes(UTF_8));
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filterStrategy.filter(kafkaRecord);

    assertThat(result).isFalse();
  }

  @Test
  void ignoreEmptyBatch_positive_returnsTrue() {
    var filterStrategy = createFilterStrategy(true, SKIP, SKIP);

    assertThat(filterStrategy.ignoreEmptyBatch()).isTrue();
  }

  @Test
  void ignoreEmptyBatch_positive_returnsFalse() {
    var filterStrategy = createFilterStrategy(false, SKIP, SKIP);

    assertThat(filterStrategy.ignoreEmptyBatch()).isFalse();
  }

  private EnabledTenantMessageFilterStrategy<String, Object> createFilterStrategy(
    boolean ignoreEmptyBatch,
    DisabledTenantStrategy disabledTenantStrategy,
    DisabledTenantStrategy allTenantsStrategy) {
    return new EnabledTenantMessageFilterStrategy<>(MODULE_ID, tenantEntitlementService,
      ignoreEmptyBatch, disabledTenantStrategy, allTenantsStrategy);
  }

  private static ConsumerRecord<String, Object> consumerRecord(String key, String tenant) {
    var kafkaRecord = new ConsumerRecord<>("test-topic", 0, 0L, key, new Object());

    if (tenant != null) {
      kafkaRecord.headers().add(XOkapiHeaders.TENANT, tenant.getBytes(UTF_8));
    }

    return kafkaRecord;
  }
}
