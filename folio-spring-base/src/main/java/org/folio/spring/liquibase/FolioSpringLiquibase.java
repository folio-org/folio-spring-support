package org.folio.spring.liquibase;

import java.sql.SQLException;
import java.util.regex.Pattern;
import liquibase.database.DatabaseFactory;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

@Log4j2
public class FolioSpringLiquibase extends SpringLiquibase {

  private static final Pattern NON_WORD_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_]");

  @Override
  public void afterPropertiesSet() {
    //just suppress liquibase auto-execution
  }

  // suppress "Make sure using a dynamically formatted SQL query is safe here."
  // because the character check prevents SQL injection
  @SuppressWarnings("java:S2077")
  public void performLiquibaseUpdate() throws LiquibaseException {
    DatabaseFactory.getInstance().register(new FolioPostgresDatabase());
    var defaultSchema = getDefaultSchema();
    if (StringUtils.isNotBlank(defaultSchema)) {
      //DB schema name check to prevent SQL injection.
      if (NON_WORD_CHARACTERS.matcher(defaultSchema).find()) {
        throw new IllegalArgumentException("Invalid schema name: " + defaultSchema);
      }
      try (var connection = getDataSource().getConnection()) {
        try (var statement = connection.createStatement()) {
          log.debug("creating [{}] schema", defaultSchema);
          statement.execute("create schema if not exists " + defaultSchema + ";");
        }
      } catch (SQLException e) {
        log.error("Default schema " + defaultSchema + " has not been created.", e);
      }
    }

    super.afterPropertiesSet();
  }

}
