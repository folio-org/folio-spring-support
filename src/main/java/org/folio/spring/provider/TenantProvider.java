package org.folio.spring.provider;

import java.util.Set;

public interface TenantProvider {
  boolean isExist(String tenant);
  Set<String> getTenants();

}
