package org.folio.tenant.settings.repository;

import org.folio.tenant.settings.entity.SettingGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for accessing setting group entities.
 */
@Repository
public interface SettingGroupRepository extends JpaRepository<SettingGroupEntity, String> {
}
