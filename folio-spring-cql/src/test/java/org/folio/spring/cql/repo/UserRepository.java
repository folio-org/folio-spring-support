package org.folio.spring.cql.repo;

import java.util.UUID;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.cql.domain.User;

public interface UserRepository extends JpaCqlRepository<User, UUID> {
}
