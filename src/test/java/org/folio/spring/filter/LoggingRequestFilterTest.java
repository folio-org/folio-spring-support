package org.folio.spring.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.boot.web.servlet.filter.OrderedFilter.REQUEST_WRAPPER_FILTER_MAX_ORDER;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.DelegatingServletInputStream;

import org.folio.spring.filter.appender.TestAppender;
import org.folio.spring.integration.XOkapiHeaders;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoggingRequestFilterTest {

  private static final String TEST_METHOD = "GET";
  private static final String TEST_URI = "/test-uri";
  private static final String TEST_QUERY = "param=true";
  private static final String TEST_REQUEST_ID = "10101";
  private static final int TEST_STATUS = 200;
  private static final String TEST_TOKEN = "test-token";
  private static final String TEST_BODY = "test-body";

  @Mock
  private HttpServletRequest servletRequest;
  @Mock
  private HttpServletResponse servletResponse;
  @Mock
  private FilterChain filterChain;

  private TestAppender testAppender;

  @BeforeEach
  void setUp() throws IOException {
    Map<String, String> testHeaders = new HashMap<>();
    testHeaders.put(XOkapiHeaders.REQUEST_ID, TEST_REQUEST_ID);
    testHeaders.put(XOkapiHeaders.TOKEN, TEST_TOKEN);

    when(servletRequest.getAttribute(anyString())).thenReturn(Instant.now().toEpochMilli());
    when(servletRequest.getMethod()).thenReturn(TEST_METHOD);
    when(servletRequest.getRequestURI()).thenReturn(TEST_URI);
    when(servletRequest.getQueryString()).thenReturn(TEST_QUERY);
    when(servletRequest.getHeaderNames()).thenReturn(Collections.enumeration(testHeaders.keySet()));
    when(servletRequest.getHeader(XOkapiHeaders.TOKEN)).thenReturn(TEST_TOKEN);
    when(servletRequest.getHeader(XOkapiHeaders.REQUEST_ID)).thenReturn(TEST_REQUEST_ID);
    when(servletRequest.getInputStream())
      .thenReturn(new DelegatingServletInputStream(new ByteArrayInputStream(TEST_BODY.getBytes())));
    when(servletRequest.getCharacterEncoding()).thenReturn("UTF-8");
    when(servletResponse.getCharacterEncoding()).thenReturn("UTF-8");
    when(servletResponse.getStatus()).thenReturn(TEST_STATUS);

    var ctx = (LoggerContext) LogManager.getContext(false);
    var config = ctx.getConfiguration();
    testAppender = (TestAppender) config.getAppenders().get("TestAppender");
    testAppender.clearMessages();
    setLoggerLevel(ctx, Level.INFO);
  }

  @Test
  void loggingTestWithNotInfoLevel() throws ServletException, IOException {
    setLoggerLevel((LoggerContext) LogManager.getContext(false), Level.FATAL);
    var filter = new LoggingRequestFilter(LoggingRequestFilter.Level.BASIC);
    filter.doFilter(servletRequest, servletResponse, filterChain);
    assertThat(testAppender.getMessages()).isEmpty();
  }

  @Test
  void basicLoggingTest() throws ServletException, IOException {
    var filter = new LoggingRequestFilter(LoggingRequestFilter.Level.BASIC);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    assertThat(testAppender.getMessages())
      .hasSize(2)
      .extracting(logEvent -> logEvent.getMessage().getFormattedMessage())
      .satisfies(requestInfo(), atIndex(0))
      .satisfies(responseStatusWithTime(), atIndex(1));
  }

  @Test
  void headersLoggingTest() throws ServletException, IOException {
    var filter = new LoggingRequestFilter(LoggingRequestFilter.Level.HEADERS);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    assertThat(testAppender.getMessages())
      .hasSize(5)
      .extracting(logEvent -> logEvent.getMessage().getFormattedMessage())
      .satisfies(requestInfo(), atIndex(0))
      .satisfies(requestHeader(XOkapiHeaders.TOKEN, TEST_TOKEN), atIndex(1))
      .satisfies(requestHeader(XOkapiHeaders.REQUEST_ID, TEST_REQUEST_ID), atIndex(2))
      .satisfies(requestEndHttp(), atIndex(3))
      .satisfies(responseStatusWithTime(), atIndex(4));
  }

  @Test
  void fullLoggingTest() throws ServletException, IOException {
    var filter = new LoggingRequestFilter(LoggingRequestFilter.Level.FULL);
    filter.doFilter(servletRequest, servletResponse, filterChain);

    assertThat(testAppender.getMessages())
      .hasSize(8)
      .extracting(logEvent -> logEvent.getMessage().getFormattedMessage())
      .satisfies(requestInfo(), atIndex(0))
      .satisfies(requestHeader(XOkapiHeaders.TOKEN, TEST_TOKEN), atIndex(1))
      .satisfies(requestHeader(XOkapiHeaders.REQUEST_ID, TEST_REQUEST_ID), atIndex(2))
      .satisfies(body(TEST_BODY), atIndex(3))
      .satisfies(requestEndHttp(), atIndex(4))
      .satisfies(responseStatusWithTime(), atIndex(5))
      .satisfies(body(), atIndex(6))
      .satisfies(responseEndHttp(), atIndex(7));
  }

  @Test
  void getOrderTest() {
    var filter = new LoggingRequestFilter(LoggingRequestFilter.Level.FULL);
    var actualOrder = filter.getOrder();

    assertThat(actualOrder).isEqualTo(REQUEST_WRAPPER_FILTER_MAX_ORDER + 3);
  }

  @Test
  void setOrderTest() {
    var filter = new LoggingRequestFilter(LoggingRequestFilter.Level.FULL);
    filter.setOrder(0);
    var actualOrder = filter.getOrder();

    assertThat(actualOrder).isZero();
  }

  private Consumer<String> requestInfo() {
    return s -> assertThat(s).isEqualTo("---> " + TEST_METHOD + " " + TEST_URI + " " + TEST_QUERY);
  }

  private Consumer<String> requestHeader(String headerName, String headerValue) {
    return s -> assertThat(s).isEqualTo(headerName + ": " + headerValue);
  }

  private Consumer<String> responseStatusWithTime() {
    return s -> assertThat(s).matches("<--- " + TEST_STATUS + " in \\d+ms");
  }

  private Consumer<String> body() {
    return body(null);
  }

  private Consumer<String> body(String testBody) {
    return s -> assertThat(s).isEqualTo("Body: " + (testBody == null ? "" : testBody));
  }

  private Consumer<String> requestEndHttp() {
    return s -> assertThat(s).isEqualTo("---> END HTTP");
  }

  private Consumer<String> responseEndHttp() {
    return s -> assertThat(s).isEqualTo("<--- END HTTP");
  }

  private void setLoggerLevel(LoggerContext ctx, Level info) {
    ctx.getLogger(LoggingRequestFilter.class.getName()).setLevel(info);
  }
}