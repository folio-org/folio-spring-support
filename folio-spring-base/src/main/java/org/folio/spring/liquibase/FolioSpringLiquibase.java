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

  private static final Pattern NON_WORD_CHARACTERS = Pattern.compile("\\W");

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
          statement.execute("create schema if not exists " + defaultSchema);
          // use advisory lock for concurrent installs of multiple modules
          // https://folio-org.atlassian.net/browse/RMB-957
          // https://github.com/folio-org/raml-module-builder/blob/v35.3.0/domain-models-runtime/src/main/resources/templates/db_scripts/extensions.ftl
          statement.execute("DO $$ BEGIN PERFORM pg_advisory_xact_lock(20201101, 1234567890); "
              + "CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public; "
              + "END $$");
          // https://github.com/folio-org/raml-module-builder/blob/v35.3.0/domain-models-runtime/src/main/resources/templates/db_scripts/general_functions.ftl#L89-L94
          statement.execute("CREATE OR REPLACE FUNCTION " + defaultSchema + ".f_unaccent(text) "
              + "RETURNS text AS $$ SELECT public.unaccent('public.unaccent', $1) "
              + "$$ LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT");
        }

      } catch (SQLException e) {
        log.error("Default schema " + defaultSchema + " has not been created.", e);
      }
    }

    super.afterPropertiesSet();
  }

}
