package org.folio.spring.kafka.filtering.configuration;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.kafka.filtering.entitlement.EntitlementEvent;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementClient;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementService;
import org.folio.spring.kafka.filtering.filter.EnabledTenantMessageFilterStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the {@code tenantAwareMessageFilter} bean used by Kafka listeners.
 */
@Log4j2
@AutoConfiguration
@AutoConfigureAfter(KafkaAutoConfiguration.class)
@EnableConfigurationProperties(KafkaTenantFilterProperties.class)
public final class KafkaConsumerFilteringConfiguration {

  private static final String ENTITLEMENT_CLIENT_GROUP = "kafka-filter-entitlement-client";

  private KafkaConsumerFilteringConfiguration() {
  }

  /**
   * Configuration active when tenant filtering is enabled.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = KafkaTenantFilterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true")
  @ImportHttpServices(types = TenantEntitlementClient.class, group = ENTITLEMENT_CLIENT_GROUP)
  @EnableScheduling
  public static class EnabledTenantFilterConfiguration {

    private static final String ENTITLEMENT_TOPIC_NAME = "entitlement";
    private static final String ENTITLEMENT_CONSUMER_GROUP_PREFIX = "folio-spring-kafka-entitlement-";

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
        builder
          .baseUrl(okapiUrl)
          .configureMessageConverters(converters ->
            converters
              .addCustomConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
              .addCustomConverter(new StringHttpMessageConverter()));

        if (loggingInterceptor != null) {
          builder
            .bufferContent((uri, httpMethod) -> true)
            .requestInterceptor(loggingInterceptor);
        }
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
    public <K, V> RecordFilterStrategy<K, V> enabledTenantMessageFilterStrategy(
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

    /**
     * Periodically re-fetches the full entitled-tenants set from the entitlement client, correcting any
     * drift accumulated from a missed or duplicate entitlement change event.
     *
     * @param tenantEntitlementService service backing the entitled-tenants cache
     */
    @Bean
    public EntitlementReconciliationTask entitlementReconciliationTask(
      TenantEntitlementService tenantEntitlementService) {
      return new EntitlementReconciliationTask(tenantEntitlementService);
    }

    /**
     * Creates the consumer factory for the {@code entitlement} Kafka topic, reusing Spring Boot's
     * {@code spring.kafka.*} bootstrap and security settings so this consumer authenticates the same
     * way the module's other Kafka consumers do.
     *
     * @param kafkaProperties Spring Boot Kafka configuration properties
     * @param jsonMapper JSON mapper used to deserialize entitlement events
     * @return consumer factory for entitlement change events
     */
    @Bean
    @ConditionalOnBean(KafkaProperties.class)
    public ConsumerFactory<String, EntitlementEvent> entitlementEventConsumerFactory(
      KafkaProperties kafkaProperties, JsonMapper jsonMapper) {

      var configs = kafkaProperties.buildConsumerProperties();
      configs.put(ConsumerConfig.GROUP_ID_CONFIG, ENTITLEMENT_CONSUMER_GROUP_PREFIX + UUID.randomUUID());
      configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
      configs.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);

      var valueDeserializer = new ErrorHandlingDeserializer<>(
        new JacksonJsonDeserializer<>(EntitlementEvent.class, jsonMapper));
      return new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), valueDeserializer);
    }

    /**
     * Starts the listener container that applies entitlement change events directly to the cached
     * entitled-tenants set.
     *
     * @param entitlementEventConsumerFactory consumer factory for entitlement change events
     * @param tenantEntitlementService service backing the entitled-tenants cache
     * @return listener container for entitlement change events
     */
    @Bean(destroyMethod = "stop")
    @ConditionalOnBean(KafkaProperties.class)
    public ConcurrentMessageListenerContainer<String, EntitlementEvent> entitlementEventListenerContainer(
      ConsumerFactory<String, EntitlementEvent> entitlementEventConsumerFactory,
      TenantEntitlementService tenantEntitlementService) {

      var containerProperties = new ContainerProperties(entitlementTopicName());
      containerProperties.setMessageListener((MessageListener<String, EntitlementEvent>) record -> {
        var event = record.value();
        if (event != null) {
          tenantEntitlementService.applyEntitlementEvent(event);
        }
      });

      var container = new ConcurrentMessageListenerContainer<>(entitlementEventConsumerFactory, containerProperties);
      container.start();
      return container;
    }

    private static String entitlementTopicName() {
      var env = StringUtils.firstNonBlank(System.getenv("ENV"), System.getProperty("env"), "folio");
      return env + "." + ENTITLEMENT_TOPIC_NAME;
    }
  }

  /**
   * Configuration active when tenant filtering is disabled.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnProperty(prefix = KafkaTenantFilterProperties.CONFIG_PREFIX, name = "enabled", havingValue = "false",
    matchIfMissing = true)
  public static class DisabledTenantFilterConfiguration {

    /**
     * Creates a no-op Kafka record filter.
     *
     * @param <K> Kafka record key type
     * @param <V> Kafka record value type
     * @return no-op record filter
     */
    @Bean("tenantAwareMessageFilter")
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
    public <K, V> RecordFilterStrategy<K, V> disabledTenantMessageFilterStrategy() {
      log.info("Kafka tenant aware message filter disabled");
      return consumerRecord -> false;
    }
  }
}
