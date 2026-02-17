package org.folio.spring.config;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.EnrichUrlAndHeadersInterceptor;
import org.folio.spring.client.ExchangeLoggingInterceptor;
import org.folio.spring.utils.RequestLoggingLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.NotFoundRestClientAdapterDecorator;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
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
  public RestClient.Builder restClientBuilder(
    @Qualifier("enrichUrlAndHeadersInterceptor") ClientHttpRequestInterceptor enrichUrlAndHeadersInterceptor,
    @Qualifier("loggingInterceptor") @Autowired(required = false) ClientHttpRequestInterceptor loggingInterceptor) {

    var builder = RestClient.builder()
      .requestInterceptor(enrichUrlAndHeadersInterceptor);

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
}
