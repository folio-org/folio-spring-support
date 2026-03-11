package org.folio.spring.liquibase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import javax.sql.DataSource;
import org.folio.spring.exception.LiquibaseMigrationException;
import org.folio.spring.testing.type.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;

@UnitTest
class FolioLiquibaseConfigurationTest {

  @Test
  void liquibaseMigrationLockService_canBeCreatedFromConfiguration() {
    var properties = new LiquibaseProperties();
    properties.setLiquibaseSchema("liquibase_schema");
    properties.setDatabaseChangeLogLockTable("databasechangeloglock");

    var configuration = new FolioLiquibaseConfiguration(properties);
    var dataSource = mock(DataSource.class);
    var service = configuration.liquibaseMigrationLockService(dataSource, null);

    assertNotNull(service);
  }

  @Test
  void liquibaseMigrationLockService_canBeCreatedWithoutLiquibaseSpecificProperties() {
    var configuration = new FolioLiquibaseConfiguration(new LiquibaseProperties());
    var dataSource = mock(DataSource.class);
    var service = configuration.liquibaseMigrationLockService(dataSource, null);

    assertNotNull(service);
  }

  @Test
  void liquibaseMigrationException_canBeCreatedWithMessageOnly() {
    var exception = new LiquibaseMigrationException("message");

    assertNotNull(exception);
  }
}
