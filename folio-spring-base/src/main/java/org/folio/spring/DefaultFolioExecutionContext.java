package org.folio.spring;

import static org.folio.spring.integration.XOkapiHeaders.OKAPI_HEADERS_PREFIX;
import static org.folio.spring.integration.XOkapiHeaders.REQUEST_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.TOKEN;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.spring.utils.FolioExecutionContextUtils.caseInsensitiveCopyOf;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.LinkedCaseInsensitiveMap;

@Getter
@ToString
public class DefaultFolioExecutionContext implements FolioExecutionContext {

  private final FolioModuleMetadata folioModuleMetadata;
  private final Map<String, Collection<String>> allHeaders;
  private final Map<String, Collection<String>> okapiHeaders;

  private final String tenantId;
  private final String okapiUrl;
  private final String token;
  private final UUID userId;
  private final String requestId;

  public DefaultFolioExecutionContext(FolioModuleMetadata folioModuleMetadata,
    Map<String, Collection<String>> allHeaders) {
    this.folioModuleMetadata = folioModuleMetadata;
    this.allHeaders = caseInsensitiveCopyOf(allHeaders);
    this.okapiHeaders = caseInsensitiveCopyOf(allHeaders);
    this.okapiHeaders.entrySet().removeIf(e -> !e.getKey().toLowerCase(Locale.ROOT).startsWith(OKAPI_HEADERS_PREFIX));

    this.tenantId = retrieveFirstSafe(okapiHeaders.get(TENANT));
    this.okapiUrl = retrieveFirstSafe(okapiHeaders.get(URL));
    this.token = retrieveFirstSafe(okapiHeaders.get(TOKEN));
    this.requestId = retrieveFirstSafe(okapiHeaders.get(REQUEST_ID));

    var userIdString = retrieveFirstSafe(okapiHeaders.get(USER_ID));
    this.userId = userIdString.isEmpty() ? null : UUID.fromString(userIdString);
  }

  public static DefaultFolioExecutionContext fromMessageHeaders(FolioModuleMetadata folioModuleMetadata,
    Map<String, Object> messageHeaders) {
    return new DefaultFolioExecutionContext(folioModuleMetadata, toOkapiHeaders(messageHeaders));
  }

  private static Map<String, Collection<String>> toOkapiHeaders(Map<String, Object> messageHeaders) {
    return messageHeaders.entrySet()
      .stream()
      .filter(e -> e.getKey().toLowerCase(Locale.ROOT).startsWith(OKAPI_HEADERS_PREFIX))
      .collect(Collectors.toMap(Map.Entry::getKey, e -> {
        Object x = e.getValue();
        if (x instanceof byte[] bytes) {
          return List.of(new String(bytes, StandardCharsets.UTF_8));
        }
        return List.of(String.valueOf(x));
      }, (s, s2) -> s, () -> new LinkedCaseInsensitiveMap<>(Locale.ROOT)));
  }

  private static String retrieveFirstSafe(Collection<String> strings) {
    return strings != null && !strings.isEmpty() ? strings.iterator().next() : "";
  }
}
