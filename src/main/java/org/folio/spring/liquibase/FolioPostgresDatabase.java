package org.folio.spring.liquibase;

import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;

/**
 * Override default behaviour that override search_path that sets in {@link org.folio.spring.config.DataSourceFolioWrapper}
 */
public class FolioPostgresDatabase extends PostgresDatabase {

  @Override
  public int getPriority() {
    return super.getPriority() + 1;
  }

  @Override
  public void rollback() throws DatabaseException {
    try {
      getConnection().rollback();
    } catch (DatabaseException e) {
      throw new DatabaseException(e);
    }
  }
}
