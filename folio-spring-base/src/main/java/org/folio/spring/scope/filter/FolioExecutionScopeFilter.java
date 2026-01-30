package org.folio.spring.scope.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.boot.servlet.filter.OrderedFilter;
import org.springframework.web.filter.GenericFilterBean;

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
    if (request instanceof HttpServletRequest httpServletRequest) {
      try (var x = new FolioExecutionContextSetter(folioModuleMetadata, httpServletRequest)) {
        chain.doFilter(request, response);
      }
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
