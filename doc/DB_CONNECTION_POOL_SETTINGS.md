# Database Connection Pool Settings

## Change Summary

- `DataSourceSchemaAdvisorBeanPostProcessor` now configures HikariCP settings using `DB_*` values.
- `FolioDatabaseEnvs.class` provides defaults and parsing for database-related environment variables.
- Database connections now include the module name via the HikariCP `ApplicationName` property.

## Configuration Precedence

Values are resolved in the following order (highest to lowest priority):

1. Environment variables (recommended for deployments; for example, `export DB_MAXSHAREDPOOLSIZE=10`)
2. JVM system properties (for local or process-specific overrides; for example, `-DDB_MAXSHAREDPOOLSIZE=10`)
3. Default values defined in [FolioDatabaseEnvs.java](../folio-spring-base/src/main/java/org/folio/spring/config/properties/FolioDatabaseEnvs.java)
4. Externalized configuration described here: https://docs.spring.io/spring-boot/reference/features/external-config.html

For numeric settings, non-numeric values are ignored and the default values will apply.

## Available Settings

| Env Variable              | Default Value | Description                                                                                                                                                                                                                                                                                    |
|---------------------------|:-------------:|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| DB_QUERYTIMEOUT           |       -       | The maximum time in milliseconds that a client will wait for a database query to complete before timing out. It sets the `statement_timeout={{value}}` for new connections; if unset, no timeout is applied.                                                                                   |
| DB_CHARSET                |     UTF-8     | The character set used for database connections. It maps to the HikariCP `characterEncoding` data source property.                                                                                                                                                                             |
| DB_MINSHAREDPOOLSIZE      |       0       | Minimum number of idle connections HikariCP keeps in the shared pool.                                                                                                                                                                                                                          |
| DB_MAXSHAREDPOOLSIZE      |       -       | Defines the maximum number of concurrent connections that one module instance opens. They are only opened if needed. If all connections are in use further requests will wait until one connection becomes free. This setting is added to provide a similar behavior as `raml-module-builder`. |
| DB_CONNECTIONRELEASEDELAY |  60000 (ms)   | Sets the delay in milliseconds after which an idle connection is closed. A connection becomes idle if the query ends, it is not idle if it is waiting for a response. Use 0 to keep idle connections open forever. The default is 1 minute.                                                    |
| DB_MAX_LIFETIME           | 1800000 (ms)  | Limits the lifetime (non-idle time plus idle time) of a database connection in milliseconds. If exceeded the connection is closed if it is or becomes idle. 0 means unlimited lifetime. The default is 30 minutes.                                                                             |

## Examples

### Environment Variables (recommended)

```bash
export DB_MINSHAREDPOOLSIZE=2
export DB_MAXSHAREDPOOLSIZE=10
export DB_CONNECTIONRELEASEDELAY=45000
java -jar mod-example.jar
```

### JVM System Properties (override)

```bash
java -DDB_MAXSHAREDPOOLSIZE=25 -DDB_CHARSET=UTF-8 -jar mod-example.jar
```
