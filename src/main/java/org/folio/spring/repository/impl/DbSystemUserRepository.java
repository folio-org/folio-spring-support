package org.folio.spring.repository.impl;

import java.util.Optional;
import lombok.AllArgsConstructor;
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
      var resultList = jdbcTemplate.query("SELECT * FROM " + getFullTableName(tenantId),
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
    new SimpleJdbcInsert(jdbcTemplate)
      .withSchemaName(moduleMetadata.getDBSchemaName(systemUser.getTenantId()))
      .withTableName(TABLE_NAME)
      .execute(new BeanPropertySqlParameterSource(systemUser));
  }

  private String getFullTableName(String tenantId) {
    return moduleMetadata.getDBSchemaName(tenantId) + "." + TABLE_NAME;
  }
}
