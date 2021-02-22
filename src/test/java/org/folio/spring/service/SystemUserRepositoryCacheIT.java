package org.folio.spring.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.UUID;
import org.folio.spring.domain.SystemUser;
import org.folio.spring.repository.SystemUserRepository;
import org.folio.spring.support.TestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource({"classpath:enable-sys-user.properties", "classpath:application.properties"})
@EnableCaching
class SystemUserRepositoryCacheIT extends TestBase {
  private static final String CACHE_NAME = "systemUserParameters";

  @Autowired
  private SystemUserRepository repository;
  @Autowired
  private CacheManager cacheManager;

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
}
