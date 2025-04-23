package org.folio.spring.config;

import feign.Client;
import feign.Logger;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.EnrichUrlAndHeadersClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignLoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingBean(value = Client.class)
public class FeignClientConfiguration {

  @Value("${feign.httpclient.max-connections}")
  private int maxTotal;

  @Value("${feign.httpclient.max-connections-per-route}")
  private int maxPerRoute;

  @Bean
  public Client enrichUrlAndHeadersClient(@Autowired FolioExecutionContext folioExecutionContext,
    @Autowired CloseableHttpClient closeableHttpClient) {
    return new EnrichUrlAndHeadersClient(folioExecutionContext, closeableHttpClient);
  }

  @Bean
  public PoolingHttpClientConnectionManager poolingConnectionManager() {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setDefaultMaxPerRoute(maxPerRoute);
    cm.setMaxTotal(maxTotal);
    return cm;
  }

  @Bean
  public CloseableHttpClient closeableHttpClient(PoolingHttpClientConnectionManager cm) {
    return HttpClients.custom().setConnectionManager(cm).build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "folio.logging.feign", name = "enabled", havingValue = "true")
  public FeignLoggerFactory feignLoggerFactory() {
    return FeignInfoLogger::new;
  }

  @Bean
  public Logger.Level feignLoggerLevel(@Value("${folio.logging.feign.level: BASIC}") Logger.Level level) {
    return level;
  }

  public static class FeignInfoLogger extends feign.Logger {

    private final org.slf4j.Logger logger;

    public FeignInfoLogger(Class<?> clazz) {
      this(LoggerFactory.getLogger(clazz));
    }

    FeignInfoLogger(org.slf4j.Logger logger) {
      this.logger = logger;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
      var message = String.format(methodTag(configKey) + format, args);
      logger.debug(message);
    }
  }
}
