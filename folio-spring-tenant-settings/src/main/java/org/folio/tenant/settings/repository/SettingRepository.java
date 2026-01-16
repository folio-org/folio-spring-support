package org.folio.tenant.settings.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.tenant.settings.entity.SettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for accessing setting entities.
 */
@Repository
public interface SettingRepository extends JpaRepository<SettingEntity, UUID> {

  /**
   * Finds all settings belonging to a specific group.
   *
   * @param groupId the group identifier
   * @return list of settings in the group
   */
  List<SettingEntity> findByGroupId(String groupId);

  /**
   * Finds a setting by group ID and key.
   *
   * @param groupId the group identifier
   * @param key the setting key
   * @return optional containing the setting if found
   */
  Optional<SettingEntity> findByGroupIdAndKey(String groupId, String key);
}
