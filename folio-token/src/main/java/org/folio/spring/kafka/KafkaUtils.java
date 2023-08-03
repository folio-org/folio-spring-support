package org.folio.spring.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.spring.config.properties.FolioEnvironment;
import org.springframework.messaging.MessageHeaders;

@UtilityClass
public class KafkaUtils {

  /**
   * Returns topic name in the format - `{env}.{tenant}.{topic-name}`
   *
   * @param initialName initial topic name as {@link String}
   * @param tenantId    tenant id as {@link String}
   * @return topic name as {@link String} object
   */
  public static String getTenantTopicName(String initialName, String tenantId) {
    return String.join(".", FolioEnvironment.getFolioEnvName(), tenantId, initialName);
  }

  public static List<Header> toKafkaHeaders(Map<String, Collection<String>> requestHeaders) {
    if (requestHeaders == null || requestHeaders.isEmpty()) {
      return Collections.emptyList();
    }
    return requestHeaders.entrySet().stream()
        .map(header -> (Header) new RecordHeader(header.getKey(),
            retrieveFirstSafe(header.getValue()).getBytes(StandardCharsets.UTF_8)))
        .toList();
  }

  public static String getHeaderValue(String headerName, MessageHeaders headers) {
    return headers.entrySet().stream()
        .filter(header -> header.getKey().equalsIgnoreCase(headerName))
        .map(header -> new String((byte[]) header.getValue(), StandardCharsets.UTF_8))
        .findFirst()
        .orElse(null);
  }

  private String retrieveFirstSafe(Collection<String> strings) {
    return strings != null && !strings.isEmpty() ? strings.iterator().next() : "";
  }
}
