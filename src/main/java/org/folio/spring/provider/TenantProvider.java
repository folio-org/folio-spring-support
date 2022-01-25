package org.folio.spring.provider;

import java.util.Map;

public interface TenantProvider {
  boolean isExist(String module);
  Map<String, String> getTenants();

}
