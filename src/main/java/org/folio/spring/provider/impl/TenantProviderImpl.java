package org.folio.spring.provider.impl;

import org.folio.spring.provider.TenantProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@Component
public class TenantProviderImpl implements TenantProvider {

  private final Map<String, String> tenants = new ConcurrentSkipListMap<>();

  @Override
  public void add(String module, String tenant) {
    if (notExist(module)) {
      tenants.put(module, tenant);
    }
  }

  @Override
  public boolean remove(String module, String tenant) {
    return tenants.remove(module, tenant);
  }

  @Override
  public boolean notExist(String module) {
    return !tenants.containsKey(module);
  }
}
