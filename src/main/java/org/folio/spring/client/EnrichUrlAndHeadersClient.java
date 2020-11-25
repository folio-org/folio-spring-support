package org.folio.spring.client;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.okhttp.OkHttpClient;
import org.folio.spring.FolioExecutionContext;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class EnrichUrlAndHeadersClient implements Client {
  private final OkHttpClient delegate;
  private final FolioExecutionContext folioExecutionContext;

  public EnrichUrlAndHeadersClient(FolioExecutionContext folioExecutionContext, okhttp3.OkHttpClient okHttpClient) {
    this.folioExecutionContext = folioExecutionContext;
    this.delegate = new OkHttpClient(okHttpClient);
  }

  @Override
  public Response execute(Request request, Request.Options options) throws IOException {
    String url;

    var okapiUrl = folioExecutionContext.getOkapiUrl();
    if (okapiUrl != null) {
      if (!okapiUrl.endsWith("/")) {
        okapiUrl += "/";
      }
      url = request.url().replace("http://", okapiUrl);
    } else {
      url = request.url();
    }

    Map<String, Collection<String>> allHeaders = new HashMap<>(request.headers());
    allHeaders.putAll(folioExecutionContext.getOkapiHeaders());

    var requestWithURL = Request.create(request.httpMethod(), url, allHeaders, request.body(), request.charset(), request.requestTemplate());
    return delegate.execute(requestWithURL, options);
  }
}
