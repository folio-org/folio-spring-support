package org.folio.spring.client;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.okhttp.OkHttpClient;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.springframework.http.HttpHeaders;

@Log4j2
public class EnrichUrlAndHeadersClient implements Client {

  private final OkHttpClient delegate;
  private final FolioExecutionContext folioExecutionContext;

  public EnrichUrlAndHeadersClient(FolioExecutionContext folioExecutionContext, okhttp3.OkHttpClient okHttpClient) {
    this.folioExecutionContext = folioExecutionContext;
    this.delegate = new OkHttpClient(okHttpClient);
  }

  @Override
  public Response execute(Request request, Request.Options options) throws IOException {
    String url = prepareUrl(request.url(), folioExecutionContext);

    Map<String, Collection<String>> allHeaders = prepareHeaders(request, folioExecutionContext);

    Request requestWithUrl = Request.create(
      request.httpMethod(),
      url,
      allHeaders,
      request.body(),
      request.charset(),
      request.requestTemplate()
    );

    log.debug(
      "FolioExecutionContext: {};\nPrepared the Feign Client Request: {} with headers {};\nCurrent thread: {}",
      folioExecutionContext,
      requestWithUrl,
      allHeaders,
      Thread.currentThread().getName()
    );

    return delegate.execute(requestWithUrl, options);
  }

  static String prepareUrl(String requestUrl, FolioExecutionContext context) {
    String okapiUrl = context.getOkapiUrl();

    if (okapiUrl == null) {
      return requestUrl;
    }

    okapiUrl = StringUtils.appendIfMissing(okapiUrl, "/");

    return requestUrl.replace("http://", okapiUrl);
  }

  static Map<String, Collection<String>> prepareHeaders(Request request, FolioExecutionContext context) {
    Map<String, Collection<String>> allHeaders = new HashMap<>(request.headers());
    allHeaders.putAll(context.getOkapiHeaders());

    // add accept-language header, if one exists
    context.getAllHeaders().keySet().stream()
      .filter(HttpHeaders.ACCEPT_LANGUAGE::equalsIgnoreCase)
      .findFirst()
      .map(key -> context.getAllHeaders().get(key))
      .ifPresent(values -> allHeaders.put(HttpHeaders.ACCEPT_LANGUAGE, values));

    return allHeaders;
  }
}
