# HTTP Request/Response Logging Configuration

This document describes two types of logging available in FOLIO Spring Support:
1. **Request Logging** - logs incoming HTTP requests to your application
2. **Exchange Logging** - logs outgoing HTTP client requests from your application

## Table of Contents
* [Request Logging (Incoming Requests)](#request-logging-incoming-requests)
* [Exchange Logging (Outgoing HTTP Client Requests)](#exchange-logging-outgoing-http-client-requests)
* [Logging Levels](#logging-levels)
* [Framework Logging Configuration](#framework-logging-configuration)
* [Example Output](#example-output)
* [What Gets Logged](#what-gets-logged)
* [Performance Considerations](#performance-considerations)
* [Security Considerations](#security-considerations)
* [Conditional Logging](#conditional-logging)

## Request Logging (Incoming Requests)

Logs incoming HTTP requests to your application endpoints.

### Enable/Disable Request Logging

To enable logging of incoming HTTP requests, add the following property to your `application.yml` or `application.properties`:

#### application.yml
```yaml
folio:
  logging:
    request:
      enabled: true
      level: BASIC  # Optional: NONE, BASIC, HEADERS, FULL (default: BASIC)
```

#### application.properties
```properties
folio.logging.request.enabled=true
folio.logging.request.level=BASIC
```

## Exchange Logging (Outgoing HTTP Client Requests)

Logs outgoing HTTP requests made by your application to external services.

### Enable/Disable Exchange Logging

To enable logging of outgoing HTTP client requests, add the following property to your `application.yml` or `application.properties`:

#### application.yml
```yaml
folio:
  logging:
    exchange:
      enabled: true
      level: BASIC  # Optional: NONE, BASIC, HEADERS, FULL (default: BASIC)
```

#### application.properties
```properties
folio.logging.exchange.enabled=true
folio.logging.exchange.level=BASIC
```

## Logging Levels

Both request and exchange logging support the following levels:

| Level       | Description                                                                                               |
|-------------|-----------------------------------------------------------------------------------------------------------|
| **NONE**    | No logging (only applicable if filter/interceptor is enabled but you want to disable logging temporarily) |
| **BASIC**   | Logs URI, HTTP method, status code, and duration                                                          |
| **HEADERS** | Logs everything in BASIC plus request and response headers                                                |
| **FULL**    | Logs everything in HEADERS plus request and response bodies                                               |

**Default Level**: `BASIC` if not specified

### Setting the Logging Level

Configure the level for each type of logging separately:

```yaml
folio:
  logging:
    request:
      enabled: true
      level: HEADERS  # For incoming requests
    exchange:
      enabled: true
      level: FULL     # For outgoing HTTP client requests
```

## Framework Logging Configuration

The logging is done at **DEBUG** level, so you also need to configure your logging framework to enable DEBUG logging for the appropriate classes:

### For Log4j2 (log4j2.xml)
```xml
<Configuration>
  <Loggers>
    <!-- Enable DEBUG logging for incoming HTTP requests -->
    <Logger name="org.folio.spring.filter.IncomingRequestLoggingFilter" level="DEBUG" additivity="false">
      <AppenderRef ref="Console"/>
    </Logger>
    
    <!-- Enable DEBUG logging for outgoing HTTP client requests -->
    <Logger name="org.folio.spring.client.ExchangeLoggingInterceptor" level="DEBUG" additivity="false">
      <AppenderRef ref="Console"/>
    </Logger>
    
    <Root level="INFO">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

### For application.yml (Spring Boot logging)
```yaml
logging:
  level:
    org.folio.spring.filter.IncomingRequestLoggingFilter: DEBUG        # For incoming requests
    org.folio.spring.client.ExchangeLoggingInterceptor: DEBUG  # For outgoing requests
```

## Example Output

### Request Logging (Incoming Requests)

#### BASIC Level
```
---> GET /users/123 includeDeleted=false
<--- 200 in 45ms
```

#### HEADERS Level
```
---> GET /users/123 includeDeleted=false
x-okapi-tenant: testlib
x-okapi-token: eyJhbGc...
Accept: application/json
---> END HTTP
<--- 200 in 45ms
```

#### FULL Level
```
---> GET /users/123 includeDeleted=false
x-okapi-tenant: testlib
x-okapi-token: eyJhbGc...
Accept: application/json
Request body: {"userId":"123","action":"update"}
---> END HTTP
<--- 200 in 45ms
Response body: {"id":"123","username":"john_doe","active":true}
<--- END HTTP
```

### Exchange Logging (Outgoing HTTP Client Requests)

#### BASIC Level
```
===========================Request Begin===========================
URI         : http://okapi/perms/users/123/permissions?indexField=userId
Method      : GET
===========================Request End=============================

===========================Response Begin==========================
URI          : http://okapi/perms/users/123/permissions?indexField=userId
Status code  : 200 (OK)
Duration     : 42 ms
===========================Response End============================
```

#### HEADERS Level
```
===========================Request Begin===========================
URI         : http://okapi/perms/users/123/permissions?indexField=userId
Method      : GET
Headers     : [x-okapi-tenant:"testlib", x-okapi-token:"eyJhbGc...", Accept:"application/json"]
===========================Request End=============================

===========================Response Begin==========================
URI          : http://okapi/perms/users/123/permissions?indexField=userId
Status code  : 200 (OK)
Headers      : [Content-Type:"application/json;charset=UTF-8", Content-Length:"157"]
Duration     : 42 ms
===========================Response End============================
```

#### FULL Level
```
===========================Request Begin===========================
URI         : http://okapi/perms/users/123/permissions?indexField=userId
Method      : POST
Headers     : [x-okapi-tenant:"testlib", x-okapi-token:"eyJhbGc...", Accept:"application/json"]
Request body: {"userId":"123","permissions":["users.item.post"]}
===========================Request End=============================

===========================Response Begin==========================
URI          : http://okapi/perms/users/123/permissions?indexField=userId
Status code  : 200 (OK)
Headers      : [Content-Type:"application/json;charset=UTF-8", Content-Length:"157"]
Duration     : 42 ms
Response body: {"permissions":["users.item.get","users.collection.get","users.item.post"],"totalRecords":3}
===========================Response End============================
```

## What Gets Logged

### Request Logging (Incoming Requests)

| Level   | URI & Method | Status & Duration | Headers | Request Body | Response Body |
|---------|--------------|-------------------|---------|--------------|---------------|
| NONE    | ❌            | ❌                 | ❌       | ❌            | ❌             |
| BASIC   | ✅            | ✅                 | ❌       | ❌            | ❌             |
| HEADERS | ✅            | ✅                 | ✅       | ❌            | ❌             |
| FULL    | ✅            | ✅                 | ✅       | ✅            | ✅             |

### Exchange Logging (Outgoing HTTP Client Requests)

| Level   | URI & Method | Status & Duration | Headers | Request Body | Response Body |
|---------|--------------|-------------------|---------|--------------|---------------|
| NONE    | ❌            | ❌                 | ❌       | ❌            | ❌             |
| BASIC   | ✅            | ✅                 | ❌       | ❌            | ❌             |
| HEADERS | ✅            | ✅                 | ✅       | ❌            | ❌             |
| FULL    | ✅            | ✅                 | ✅       | ✅            | ✅             |

## Performance Considerations

⚠️ **Important Notes:**

1. **Debug Level Only**: Logging only occurs when DEBUG level is enabled for the respective logger class
   - `LoggingRequestFilter` for incoming requests
   - `ExchangeLoggingInterceptor` for outgoing requests
2. **Production**: Disable in production or use BASIC/HEADERS level only. FULL level logs complete request/response bodies
3. **Sensitive Data**: Be cautious about logging sensitive data (passwords, tokens are logged with HEADERS and FULL levels)
4. **Performance Impact**: 
   - BASIC level: minimal overhead (~1-2ms per request)
   - HEADERS level: low overhead (~2-3ms per request)
   - FULL level: moderate overhead (~3-5ms per request, more for large payloads)
5. **Memory**: FULL level buffers entire request/response bodies in memory

## Security Considerations

The logging can expose sensitive information depending on the level configured:

| Level   | Security Risk                                            | Recommended For                         |
|---------|----------------------------------------------------------|-----------------------------------------|
| NONE    | No risk                                                  | Production (when logging is not needed) |
| BASIC   | Low - only URIs and methods exposed                      | Production, Staging                     |
| HEADERS | Medium - authentication tokens and headers exposed       | Development, Staging (with caution)     |
| FULL    | High - complete request/response bodies with credentials | Development only                        |

**For production environments, consider:**
- Using BASIC level for general monitoring
- Using HEADERS level only for specific troubleshooting with restricted log access
- Never using FULL level in production
- Implementing separate log files with restricted access for any level above BASIC
- Implementing log rotation and retention policies
- Ensuring logs are encrypted at rest if they contain sensitive data

## Conditional Logging

You can enable logging only in specific environments:

### Per Environment Configuration
```yaml
spring:
  profiles: dev
  
folio:
  logging:
    request:
      enabled: true
      level: FULL
    exchange:
      enabled: true
      level: FULL

---

spring:
  profiles: staging
  
folio:
  logging:
    request:
      enabled: true
      level: HEADERS
    exchange:
      enabled: true
      level: HEADERS

---

spring:
  profiles: prod
  
folio:
  logging:
    request:
      enabled: false
    exchange:
      enabled: false
```