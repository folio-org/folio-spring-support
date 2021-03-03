package org.folio.spring.filter;

import org.apache.commons.lang3.StringUtils;
import org.folio.spring.integration.XOkapiHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component("defaultTenantOkapiHeaderValidationFilter")
@ConditionalOnMissingBean(name = "tenantOkapiHeaderValidationFilter")
@ConditionalOnProperty(prefix = "folio.tenant.validation", name = "enabled", matchIfMissing = true)
public class TenantOkapiHeaderValidationFilter extends GenericFilterBean {

  public static final String ERROR_MSG = XOkapiHeaders.TENANT + " header must be provided";

  @Value("${header.validation.x-okapi-tenant.exclude.base-paths:/admin}")
  private String[] excludeBasePaths;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    for (String basePath : excludeBasePaths) {
      if (req.getRequestURI().startsWith(basePath)) {
        chain.doFilter(request, response);
        return;
      }
    }
    if (StringUtils.isBlank(req.getHeader(XOkapiHeaders.TENANT))) {
      var res = (HttpServletResponse) response;
      res.setContentType("text/plain");
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().println(ERROR_MSG);
    } else {
      chain.doFilter(request, response);
    }
  }
}
