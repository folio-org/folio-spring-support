package org.folio.spring.utils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@UtilityClass
public class RequestUtils {

  public static HttpServletRequest getHttpServletRequest() {
    var requestAttributes = RequestContextHolder.getRequestAttributes();
    return (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes)
      ? servletRequestAttributes.getRequest() : null;
  }

  public static Map<String, Collection<String>> getHttpHeadersFromRequest() {
    return getHttpHeadersFromRequest(getHttpServletRequest());
  }

  public static Map<String, Collection<String>> getHttpHeadersFromRequest(HttpServletRequest request) {
    return request != null
      ? Collections.list(request.getHeaderNames()).stream()
          .collect(Collectors.toMap(
            String::toLowerCase, h -> Collections.list(request.getHeaders(h)), (a, b) -> {
              a.addAll(b);
              return a;
            }))
      : Collections.emptyMap();
  }

}
