package org.folio.spring.provider.impl;

import org.folio.spring.provider.TenantProvider;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class TenantProviderImpl implements TenantProvider {

  private final Set<String> tenants = new ConcurrentSkipListSet<>();

  @Override
  public boolean isExist(String tenant) {
    return tenants.contains(tenant);
  }

  @Override
  public Set<String> getTenants() {
    return tenants;
  }
}
