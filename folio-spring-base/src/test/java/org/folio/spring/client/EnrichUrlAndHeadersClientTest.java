package org.folio.spring.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.Request;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class EnrichUrlAndHeadersClientTest {

  static List<Arguments> urlPreparationCases() {
    return Arrays.asList(
      arguments("http://test-url", null, "http://test-url"),
      arguments("http://test-url", "http://okapi", "http://okapi/test-url"),
      arguments("http://test-url", "http://okapi/", "http://okapi/test-url")
    );
  }

  @ParameterizedTest
  @MethodSource("urlPreparationCases")
  void testUrlPreparation(String requestUrl, String okapiUrl, String expected) {
    FolioExecutionContext context = mock(FolioExecutionContext.class);

    when(context.getOkapiUrl()).thenReturn(okapiUrl);
    assertThat(
      EnrichUrlAndHeadersClient.prepareUrl(requestUrl, context),
      is(expected)
    );
  }

  @Test
  void testHeaderPreparationWithNoLanguage() {
    Request request = mock(Request.class);
    when(request.headers())
      .thenReturn(
        Map.of(
          "a",
          List.of("a-val"),
          "b",
          List.of("b-val"),
          "c",
          List.of("c-val")
        )
      );

    FolioExecutionContext context = mock(FolioExecutionContext.class);
    when(context.getOkapiHeaders()).thenReturn(Map.of("z", List.of("z-val")));
    when(context.getAllHeaders())
      .thenReturn(
        Map.of("misc-1", List.of("misc-1-val"), "misc-2", List.of("misc-2-val"))
      );

    assertThat(
      EnrichUrlAndHeadersClient.prepareHeaders(request, context),
      is(
        allOf(
          hasEntry("a", List.of("a-val")),
          hasEntry("b", List.of("b-val")),
          hasEntry("c", List.of("c-val")),
          hasEntry("z", List.of("z-val")),
          is(aMapWithSize(4))
        )
      )
    );
  }

  @Test
  void testHeaderPreparationWithLanguage() {
    Request request = mock(Request.class);
    when(request.headers())
      .thenReturn(
        Map.of(
          "a",
          List.of("a-val"),
          "b",
          List.of("b-val"),
          "c",
          List.of("c-val")
        )
      );

    FolioExecutionContext context = mock(FolioExecutionContext.class);
    when(context.getOkapiHeaders()).thenReturn(Map.of("z", List.of("z-val")));
    when(context.getAllHeaders())
      .thenReturn(
        Map.of(
          "misc-1",
          List.of("misc-1-val"),
          "accept-language",
          List.of("en-US,en;q=0.9"),
          "misc-2",
          List.of("misc-2-val")
        )
      );

    assertThat(
      EnrichUrlAndHeadersClient.prepareHeaders(request, context),
      is(
        allOf(
          hasEntry("a", List.of("a-val")),
          hasEntry("b", List.of("b-val")),
          hasEntry("c", List.of("c-val")),
          hasEntry("z", List.of("z-val")),
          hasEntry("Accept-Language", List.of("en-US,en;q=0.9")),
          is(aMapWithSize(5))
        )
      )
    );
  }
}
