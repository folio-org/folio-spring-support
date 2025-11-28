package org.folio.spring.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import org.folio.spring.testing.type.UnitTest;
import org.folio.spring.utils.RequestLoggingLevel;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

@UnitTest
class ExchangeLoggingInterceptorTest {

  @Test
  void shouldLogRequestAndResponse() throws IOException {
    ExchangeLoggingInterceptor interceptor = new ExchangeLoggingInterceptor(RequestLoggingLevel.FULL);
    
    HttpRequest request = mock(HttpRequest.class);
    when(request.getURI()).thenReturn(URI.create("http://test.com/api"));
    when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
    when(request.getHeaders()).thenReturn(new HttpHeaders());
    
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    when(response.getStatusText()).thenReturn("OK");
    when(response.getHeaders()).thenReturn(new HttpHeaders());
    when(response.getBody()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
    
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    when(execution.execute(any(), any())).thenReturn(response);
    
    byte[] body = "test body".getBytes();
    ClientHttpResponse result = interceptor.intercept(request, body, execution);
    
    assertThat(result).isEqualTo(response);
  }

  @Test
  void shouldHandleEmptyRequestBody() throws IOException {
    ExchangeLoggingInterceptor interceptor = new ExchangeLoggingInterceptor(RequestLoggingLevel.FULL);
    
    HttpRequest request = mock(HttpRequest.class);
    when(request.getURI()).thenReturn(URI.create("http://test.com/api"));
    when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.GET);
    when(request.getHeaders()).thenReturn(new HttpHeaders());
    
    ClientHttpResponse response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    when(response.getStatusText()).thenReturn("OK");
    when(response.getHeaders()).thenReturn(new HttpHeaders());
    when(response.getBody()).thenReturn(new ByteArrayInputStream(new byte[0]));
    
    ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    when(execution.execute(any(), any())).thenReturn(response);
    
    byte[] body = new byte[0];
    ClientHttpResponse result = interceptor.intercept(request, body, execution);
    
    assertThat(result).isEqualTo(response);
  }
}
