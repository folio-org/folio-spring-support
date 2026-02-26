package org.folio.spring.cql.repo;

import java.util.UUID;
import org.folio.spring.cql.JpaCqlRepository;
import org.folio.spring.cql.domain.CapabilitySet;

public interface CapabilitySetRepository extends JpaCqlRepository<CapabilitySet, UUID> {
}
