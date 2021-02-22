package org.folio.spring.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;
import org.folio.spring.domain.SystemUser;
import org.folio.spring.repository.SystemUserRepository;
import org.folio.spring.support.TestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource({"classpath:enable-sys-user.properties", "classpath:application.properties"})
@EnableCaching
class SystemUserRepositoryCacheIT extends TestBase {
  private static final String CACHE_NAME = "systemUserParameters";

  @Autowired
  private SystemUserRepository repository;
  @Autowired
  private CacheManager cacheManager;

  @AfterEach
  void removeSystemUsers(@Autowired JdbcTemplate template) {
    template.execute("DELETE FROM test_tenant_folio_spring_base.system_user_parameters");
  }

  @Test
  void shouldPutIntoCacheWhenResultExists() {
    var systemUser = SystemUser.builder()
      .id(UUID.randomUUID())
      .tenantId(TENANT_NAME)
      .okapiToken("aa.bb.cc")
      .username("username")
      .password("password")
      .okapiUrl(getOkapiUrl())
      .build();

    repository.save(systemUser);
    var systemUserOptional = repository.getByTenantId(TENANT_NAME);

    assertThat(systemUserOptional.isPresent(), is(true));
    assertThat(cacheManager.getCache(CACHE_NAME), notNullValue());
    assertThat(cacheManager.getCache(CACHE_NAME).get(TENANT_NAME, SystemUser.class),
      is(systemUser));
  }

  @Test
  void shouldNotPutIntoCacheWhenResultIsNull() {
    var systemUserOptional = repository.getByTenantId("undefined_tenant");

    assertThat(systemUserOptional.isPresent(), is(false));
    assertThat(cacheManager.getCache(CACHE_NAME), notNullValue());
    assertThat(cacheManager.getCache(CACHE_NAME).get(TENANT_NAME), is(nullValue()));
  }

  @Test
  void shouldUpdateUserIfExists() {
    var systemUser = SystemUser.builder()
      .id(UUID.randomUUID())
      .tenantId(TENANT_NAME)
      .username("folio-spring-base")
      .password("password")
      .okapiToken("aa.bb.cc")
      .okapiUrl(getOkapiUrl());

    var originSystemUser = systemUser.build();
    repository.save(originSystemUser);

    // Should update the user
    repository.save(systemUser.okapiToken("new_token").okapiUrl("new_url").build());

    var updatedSystemUser = repository.getByTenantId(TENANT_NAME).get();

    assertThat(updatedSystemUser.getId(), is(originSystemUser.getId()));
    assertThat(updatedSystemUser.getUsername(), is(originSystemUser.getUsername()));
    assertThat(updatedSystemUser.getPassword(), is(originSystemUser.getPassword()));
    assertThat(updatedSystemUser.getOkapiToken(), is("new_token"));
    assertThat(updatedSystemUser.getOkapiUrl(), is("new_url"));
    assertThat(updatedSystemUser.getTenantId(), is(TENANT_NAME));
  }

  @Test
  void shouldCreateUser() {
    var systemUser = SystemUser.builder()
      .id(UUID.randomUUID())
      .tenantId(TENANT_NAME)
      .okapiToken("new_token")
      .username("new_user")
      .password("password")
      .okapiUrl(getOkapiUrl())
      .build();

    repository.save(systemUser);

    var finalSystemUser = repository.getByTenantId(TENANT_NAME).get();

    assertThat(finalSystemUser.getId(), is(systemUser.getId()));
    assertThat(finalSystemUser.getUsername(), is("new_user"));
    assertThat(finalSystemUser.getPassword(), is("password"));
    assertThat(finalSystemUser.getOkapiToken(), is("new_token"));
    assertThat(finalSystemUser.getOkapiUrl(), is(getOkapiUrl()));
    assertThat(finalSystemUser.getTenantId(), is(TENANT_NAME));
  }
}
