package org.folio.spring.scope.filter;

import static org.folio.spring.utils.RequestUtils.getHttpHeadersFromRequest;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.web.filter.GenericFilterBean;

import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;

@Log4j2
public class FolioExecutionScopeFilter extends GenericFilterBean implements OrderedFilter {

  private final FolioModuleMetadata folioModuleMetadata;

  private int order = REQUEST_WRAPPER_FILTER_MAX_ORDER + 2;

  public FolioExecutionScopeFilter(FolioModuleMetadata folioModuleMetadata) {
    this.folioModuleMetadata = folioModuleMetadata;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
    if (request instanceof HttpServletRequest) {
      var httpHeaders = getHttpHeadersFromRequest((HttpServletRequest) request);
      var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, httpHeaders);
      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
      try {
        chain.doFilter(request, response);
      } finally {
        FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
      }
    }
  }

  @Override
  public int getOrder() {
    return order;
  }

  private void setOrder(int order) {
    this.order = order;
  }
}
