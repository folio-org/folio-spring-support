package org.folio.spring.repository.impl;

import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.domain.SystemUser;
import org.folio.spring.repository.SystemUserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

@Log4j2
@AllArgsConstructor
public class DbSystemUserRepository implements SystemUserRepository {
  public static final String TABLE_NAME = "system_user_parameters";
  private static final String SYSTEM_USER_CACHE = "systemUserParameters";

  private final JdbcTemplate jdbcTemplate;
  private final FolioModuleMetadata moduleMetadata;

  @Cacheable(cacheNames = SYSTEM_USER_CACHE, unless = "#result==null")
  @Override
  public Optional<SystemUser> getByTenantId(String tenantId) {
    try {
      var resultList = jdbcTemplate.query(getSelectQuery(tenantId),
        new BeanPropertyRowMapper<>(SystemUser.class));

      return resultList.isEmpty()
        ? Optional.empty() : Optional.of(resultList.get(resultList.size() - 1));
    } catch (BadSqlGrammarException ex) {
      // means schema does not exist
      return Optional.empty();
    }
  }

  @CacheEvict(cacheNames = SYSTEM_USER_CACHE, key = "#systemUser.tenantId")
  @Override
  public void save(SystemUser systemUser) {
    if (!userExists(systemUser.getTenantId(), systemUser.getId())) {
      new SimpleJdbcInsert(jdbcTemplate)
        .withSchemaName(moduleMetadata.getDBSchemaName(systemUser.getTenantId()))
        .withTableName(TABLE_NAME)
        .execute(new BeanPropertySqlParameterSource(systemUser));
    } else {
      log.info("User already exists, updating it...");

      jdbcTemplate.update(getUpdateQuery(systemUser.getTenantId()),
        systemUser.getOkapiToken(), systemUser.getOkapiUrl(), systemUser.getId());
    }
  }

  private boolean userExists(String tenant, UUID id) {
    var count = jdbcTemplate.queryForObject(userExistsQuery(tenant), Integer.class, id);
    return count != null && count > 0;
  }

  private String getUpdateQuery(String tenantId) {
    return String.format("UPDATE \"%s\".\"%s\" " +
        "SET okapi_token = ?, okapi_url = ? " +
        "WHERE id = ?", moduleMetadata.getDBSchemaName(tenantId), TABLE_NAME);
  }

  private String getSelectQuery(String tenantId) {
    return String.format("SELECT * FROM \"%s\".\"%s\"", moduleMetadata.getDBSchemaName(tenantId),
      TABLE_NAME);
  }

  private String userExistsQuery(String tenantId) {
    return String.format("SELECT COUNT(*) FROM \"%s\".\"%s\" where id = ?",
      moduleMetadata.getDBSchemaName(tenantId), TABLE_NAME);
  }
}
