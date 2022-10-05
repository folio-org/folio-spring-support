package org.folio.spring.utils;

import static java.util.Collections.enumeration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.jeasy.random.randomizers.collection.ListRandomizer;
import org.jeasy.random.randomizers.collection.MapRandomizer;
import org.jeasy.random.randomizers.text.StringRandomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class RequestUtilsTest {

  @Mock
  private RequestAttributes requestAttributes;
  @Mock
  private HttpServletRequest request;
  @InjectMocks
  private ServletRequestAttributes servletRequestAttributes;

  @Test
  void shouldReturnServletRequestFromServletRequestAttributes() {
    try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
      mockedHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      HttpServletRequest result = RequestUtils.getHttpServletRequest();

      assertEquals(request, result);
    }
  }

  @Test
  void shouldReturnNullIfNotServletRequestAttributes() {
    try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
      mockedHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);

      HttpServletRequest result = RequestUtils.getHttpServletRequest();

      assertNull(result);
    }
  }

  @Test
  void shouldReturnEmptyHeadersIfRequestIsNull() {
    try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
      mockedHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(requestAttributes);

      Map<String, Collection<String>> result = RequestUtils.getHttpHeadersFromRequest();

      assertThat(result).isEmpty();
    }
  }

  @Test
  void shouldReturnHeadersFromRequest() {
    Map<String, Collection<String>> headers = randomHeaders();

    testHeadersExtraction(headers, keysToLowercase(headers));
  }

  @Test
  void shouldReturnCombinedValuesForSameHeaderFromRequest() {
    String key = "someHeader";

    Map<String, Collection<String>> headers = Map.of(
        key, List.of(randomString()),
        key.toLowerCase(), List.of(randomString())
    );

    testHeadersExtraction(headers, Map.of(key.toLowerCase(), flattenValues(headers)));
  }

  private void testHeadersExtraction(Map<String, Collection<String>> headersInRequest,
      Map<String, Collection<String>> expected) {

    try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
      mockedHolder.when(RequestContextHolder::getRequestAttributes).thenReturn(servletRequestAttributes);

      when(request.getHeaderNames()).thenReturn(enumeration(headersInRequest.keySet()));
      when(request.getHeaders(anyString())).then(invocation -> {
        String h = invocation.getArgument(0);
        return enumeration(headersInRequest.get(h));
      });

      Map<String, Collection<String>> result = RequestUtils.getHttpHeadersFromRequest();

      assertThat(result).isEqualTo(expected);
    }
  }

  private static String randomString() {
    return new StringRandomizer().getRandomValue();
  }

  private static Map<String, Collection<String>> randomHeaders() {
    MapRandomizer<String, Collection<String>> headerRandomizer = new MapRandomizer<>(
        new StringRandomizer(),
        new ListRandomizer<>(new StringRandomizer())
    );

    return headerRandomizer.getRandomValue();
  }

  private static List<String> flattenValues(Map<String, Collection<String>> headers) {
    return headers.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
  }

  private static Map<String, Collection<String>> keysToLowercase(Map<String, Collection<String>> headers) {
    Map<String, Collection<String>> result = new HashMap<>();

    headers.forEach((key, values) -> result.merge(key.toLowerCase(), values, (existing, added) -> {
      List<String> combined = new ArrayList<>(existing.size() + added.size());

      combined.addAll(existing);
      combined.addAll(added);

      return combined;
    }));

    return result;
  }
}