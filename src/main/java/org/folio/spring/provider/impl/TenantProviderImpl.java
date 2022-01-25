package org.folio.spring.provider.impl;

import org.folio.spring.provider.TenantProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@Component
public class TenantProviderImpl implements TenantProvider {

  private final Map<String, String> tenants = new ConcurrentSkipListMap<>();

  @Override
  public boolean isExist(String module) {
    return tenants.containsKey(module);
  }

  @Override
  public Map<String, String> getTenants() {
    return tenants;
  }
}
