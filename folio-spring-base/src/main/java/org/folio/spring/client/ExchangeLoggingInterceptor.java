package org.folio.spring.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.utils.RequestLoggingLevel;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

@Log4j2
@RequiredArgsConstructor
public class ExchangeLoggingInterceptor implements ClientHttpRequestInterceptor {

  private final RequestLoggingLevel level;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
    throws IOException {

    logRequest(request, body);

    long startTime = System.currentTimeMillis();
    ClientHttpResponse response = execution.execute(request, body);
    long duration = System.currentTimeMillis() - startTime;

    logResponse(request, response, duration);

    return response;
  }

  private void logRequest(HttpRequest request, byte[] body) {
    if (log.isDebugEnabled()) {
      log.debug("===========================Request Begin===========================");
      log.debug("URI         : {}", request.getURI());
      log.debug("Method      : {}", request.getMethod());
      if (level.ordinal() >= RequestLoggingLevel.HEADERS.ordinal()) {
        log.debug("Headers     : {}", request.getHeaders());
      }
      if (level.ordinal() == RequestLoggingLevel.FULL.ordinal() && body.length > 0) {
        log.debug("Request body: {}", new String(body, StandardCharsets.UTF_8));
      }
      log.debug("===========================Request End=============================");
    }
  }

  private void logResponse(HttpRequest request, ClientHttpResponse response, long duration) throws IOException {
    if (log.isDebugEnabled()) {
      log.debug("===========================Response Begin==========================");
      log.debug("URI          : {}", request.getURI());
      log.debug("Status code  : {} ({})", response.getStatusCode(), response.getStatusText());
      if (level.ordinal() >= RequestLoggingLevel.HEADERS.ordinal()) {
        log.debug("Headers      : {}", response.getHeaders());
      }
      log.debug("Duration     : {} ms", duration);

      if (level.ordinal() == RequestLoggingLevel.FULL.ordinal()) {
        byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
        if (responseBody.length > 0) {
          log.debug("Response body: {}", new String(responseBody, StandardCharsets.UTF_8));
        }
      }
      log.debug("===========================Response End============================");
    }
  }
}
