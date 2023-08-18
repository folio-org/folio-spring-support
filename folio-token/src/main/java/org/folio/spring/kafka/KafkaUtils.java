package org.folio.spring.kafka;

import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.springframework.messaging.MessageHeaders;

@UtilityClass
public class KafkaUtils {

  public static String getHeaderValue(String headerName, MessageHeaders headers) {
    return headers.entrySet().stream()
        .filter(header -> header.getKey().equalsIgnoreCase(headerName))
        .map(header -> new String((byte[]) header.getValue(), StandardCharsets.UTF_8))
        .findFirst()
        .orElse(null);
  }
}
