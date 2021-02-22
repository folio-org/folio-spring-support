package org.folio.spring.repository.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.domain.SystemUser;
import org.folio.spring.repository.SystemUserRepository;

@Log4j2
@AllArgsConstructor
public class InMemorySystemUserRepository implements SystemUserRepository {
  private static final Map<String, SystemUser> systemUsers =
    new ConcurrentHashMap<>();

  @Override
  public Optional<SystemUser> getByTenantId(String tenantId) {
    return Optional.ofNullable(systemUsers.get(tenantId));
  }

  @Override
  public void save(SystemUser systemUser) {
    systemUsers.put(systemUser.getTenantId(), systemUser);
  }
}
