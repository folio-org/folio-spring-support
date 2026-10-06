# HTTP Client Configuration for HTTP Service Clients

HTTP Service Clients (`@HttpExchange` interfaces) created by folio-spring-base share one pooled Apache HttpClient 5.
Without tuning, Apache's default pool (5 connections per route, 25 in total) serializes concurrent calls, because
all calls go to a single sidecar host. This document describes the settings that control the pool.

The pool is active when HTTP Service Clients are enabled (`folio.exchange.enabled=true`).

## Table of Contents
* [Properties](#properties)
* [Example Configuration](#example-configuration)
* [Tuning Guidance](#tuning-guidance)
* [Overriding the Request Factory](#overriding-the-request-factory)

## Properties

All properties use the prefix `folio.exchange.http-client`. "Not set" means the HttpClient default is used.

Each property can also be set with a `FOLIO_EXCHANGE_HTTP_CLIENT_*` environment variable (listed below), using
Spring Boot's standard [relaxed binding](https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables),
so modules don't need `${...}` placeholders. Environment variables override values from the module's
`application.yml`/`application.properties`; command line arguments and JVM system properties still take precedence.

| Property                       | Environment variable                                      | Type             | Default                         | Description                                                               |
|--------------------------------|-----------------------------------------------------------|------------------|---------------------------------|---------------------------------------------------------------------------|
| `max-connections-per-route`    | `FOLIO_EXCHANGE_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE`    | int              | `50`                            | Maximum concurrent connections per route (host)                           |
| `max-connections-total`        | `FOLIO_EXCHANGE_HTTP_CLIENT_MAX_CONNECTIONS_TOTAL`        | int              | `100`                           | Maximum concurrent connections in the pool                                |
| `pool-concurrency-policy`      | `FOLIO_EXCHANGE_HTTP_CLIENT_POOL_CONCURRENCY_POLICY`      | `STRICT` / `LAX` | `STRICT`                        | `STRICT` enforces limits exactly; `LAX` favors concurrency                |
| `pool-reuse-policy`            | `FOLIO_EXCHANGE_HTTP_CLIENT_POOL_REUSE_POLICY`            | `LIFO` / `FIFO`  | `LIFO`                          | Which idle connection is reused first                                     |
| `connect-timeout`              | `FOLIO_EXCHANGE_HTTP_CLIENT_CONNECT_TIMEOUT`              | Duration         | `10s`                           | Timeout for establishing a connection                                     |
| `socket-timeout`               | `FOLIO_EXCHANGE_HTTP_CLIENT_SOCKET_TIMEOUT`               | Duration         | not set (no limit)              | Maximum inactivity between two data packets                               |
| `connection-time-to-live`      | `FOLIO_EXCHANGE_HTTP_CLIENT_CONNECTION_TIME_TO_LIVE`      | Duration         | not set                         | Maximum total lifetime of a connection                                    |
| `validate-after-inactivity`    | `FOLIO_EXCHANGE_HTTP_CLIENT_VALIDATE_AFTER_INACTIVITY`    | Duration         | not set                         | Validate a pooled connection if idle longer than this before reuse        |
| `connection-request-timeout`   | `FOLIO_EXCHANGE_HTTP_CLIENT_CONNECTION_REQUEST_TIMEOUT`   | Duration         | not set (HttpClient default 3m) | Maximum wait to lease a connection from the pool                          |
| `response-timeout`             | `FOLIO_EXCHANGE_HTTP_CLIENT_RESPONSE_TIMEOUT`             | Duration         | not set (no limit)              | Maximum wait for a response after the request is sent                     |
| `evict-idle-connections-after` | `FOLIO_EXCHANGE_HTTP_CLIENT_EVICT_IDLE_CONNECTIONS_AFTER` | Duration         | not set (disabled)              | Close connections idle longer than this using a background evictor thread |
| `automatic-retries-enabled`    | `FOLIO_EXCHANGE_HTTP_CLIENT_AUTOMATIC_RETRIES_ENABLED`    | boolean          | `true`                          | Automatic retry of requests on I/O errors                                 |

Durations use Spring Boot syntax, e.g. `500ms`, `10s`, `5m`. A `0` timeout means no limit.

Values are validated at startup, and an invalid value fails the application with an error naming the property:
* `max-connections-per-route` and `max-connections-total` must be positive;
* `max-connections-total` must be greater than or equal to `max-connections-per-route`;
* durations must not be negative.

Not exposed (configure in code by replacing the request factory): proxy, TLS/SSL, DNS resolver, socket/TCP options,
cookies, redirects, user agent, compression.

## Example Configuration

```yaml
folio:
  exchange:
    http-client:
      max-connections-per-route: 50
      max-connections-total: 100
      connect-timeout: 10s
      connection-request-timeout: 30s
      connection-time-to-live: 10m
      validate-after-inactivity: 5s
      evict-idle-connections-after: 1m
```

The same configuration via environment variables:

```shell
FOLIO_EXCHANGE_HTTP_CLIENT_MAX_CONNECTIONS_PER_ROUTE=50
FOLIO_EXCHANGE_HTTP_CLIENT_MAX_CONNECTIONS_TOTAL=100
FOLIO_EXCHANGE_HTTP_CLIENT_CONNECT_TIMEOUT=10s
FOLIO_EXCHANGE_HTTP_CLIENT_CONNECTION_REQUEST_TIMEOUT=30s
FOLIO_EXCHANGE_HTTP_CLIENT_CONNECTION_TIME_TO_LIVE=10m
FOLIO_EXCHANGE_HTTP_CLIENT_VALIDATE_AFTER_INACTIVITY=5s
FOLIO_EXCHANGE_HTTP_CLIENT_EVICT_IDLE_CONNECTIONS_AFTER=1m
```

## Tuning Guidance

* **Per-route limit is the effective concurrency limit.** All calls go through one sidecar host, so
  `max-connections-per-route` caps parallel outgoing requests. Keep it at or above the size of the thread pool(s)
  that make the calls, and `max-connections-total` at or above it.
* **`connection-request-timeout` vs `response-timeout`.** The former bounds waiting for a free pooled connection
  (pool exhaustion); the latter bounds waiting for the server to answer once the request was sent. Set the former to
  fail fast under saturation.
* **No read/response/socket timeout by default.** Long-running calls (for example reindex operations) must not be
  cut off, so these are unlimited unless you set them.
* **Stale connections.** Behind load balancers or proxies that silently drop idle connections, use
  `connection-time-to-live`, `validate-after-inactivity` and/or `evict-idle-connections-after` to avoid reusing dead
  connections.

## Overriding the Request Factory

The pool is created in the bean `exchangeClientHttpRequestFactory`, guarded by `@ConditionalOnMissingBean`. A module
needing full control (proxy, TLS, etc.) can define its own bean with the same name:

```java
@Bean
public ClientHttpRequestFactory exchangeClientHttpRequestFactory() {
  var httpClient = HttpClients.custom()
    .setConnectionManager(myConnectionManager())
    .build();
  return new HttpComponentsClientHttpRequestFactory(httpClient);
}
```
