package org.folio.spring.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@UnitTest
class EnrichUrlAndHeadersInterceptorTest {

  static List<Arguments> urlPreparationCases() {
    return List.of(
      arguments("http://test-url", null, "http://test-url"),
      arguments("http://test-url", "http://okapi", "http://okapi/test-url"),
      arguments("http://test-url", "http://okapi/", "http://okapi/test-url"),
      arguments("https://test-url", "http://okapi/", "http://okapi/test-url"),
      arguments("test-url", "http://okapi/", "http://okapi/test-url")
    );
  }

  @ParameterizedTest
  @MethodSource("urlPreparationCases")
  void testUrlPreparation(String requestUrl, String okapiUrl, String expected) {
    FolioExecutionContext context = mock(FolioExecutionContext.class);

    when(context.getOkapiUrl()).thenReturn(okapiUrl);
    assertThat(EnrichUrlAndHeadersInterceptor.prepareUrl(requestUrl, context), is(expected));
  }

  @Test
  void testHeaderPreparationWithNoLanguage() {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.put("a", List.of("a-val"));
    requestHeaders.put("b", List.of("b-val"));
    requestHeaders.put("c", List.of("c-val"));

    FolioExecutionContext context = mock(FolioExecutionContext.class);
    when(context.getOkapiHeaders()).thenReturn(Map.of("z", List.of("z-val")));
    when(context.getAllHeaders()).thenReturn(Map.of("misc-1", List.of("misc-1-val"), "misc-2", List.of("misc-2-val")));

    HttpHeaders result = EnrichUrlAndHeadersInterceptor.prepareHeaders(requestHeaders, context);

    assertThat(result.get("a"), is(List.of("a-val")));
    assertThat(result.get("b"), is(List.of("b-val")));
    assertThat(result.get("c"), is(List.of("c-val")));
    assertThat(result.get("z"), is(List.of("z-val")));
    assertThat(result.size(), is(4));
  }

  @Test
  void testHeaderPreparationWithLanguage() {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.put("a", List.of("a-val"));
    requestHeaders.put("b", List.of("b-val"));
    requestHeaders.put("c", List.of("c-val"));

    FolioExecutionContext context = mock(FolioExecutionContext.class);
    when(context.getOkapiHeaders()).thenReturn(Map.of("z", List.of("z-val")));
    when(context.getAllHeaders())
      .thenReturn(
        Map.of(
          "misc-1", List.of("misc-1-val"),
          "accept-language", List.of("en-US,en;q=0.9"),
          "misc-2", List.of("misc-2-val")
        )
      );

    HttpHeaders result = EnrichUrlAndHeadersInterceptor.prepareHeaders(requestHeaders, context);

    assertThat(result.get("a"), is(List.of("a-val")));
    assertThat(result.get("b"), is(List.of("b-val")));
    assertThat(result.get("c"), is(List.of("c-val")));
    assertThat(result.get("z"), is(List.of("z-val")));
    assertThat(result.get("Accept-Language"), is(List.of("en-US,en;q=0.9")));
    assertThat(result.size(), is(5));
  }

  @Test
  void testInterceptExecution() throws IOException {
    HttpRequest request = mock(HttpRequest.class);
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.put("a", List.of("a-val"));
    
    when(request.getURI()).thenReturn(URI.create("http://test-url"));
    when(request.getHeaders()).thenReturn(requestHeaders);
    when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);

    FolioExecutionContext context = mock(FolioExecutionContext.class);
    when(context.getOkapiUrl()).thenReturn("http://okapi");
    when(context.getOkapiHeaders()).thenReturn(Map.of("z", List.of("z-val")));
    when(context.getAllHeaders())
      .thenReturn(
        Map.of(
          "misc-1", List.of("misc-1-val"),
          "accept-language", List.of("en-US,en;q=0.9"),
          "misc-2", List.of("misc-2-val")
        )
      );

    byte[] body = "test-data".getBytes();
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    
    when(execution.execute(any(HttpRequest.class), eq(body))).thenReturn(response);

    EnrichUrlAndHeadersInterceptor interceptor = new EnrichUrlAndHeadersInterceptor(context);
    ClientHttpResponse result = interceptor.intercept(request, body, execution);

    assertThat(result, is(response));

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(execution, times(1)).execute(requestCaptor.capture(), eq(body));

    HttpRequest capturedRequest = requestCaptor.getValue();
    assertThat(capturedRequest.getURI().toString(), is("http://okapi/test-url"));
    assertThat(capturedRequest.getHeaders().get("a"), is(List.of("a-val")));
    assertThat(capturedRequest.getHeaders().get("z"), is(List.of("z-val")));
    assertThat(capturedRequest.getHeaders().get("Accept-Language"), is(List.of("en-US,en;q=0.9")));
  }
}
