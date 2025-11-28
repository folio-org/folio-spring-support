package org.folio.spring.client;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Strings;
import org.folio.spring.FolioExecutionContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

@Log4j2
@RequiredArgsConstructor
public class EnrichUrlAndHeadersInterceptor implements ClientHttpRequestInterceptor {

  public static final Pattern SCHEMA_PATTERN = Pattern.compile("https?://");
  private final FolioExecutionContext folioExecutionContext;

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
    throws IOException {

    var uri = prepareUrl(request.getURI().toString(), folioExecutionContext);
    var headers = prepareHeaders(request.getHeaders(), folioExecutionContext);

    var modifiedRequest = new CustomHttpRequestWrapper(request, URI.create(uri), headers);

    log.debug(
      "FolioExecutionContext: {};\nPrepared HTTP Request: {} with headers {};\nCurrent thread: {}",
      folioExecutionContext,
      modifiedRequest.getURI(),
      headers,
      Thread.currentThread().getName()
    );

    return execution.execute(modifiedRequest, body);
  }

  static String prepareUrl(String requestUrl, FolioExecutionContext context) {
    var okapiUrl = context.getOkapiUrl();

    if (okapiUrl == null) {
      return requestUrl;
    }

    okapiUrl = Strings.CS.appendIfMissing(okapiUrl, "/");

    if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
      return SCHEMA_PATTERN.matcher(requestUrl).replaceFirst(okapiUrl);
    } else {
      return okapiUrl + requestUrl;
    }
  }

  static HttpHeaders prepareHeaders(HttpHeaders requestHeaders, FolioExecutionContext context) {
    var allHeaders = new HttpHeaders();
    allHeaders.addAll(requestHeaders);

    var okapiHeaders = context.getOkapiHeaders();
    if (okapiHeaders != null) {
      okapiHeaders.forEach((key, values) ->
        allHeaders.put(key, values.stream().toList())
      );
    }

    var allContextHeaders = context.getAllHeaders();
    if (allContextHeaders != null) {
      allContextHeaders.keySet().stream()
        .filter(HttpHeaders.ACCEPT_LANGUAGE::equalsIgnoreCase)
        .findFirst()
        .map(allContextHeaders::get)
        .ifPresent(values -> allHeaders.put(HttpHeaders.ACCEPT_LANGUAGE, values.stream().toList()));
    }

    return allHeaders;
  }

  private static class CustomHttpRequestWrapper extends HttpRequestWrapper {
    private final URI uri;
    private final HttpHeaders headers;

    CustomHttpRequestWrapper(HttpRequest request, URI uri, HttpHeaders headers) {
      super(request);
      this.uri = uri;
      this.headers = headers;
    }

    @Override
    public URI getURI() {
      return uri;
    }

    @Override
    public HttpHeaders getHeaders() {
      return headers;
    }
  }
}
