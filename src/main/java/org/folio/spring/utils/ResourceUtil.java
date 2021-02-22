package org.folio.spring.utils;

import java.nio.file.Files;
import java.util.List;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.util.ResourceUtils;

@UtilityClass
public class ResourceUtil {
  @SneakyThrows
  public static List<String> getResourceLines(String path) {
    return Files.readAllLines(ResourceUtils.getFile(path).toPath());
  }
}
