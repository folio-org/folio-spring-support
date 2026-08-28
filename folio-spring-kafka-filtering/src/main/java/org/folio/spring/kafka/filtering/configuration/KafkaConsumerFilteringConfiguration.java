package org.folio.spring.kafka.filtering.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementClient;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.folio.spring.kafka.filtering.filter.EnabledTenantMessageFilterStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the {@code tenantAwareMessageFilter} bean used by Kafka listeners.
 */
@Log4j2
@AutoConfiguration
@EnableConfigurationProperties(KafkaTenantFilterProperties.class)
public class KafkaConsumerFilteringConfiguration {

  private static final String ENTITLEMENT_CLIENT_GROUP = "kafka-filter-entitlement-client";

  /**
   * Creates Kafka consumer filtering auto-configuration.
   */
  public KafkaConsumerFilteringConfiguration() {
  }

  /**
   * Configuration active when tenant filtering is enabled.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = KafkaTenantFilterProperties.PREFIX, name = "enabled", havingValue = "true")
  @ImportHttpServices(types = TenantEntitlementClient.class, group = ENTITLEMENT_CLIENT_GROUP)
  public static class EnabledTenantFilterConfiguration {

    /**
     * Creates enabled tenant filter configuration.
     */
    public EnabledTenantFilterConfiguration() {
    }

    /**
     * Configures the HTTP client used to resolve entitled tenants.
     *
     * @param okapiUrl base URL for the tenant entitlement client
     * @param jsonMapper JSON mapper used by the HTTP client
     * @param loggingInterceptor optional folio-spring-support logging interceptor
     * @return rest client group configurer
     */
    @Bean
    public RestClientHttpServiceGroupConfigurer tenantEntitlementClientGroupConfigurer(
      @Value("${okapi.url}") @NotBlank String okapiUrl,
      JsonMapper jsonMapper,
      @Qualifier("loggingInterceptor") @Autowired(required = false) ClientHttpRequestInterceptor loggingInterceptor) {

      return groups -> groups.filterByName(ENTITLEMENT_CLIENT_GROUP).forEachClient((group, builder) -> {
        builder.baseUrl(okapiUrl)
          .configureMessageConverters(converters ->
            converters
              .addCustomConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
              .addCustomConverter(new StringHttpMessageConverter()));

        if (loggingInterceptor != null) {
          builder.bufferContent((uri, httpMethod) -> true)
            .requestInterceptor(loggingInterceptor);
        }

        builder.build();
      });
    }

    /**
     * Creates the service used to resolve tenants entitled to the current module.
     *
     * @param folioModuleMetadata current module metadata
     * @param tenantEntitlementClient HTTP client for tenant entitlement lookups
     * @return tenant entitlement service
     */
    @Bean
    public TenantEntitlementService tenantEntitlementService(FolioModuleMetadata folioModuleMetadata,
      TenantEntitlementClient tenantEntitlementClient) {
      if (folioModuleMetadata.getModuleVersion().filter(StringUtils::isNotBlank).isEmpty()) {
        throw new IllegalStateException("Kafka tenant filtering requires spring.application.version");
      }

      return new TenantEntitlementService(folioModuleMetadata.getModuleId(), tenantEntitlementClient);
    }

    /**
     * Creates the tenant-aware Kafka record filter.
     *
     * @param tenantEntitlementService service for tenant entitlement lookups
     * @param tenantFilterProperties filter settings
     * @param <K> Kafka record key type
     * @param <V> Kafka record value type
     * @return tenant-aware record filter
     */
    @Bean("tenantAwareMessageFilter")
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
    public <K, V> RecordFilterStrategy<K, V> enabledTenantMessageFilter(
      TenantEntitlementService tenantEntitlementService, KafkaTenantFilterProperties tenantFilterProperties) {

      log.info("Kafka tenant aware message filter enabled: tenantFilter = {}", tenantFilterProperties);
      return new EnabledTenantMessageFilterStrategy<>(
        tenantEntitlementService.getModuleId(),
        tenantEntitlementService,
        tenantFilterProperties.isIgnoreEmptyBatch(),
        tenantFilterProperties.getTenantDisabledStrategy(),
        tenantFilterProperties.getAllTenantsDisabledStrategy()
      );
    }
  }

  /**
   * Configuration active when tenant filtering is disabled.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = KafkaTenantFilterProperties.PREFIX, name = "enabled", havingValue = "false",
    matchIfMissing = true)
  public static class DisabledTenantFilterConfiguration {

    /**
     * Creates disabled tenant filter configuration.
     */
    public DisabledTenantFilterConfiguration() {
    }

    /**
     * Creates a no-op Kafka record filter.
     *
     * @param <K> Kafka record key type
     * @param <V> Kafka record value type
     * @return no-op record filter
     */
    @Bean("tenantAwareMessageFilter")
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
    public <K, V> RecordFilterStrategy<K, V> disabledTenantMessageFilter() {
      log.info("Kafka tenant aware message filter disabled");
      return consumerRecord -> false;
    }
  }
}
