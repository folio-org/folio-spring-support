package org.folio.spring.config;

import feign.Client;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.client.EnrichUrlAndHeadersClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

}
