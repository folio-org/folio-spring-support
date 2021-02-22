package org.folio.spring.repository;

import java.util.Optional;
import org.folio.spring.domain.SystemUser;

public interface SystemUserRepository {
  Optional<SystemUser> getByTenantId(String tenantId);

  void save(SystemUser systemUser);
}
