package org.folio.spring.utils;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.util.LinkedCaseInsensitiveMap;

@Log4j2
@UtilityClass
public class FolioExecutionContextUtils {
  public static FolioExecutionContext prepareContextForTenant(String tenantId,
                                                              FolioModuleMetadata folioModuleMetadata,
                                                              FolioExecutionContext context) {
    if (MapUtils.isNotEmpty(context.getAllHeaders())) {
      var headersCopy = caseInsensitiveCopyOf(context.getAllHeaders());
      headersCopy.put(TENANT, List.of(tenantId));
      log.debug("FOLIO context initialized with tenant {}", tenantId);
      return new DefaultFolioExecutionContext(folioModuleMetadata, headersCopy);
    }
    throw new IllegalStateException("Okapi headers not provided");
  }

  public static <V> LinkedCaseInsensitiveMap<V> caseInsensitiveCopyOf(Map<String, V> source) {
    var result = new LinkedCaseInsensitiveMap<V>(Locale.ROOT);
    if (source != null) {
      result.putAll(source);
    }
    return result;
  }
}
