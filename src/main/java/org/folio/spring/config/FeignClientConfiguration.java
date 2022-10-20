package org.folio.spring.config;

import feign.Client;
import feign.Logger;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.EnrichUrlAndHeadersClient;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignLoggerFactory;
import org.springframework.cloud.openfeign.clientconfig.OkHttpFeignConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnMissingBean(value = Client.class)
@Import(OkHttpFeignConfiguration.class)
public class FeignClientConfiguration {
  @Bean
  public Client enrichUrlAndHeadersClient(@Autowired FolioExecutionContext folioExecutionContext, @Autowired okhttp3.OkHttpClient okHttpClient) {
    return new EnrichUrlAndHeadersClient(folioExecutionContext, okHttpClient);
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
