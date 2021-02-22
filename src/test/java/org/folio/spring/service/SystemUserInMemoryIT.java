package org.folio.spring.service;

import static org.folio.spring.support.TestBase.TENANT_NAME;
import static org.folio.spring.support.TestBase.getOkapiUrl;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.UUID;
import org.folio.spring.domain.SystemUser;
import org.folio.spring.repository.SystemUserRepository;
import org.folio.spring.repository.impl.InMemorySystemUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(value = {
  "classpath:enable-sys-user.properties",
  "classpath:application.properties"},
  properties = "spring.liquibase.enabled=false")
class SystemUserInMemoryIT {
  @Autowired
  private SystemUserRepository repository;

  @Configuration
  @EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class})
  static class NoDbConfiguration {}

  @Test
  void shouldCreateUser() {
    assertThat(repository, instanceOf(InMemorySystemUserRepository.class));

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
