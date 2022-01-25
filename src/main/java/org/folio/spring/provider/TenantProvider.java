package org.folio.spring.provider;

public interface TenantProvider {
  void add(String module, String tenant);
  boolean remove(String module, String tenant);
  boolean notExist(String module);
}
