package org.folio.spring.liquibase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.jdbc.core.JdbcTemplate;

@UnitTest
class FolioLiquibaseConfigurationTest {

  @Test
  void liquibaseMigrationLockService_canBeCreatedFromConfiguration() {
    var properties = new LiquibaseProperties();
    properties.setLiquibaseSchema("liquibase_schema");
    properties.setDatabaseChangeLogLockTable("databasechangeloglock");

    var configuration = new FolioLiquibaseConfiguration(properties);
    var jdbcTemplate = mock(JdbcTemplate.class);
    var service = configuration.liquibaseMigrationLockService(jdbcTemplate);

    assertNotNull(service);
  }

  @Test
  void liquibaseMigrationLockService_canBeCreatedWithoutLiquibaseSpecificProperties() {
    var configuration = new FolioLiquibaseConfiguration(new LiquibaseProperties());
    var jdbcTemplate = mock(JdbcTemplate.class);
    var service = configuration.liquibaseMigrationLockService(jdbcTemplate);

    assertNotNull(service);
  }
}
