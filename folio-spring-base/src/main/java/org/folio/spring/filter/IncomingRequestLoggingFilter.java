package org.folio.spring.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.spring.utils.MultiReadHttpServletRequestWrapper;
import org.folio.spring.utils.RequestLoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.servlet.filter.OrderedFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Log4j2
@Component
@ConditionalOnProperty(
  prefix = "folio.logging.request",
  name = "enabled",
  havingValue = "true"
)
public class IncomingRequestLoggingFilter extends GenericFilterBean implements OrderedFilter {

  private static final String START_TIME_ATTR = "startTime";

  private final RequestLoggingLevel level;

  public IncomingRequestLoggingFilter(@Value("${folio.logging.request.level: BASIC}") RequestLoggingLevel level) {
    this.level = level;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
    if (log.isDebugEnabled()) {
      filterWrapped(wrapRequest(request), wrapResponse(response), chain);
    } else {
      chain.doFilter(request, response);
    }
  }

  @Override
  public int getOrder() {
    return REQUEST_WRAPPER_FILTER_MAX_ORDER + 3;
  }

  private void filterWrapped(MultiReadHttpServletRequestWrapper request, ContentCachingResponseWrapper response,
                             FilterChain chain) throws ServletException, IOException {
    filterBefore(request);
    chain.doFilter(request, response);
    filterAfter(request, response);
    response.copyBodyToResponse();
  }

  private void filterBefore(MultiReadHttpServletRequestWrapper request) throws IOException {
    request.setAttribute(START_TIME_ATTR, Instant.now().toEpochMilli());

    log.debug("---> {} {} {}",
      request.getMethod(),
      request.getRequestURI(),
      request.getQueryString()
    );

    if (level.ordinal() >= RequestLoggingLevel.HEADERS.ordinal()) {
      var headerNames = request.getHeaderNames();
      while (headerNames.hasMoreElements()) {
        var headerName = headerNames.nextElement();
        log.debug("{}: {}", headerName, request.getHeader(headerName));
      }
    }

    if (level.ordinal() == RequestLoggingLevel.FULL.ordinal()) {
      var body = IOUtils.toString(request.getInputStream(), request.getCharacterEncoding());
      log.debug("Request body: {}", body);
    }

    if (level.ordinal() > RequestLoggingLevel.BASIC.ordinal()) {
      log.debug("---> END HTTP");
    }
  }

  private void filterAfter(MultiReadHttpServletRequestWrapper request, ContentCachingResponseWrapper response)
    throws UnsupportedEncodingException {
    var startTime = (long) request.getAttribute(START_TIME_ATTR);

    log.debug("<--- {} in {}ms",
      response.getStatus(),
      Instant.now().toEpochMilli() - startTime
    );

    if (level.ordinal() == RequestLoggingLevel.FULL.ordinal()) {
      var body = new String(response.getContentAsByteArray(), response.getCharacterEncoding());
      log.debug("Response body: {}", body);
      log.debug("<--- END HTTP");
    }
  }

  private ContentCachingResponseWrapper wrapResponse(ServletResponse response) {
    return new ContentCachingResponseWrapper((HttpServletResponse) response);
  }

  private MultiReadHttpServletRequestWrapper wrapRequest(ServletRequest request) {
    return new MultiReadHttpServletRequestWrapper((HttpServletRequest) request);
  }
}
