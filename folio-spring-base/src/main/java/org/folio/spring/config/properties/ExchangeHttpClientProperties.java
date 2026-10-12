package org.folio.spring.config.properties;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the pooled Apache HttpClient used by HTTP Service Clients ({@code @HttpExchange}).
 * A {@code null} value means "not set": the HttpClient default (usually no limit) is used.
 * A zero timeout also means no limit. Invalid values fail the application startup.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "folio.exchange.http-client")
public class ExchangeHttpClientProperties {

  /**
   * Maximum number of concurrent connections per route. All calls normally go through a single Okapi/sidecar host,
   * so this is the effective concurrency limit.
   */
  @Positive
  private int maxConnectionsPerRoute = 50;

  /**
   * Maximum number of concurrent connections in the whole pool.
   */
  @Positive
  private int maxConnectionsTotal = 100;

  /**
   * Pool concurrency policy: STRICT enforces the limits exactly, LAX allows better concurrency at the cost of
   * possibly exceeding the limits.
   */
  @NotNull
  private PoolConcurrencyPolicy poolConcurrencyPolicy = PoolConcurrencyPolicy.STRICT;

  /**
   * Pool connection reuse policy: LIFO reuses the most recently released connection, FIFO the oldest one.
   */
  @NotNull
  private PoolReusePolicy poolReusePolicy = PoolReusePolicy.LIFO;

  /**
   * Timeout for establishing a connection.
   */
  @NotNull
  @DurationMin(nanos = 0)
  private Duration connectTimeout = Duration.ofSeconds(10);

  /**
   * Socket timeout (maximum inactivity between two data packets). Not set means no limit.
   */
  @DurationMin(nanos = 0)
  private Duration socketTimeout;

  /**
   * Maximum total lifetime of a connection. Not set means connections live as long as they are usable.
   */
  @DurationMin(nanos = 0)
  private Duration connectionTimeToLive;

  /**
   * Period of inactivity after which a pooled connection is validated before being reused.
   */
  @DurationMin(nanos = 0)
  private Duration validateAfterInactivity;

  /**
   * Maximum time to wait for a connection to be leased from the pool. Not set means the HttpClient default (3 minutes).
   */
  @DurationMin(nanos = 0)
  private Duration connectionRequestTimeout;

  /**
   * Maximum time to wait for a response after the request was sent. Not set means no limit.
   */
  @DurationMin(nanos = 0)
  private Duration responseTimeout;

  /**
   * Close connections idle for longer than this period using a background evictor thread. Not set disables
   * idle eviction.
   */
  @DurationMin(nanos = 0)
  private Duration evictIdleConnectionsAfter;

  /**
   * Whether the HttpClient automatically retries requests on I/O errors.
   */
  private boolean automaticRetriesEnabled = true;

  /**
   * The pool can't serve more connections per route than in total, so a total limit below the per-route limit
   * would silently lower the effective per-route limit.
   */
  @AssertTrue(message = "max-connections-total must be greater than or equal to max-connections-per-route")
  public boolean isMaxConnectionsTotalValid() {
    return maxConnectionsTotal >= maxConnectionsPerRoute;
  }
}
