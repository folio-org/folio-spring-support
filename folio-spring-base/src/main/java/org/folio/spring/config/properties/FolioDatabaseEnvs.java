package org.folio.spring.config.properties;

import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Getter
@RequiredArgsConstructor
public enum FolioDatabaseEnvs {

  /**
   * The maximum time in milliseconds that a client will wait for a database query to complete before timing out.
   */
  DB_QUERYTIMEOUT(null),

  /**
   * The character set used for database connections. Defaults to UTF-8.
   */
  DB_CHARSET("UTF-8"),

  /**
   * Defines the minimum number of idle connections that HikariCP tries to maintain in the pool. It defaults to 0.
   */
  DB_MINSHAREDPOOLSIZE("0"),

  /**
   * Defines the maximum number of concurrent connections that one module instance opens. They are only opened if
   * needed. If all connections are in use further requests will wait until one connection becomes free. This setting
   * is added to provide a similar behavior as `raml-module-builder`.
   */
  DB_MAXSHAREDPOOLSIZE(null),

  /**
   * Sets the delay in milliseconds after which an idle connection is closed. A connection becomes idle if the query
   * ends, it is not idle if it is waiting for a response. Use 0 to keep idle connections open forever. Default is
   * one minute (60000 ms).
   */
  DB_CONNECTIONRELEASEDELAY("60000"),

  /**
   * Limits the lifetime (non-idle time plus idle time) of a database connection in milliseconds. If exceeded the
   * connection is closed if it is or becomes idle. 0 means unlimited lifetime. It defaults to 1800000 (30 minutes).
   */
  DB_MAX_LIFETIME("1800000");

  private final String defaultValue;

  /**
   * Finds the environment variable value as a trimmed string or returns the default value if not set.
   *
   * @return An {@link Optional} containing the environment variable value or the default value if not set.
   */
  public Optional<String> findString() {
    return Optional.ofNullable(System.getenv(this.name()))
      .or(() -> Optional.ofNullable(System.getProperty(this.name()))
      .map(StringUtils::trimToNull)
      .or(() -> Optional.ofNullable(defaultValue)));
  }

  /**
   * Finds the environment variable value as a long or returns empty if not set or not numeric.
   *
   * @return An {@link Optional} containing the long value of the env variable or empty if not set or not numeric.
   */
  public Optional<Long> findLong() {
    return findString()
      .filter(StringUtils::isNumeric)
      .flatMap(FolioDatabaseEnvs::parseLongSafe)
      .or(() -> parseLongSafe(defaultValue));
  }

  private static Optional<Long> parseLongSafe(String value) {
    try {
      return Optional.of(Long.parseLong(value));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
