package org.folio.spring.config;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.EnrichUrlAndHeadersInterceptor;
import org.folio.spring.client.ExchangeLoggingInterceptor;
import org.folio.spring.config.properties.ExchangeHttpClientProperties;
import org.folio.spring.utils.RequestLoggingLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.json.JsonMapper;

@Log4j2
@Configuration
@EnableConfigurationProperties(ExchangeHttpClientProperties.class)
@ConditionalOnProperty(prefix = "folio.exchange", name = "enabled", havingValue = "true")
public class HttpServiceClientConfiguration {

  @Bean
  public ClientHttpRequestInterceptor enrichUrlAndHeadersInterceptor(FolioExecutionContext folioExecutionContext) {
    return new EnrichUrlAndHeadersInterceptor(folioExecutionContext);
  }

  @Bean
  @ConditionalOnProperty(prefix = "folio.logging.exchange", name = "enabled", havingValue = "true")
  public ClientHttpRequestInterceptor loggingInterceptor(@Value("${folio.logging.exchange.level: BASIC}")
                                                         RequestLoggingLevel level) {
    return new ExchangeLoggingInterceptor(level);
  }

  @Bean
  @ConditionalOnMissingBean(name = "exchangeClientHttpRequestFactory")
  public ClientHttpRequestFactory exchangeClientHttpRequestFactory(ExchangeHttpClientProperties properties) {
    log.info("Creating pooled HTTP client for HTTP Service Clients: {}", properties);

    var clientBuilder = HttpClients.custom()
      .setConnectionManager(createConnectionManager(properties))
      .setDefaultRequestConfig(createRequestConfig(properties));

    if (properties.getEvictIdleConnectionsAfter() != null) {
      clientBuilder.evictExpiredConnections()
        .evictIdleConnections(TimeValue.of(properties.getEvictIdleConnectionsAfter()));
    }
    if (!properties.isAutomaticRetriesEnabled()) {
      clientBuilder.disableAutomaticRetries();
    }

    return new HttpComponentsClientHttpRequestFactory(clientBuilder.build());
  }

  @Bean
  public RestClient.Builder restClientBuilder(JsonMapper jsonMapper,
    @Qualifier("exchangeClientHttpRequestFactory") ClientHttpRequestFactory exchangeClientHttpRequestFactory,
    @Qualifier("enrichUrlAndHeadersInterceptor") ClientHttpRequestInterceptor enrichUrlAndHeadersInterceptor,
    @Qualifier("loggingInterceptor") @Autowired(required = false) ClientHttpRequestInterceptor loggingInterceptor,
    @Qualifier("exchangeJsonMapper") @Autowired(required = false) JsonMapper exchangeJsonMapper) {

    var builder = RestClient.builder()
      .requestFactory(exchangeClientHttpRequestFactory)
      .requestInterceptor(enrichUrlAndHeadersInterceptor)
      .configureMessageConverters(configurer ->
        configurer
          .addCustomConverter(new JacksonJsonHttpMessageConverter(getIfNull(exchangeJsonMapper, jsonMapper)))
          .addCustomConverter(new StringHttpMessageConverter())
      );

    if (loggingInterceptor != null) {
      builder
        .bufferContent((uri, httpMethod) -> true)
        .requestInterceptor(loggingInterceptor);
    }

    return builder;
  }

  @Bean
  public HttpServiceProxyFactory httpServiceProxyFactory(RestClient.Builder restClientBuilder) {
    return HttpServiceProxyFactory
      .builderFor(RestClientAdapter.create(restClientBuilder.build()))
      .exchangeAdapterDecorator(NotFoundRestClientAdapterDecorator::new)
      .build();
  }

  static PoolingHttpClientConnectionManager createConnectionManager(ExchangeHttpClientProperties properties) {
    var connectionConfig = ConnectionConfig.custom()
      .setConnectTimeout(Timeout.of(properties.getConnectTimeout()));
    if (properties.getSocketTimeout() != null) {
      connectionConfig.setSocketTimeout(Timeout.of(properties.getSocketTimeout()));
    }
    if (properties.getConnectionTimeToLive() != null) {
      connectionConfig.setTimeToLive(TimeValue.of(properties.getConnectionTimeToLive()));
    }
    if (properties.getValidateAfterInactivity() != null) {
      connectionConfig.setValidateAfterInactivity(TimeValue.of(properties.getValidateAfterInactivity()));
    }

    return PoolingHttpClientConnectionManagerBuilder.create()
      .setMaxConnPerRoute(properties.getMaxConnectionsPerRoute())
      .setMaxConnTotal(properties.getMaxConnectionsTotal())
      .setPoolConcurrencyPolicy(properties.getPoolConcurrencyPolicy())
      .setConnPoolPolicy(properties.getPoolReusePolicy())
      .setDefaultConnectionConfig(connectionConfig.build())
      .build();
  }

  static RequestConfig createRequestConfig(ExchangeHttpClientProperties properties) {
    var requestConfig = RequestConfig.custom();
    if (properties.getConnectionRequestTimeout() != null) {
      requestConfig.setConnectionRequestTimeout(Timeout.of(properties.getConnectionRequestTimeout()));
    }
    if (properties.getResponseTimeout() != null) {
      requestConfig.setResponseTimeout(Timeout.of(properties.getResponseTimeout()));
    }
    return requestConfig.build();
  }
}
