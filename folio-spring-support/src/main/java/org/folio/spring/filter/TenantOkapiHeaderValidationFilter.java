package org.folio.spring.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component("defaultTenantOkapiHeaderValidationFilter")
@ConditionalOnMissingBean(name = "tenantOkapiHeaderValidationFilter")
@ConditionalOnProperty(prefix = "folio.tenant.validation", name = "enabled", matchIfMissing = true)
public class TenantOkapiHeaderValidationFilter extends GenericFilterBean implements OrderedFilter {

  public static final String ERROR_MSG = XOkapiHeaders.TENANT + " header must be provided";

  private int order = REQUEST_WRAPPER_FILTER_MAX_ORDER + 1;

  @Value("${header.validation.x-okapi-tenant.exclude.base-paths:/admin}")
  private String[] excludeBasePaths;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    if (StringUtils.isNotBlank(req.getHeader(XOkapiHeaders.TENANT)) || Arrays.stream(excludeBasePaths)
      .anyMatch(req.getRequestURI()::startsWith)) {
      chain.doFilter(request, response);
    } else {
      logger.warn(ERROR_MSG);
      var res = (HttpServletResponse) response;
      res.setContentType("text/plain");
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().println(ERROR_MSG);
    }
  }

  @Override
  public int getOrder() {
    return order;
  }

  public void setOrder(int order) {
    this.order = order;
  }
}
