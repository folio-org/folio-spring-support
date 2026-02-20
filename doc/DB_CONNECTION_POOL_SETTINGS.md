# Database Connection Pool Settings

## Change Summary

- `DataSourceSchemaAdvisorBeanPostProcessor` now configures HikariCP settings using `DB_*` values.
- `FolioDatabaseEnvs.class` provides defaults and parsing for database-related environment variables.
- Database connections now include the module name via the HikariCP `ApplicationName` property.

## Configuration Precedence

Values are resolved in the following order (highest to lowest priority):

1. Environment variables (recommended for deployments)
2. JVM system properties (for local or process-specific overrides)
3. Default values defined in [FolioDatabaseEnvs.java](../folio-spring-base/src/main/java/org/folio/spring/config/properties/FolioDatabaseEnvs.java)
4. Externalized configuration described here: https://docs.spring.io/spring-boot/reference/features/external-config.html

For numeric settings, non-numeric values are ignored and the default values will apply.

## Override Rules

- If both `DB_MAXPOOLSIZE` and `DB_MAXSHAREDPOOLSIZE` are set, `DB_MAXSHAREDPOOLSIZE` wins.
- `DB_QUERYTIMEOUT` sets the `statement_timeout` for new connections; if unset, no timeout is applied.
- `DB_CHARSET` maps to the HikariCP `characterEncoding` data source property.

## Available Settings

| Env Variable              | Default Value | Description                                                                                         |
|---------------------------|:-------------:|-----------------------------------------------------------------------------------------------------|
| DB_QUERYTIMEOUT           |       -       | Maximum time in milliseconds a query can run before timing out.                                     |
| DB_CHARSET                |     UTF-8     | Character set used for database connections.                                                        |
| DB_MINPOOLSIZE            |       0       | Minimum number of idle connections HikariCP keeps in the pool.                                      |
| DB_MAXPOOLSIZE            |       4       | Maximum number of concurrent connections opened by a module instance.                               |
| DB_MAXSHAREDPOOLSIZE      |       -       | Max concurrent connections with RMB-like shared-pool behavior; overrides `DB_MAXPOOLSIZE` when set. |
| DB_CONNECTIONRELEASEDELAY |  60000 (ms)   | Idle timeout in milliseconds; 0 keeps idle connections open forever.                                |
| DB_MAX_LIFETIME           | 1800000 (ms)  | Maximum connection lifetime in milliseconds; 0 means unlimited.                                     |

## Examples

### Environment Variables (recommended)

```bash
export DB_MAXPOOLSIZE=10
export DB_MINPOOLSIZE=2
export DB_CONNECTIONRELEASEDELAY=45000
java -jar mod-example.jar
```

### JVM System Properties (override)

```bash
java -DDB_MAXSHAREDPOOLSIZE=25 -DDB_CHARSET=UTF-8 -jar mod-example.jar
```
