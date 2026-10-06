package org.folio.spring.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.Map;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.config.properties.ExchangeHttpClientProperties;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class HttpServiceClientConfigurationTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
    .withUserConfiguration(HttpServiceClientConfiguration.class)
    .withBean(FolioExecutionContext.class, () -> mock(FolioExecutionContext.class))
    .withBean(JsonMapper.class, JsonMapper::new)
    .withPropertyValues("folio.exchange.enabled=true");

  @Test
  void context_positive_defaultsBindAndBeansCreated() {
    runner.run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context).hasSingleBean(HttpServiceProxyFactory.class);
      assertThat(context.getBean("exchangeClientHttpRequestFactory"))
        .isInstanceOf(HttpComponentsClientHttpRequestFactory.class);

      var props = context.getBean(ExchangeHttpClientProperties.class);
      assertThat(props.getMaxConnectionsPerRoute()).isEqualTo(50);
      assertThat(props.getMaxConnectionsTotal()).isEqualTo(100);
      assertThat(props.getPoolConcurrencyPolicy()).isEqualTo(PoolConcurrencyPolicy.STRICT);
      assertThat(props.getPoolReusePolicy()).isEqualTo(PoolReusePolicy.LIFO);
      assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
      assertThat(props.getSocketTimeout()).isNull();
      assertThat(props.getResponseTimeout()).isNull();
      assertThat(props.isAutomaticRetriesEnabled()).isTrue();
    });
  }

  @Test
  void context_positive_customPropertiesBind() {
    runner.withPropertyValues(
      "folio.exchange.http-client.max-connections-per-route=7",
      "folio.exchange.http-client.max-connections-total=9",
      "folio.exchange.http-client.pool-concurrency-policy=LAX",
      "folio.exchange.http-client.pool-reuse-policy=FIFO",
      "folio.exchange.http-client.connect-timeout=3s",
      "folio.exchange.http-client.socket-timeout=4s",
      "folio.exchange.http-client.connection-time-to-live=5m",
      "folio.exchange.http-client.validate-after-inactivity=2s",
      "folio.exchange.http-client.connection-request-timeout=6s",
      "folio.exchange.http-client.response-timeout=7s",
      "folio.exchange.http-client.evict-idle-connections-after=30s",
      "folio.exchange.http-client.automatic-retries-enabled=false"
    ).run(context -> {
      assertThat(context).hasNotFailed();
      var props = context.getBean(ExchangeHttpClientProperties.class);
      assertThat(props.getMaxConnectionsPerRoute()).isEqualTo(7);
      assertThat(props.getMaxConnectionsTotal()).isEqualTo(9);
      assertThat(props.getPoolConcurrencyPolicy()).isEqualTo(PoolConcurrencyPolicy.LAX);
      assertThat(props.getPoolReusePolicy()).isEqualTo(PoolReusePolicy.FIFO);
      assertThat(props.getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
      assertThat(props.getSocketTimeout()).isEqualTo(Duration.ofSeconds(4));
      assertThat(props.getConnectionTimeToLive()).isEqualTo(Duration.ofMinutes(5));
      assertThat(props.getValidateAfterInactivity()).isEqualTo(Duration.ofSeconds(2));
      assertThat(props.getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(6));
      assertThat(props.getResponseTimeout()).isEqualTo(Duration.ofSeconds(7));
      assertThat(props.getEvictIdleConnectionsAfter()).isEqualTo(Duration.ofSeconds(30));
      assertThat(props.isAutomaticRetriesEnabled()).isFalse();
    });
  }

  @Test
  void context_positive_envVariablesBindAndOverrideConfigFiles() {
    runner.withPropertyValues("folio.exchange.http-client.max-connections-per-route=10")
      .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
        new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, Map.of(
          "FOLIO_EXCHANGE_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE", "77",
          "FOLIO_EXCHANGE_HTTP_CLIENT_CONNECTION_REQUEST_TIMEOUT", "30s",
          "FOLIO_EXCHANGE_HTTP_CLIENT_AUTOMATIC_RETRIES_ENABLED", "false"))))
      .run(context -> {
        assertThat(context).hasNotFailed();
        var props = context.getBean(ExchangeHttpClientProperties.class);
        assertThat(props.getMaxConnectionsPerRoute()).isEqualTo(77);
        assertThat(props.getConnectionRequestTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(props.isAutomaticRetriesEnabled()).isFalse();
      });
  }

  @Test
  void context_positive_zeroTimeoutsAndEqualPoolSizesAreValid() {
    runner.withPropertyValues(
      "folio.exchange.http-client.max-connections-per-route=20",
      "folio.exchange.http-client.max-connections-total=20",
      "folio.exchange.http-client.connect-timeout=0s",
      "folio.exchange.http-client.response-timeout=0s"
    ).run(context -> assertThat(context).hasNotFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "max-connections-per-route=0",
    "max-connections-total=-1",
    "max-connections-total=10",
    "connect-timeout=-1s",
    "socket-timeout=-1s",
    "connection-time-to-live=-1s",
    "validate-after-inactivity=-1s",
    "connection-request-timeout=-1s",
    "response-timeout=-1s",
    "evict-idle-connections-after=-1s"
  })
  void context_negative_invalidPropertyFailsStartup(String property) {
    runner.withPropertyValues("folio.exchange.http-client." + property)
      .run(context -> assertThat(context).hasFailed()
        .getFailure().rootCause().hasMessageContaining("folio.exchange.http-client"));
  }

  @Test
  void createConnectionManager_positive_appliesPoolSizes() {
    var props = new ExchangeHttpClientProperties();
    props.setMaxConnectionsPerRoute(11);
    props.setMaxConnectionsTotal(22);
    props.setSocketTimeout(Duration.ofSeconds(1));
    props.setConnectionTimeToLive(Duration.ofMinutes(1));
    props.setValidateAfterInactivity(Duration.ofSeconds(1));

    try (var manager = HttpServiceClientConfiguration.createConnectionManager(props)) {
      assertThat(manager.getMaxTotal()).isEqualTo(22);
      assertThat(manager.getDefaultMaxPerRoute()).isEqualTo(11);
    }
  }

  @Test
  void createRequestConfig_positive_timeoutsSet() {
    var props = new ExchangeHttpClientProperties();
    props.setConnectionRequestTimeout(Duration.ofSeconds(2));
    props.setResponseTimeout(Duration.ofSeconds(5));

    var config = HttpServiceClientConfiguration.createRequestConfig(props);

    assertThat(config.getConnectionRequestTimeout().toSeconds()).isEqualTo(2);
    assertThat(config.getResponseTimeout().toSeconds()).isEqualTo(5);
  }

  @Test
  void createRequestConfig_positive_timeoutsUnsetKeepDefaults() {
    var config = HttpServiceClientConfiguration.createRequestConfig(new ExchangeHttpClientProperties());

    assertThat(config.getConnectionRequestTimeout().toMinutes()).isEqualTo(3);
    assertThat(config.getResponseTimeout()).isNull();
  }

  @Test
  void context_positive_userDefinedFactoryOverridesDefault() {
    var custom = new SimpleClientHttpRequestFactory();
    runner.withBean("exchangeClientHttpRequestFactory", ClientHttpRequestFactory.class, () -> custom)
      .run(context -> {
        assertThat(context).hasNotFailed();
        assertThat(context.getBean("exchangeClientHttpRequestFactory")).isSameAs(custom);
      });
  }
}
