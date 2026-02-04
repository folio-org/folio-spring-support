# Migration Guide: Spring Boot 4.0 and folio-spring-support 10.0

This guide provides a complete step-by-step migration path for FOLIO modules upgrading to Spring Boot 4.0.0 and folio-spring-support 10.0.0, which includes migration from OpenFeign to Spring HTTP Service Clients.

## Table of Contents
1. [Overview](#overview)
2. [Migration Steps](#migration-steps)
   - [Step 1: Update Dependencies](#step-1-update-dependencies)
   - [Step 2: Update Configuration](#step-2-update-configuration)
   - [Step 3: Update Client Interfaces](#step-3-update-client-interfaces)
   - [Step 4: Update Main Application Class](#step-4-update-main-application-class)
   - [Step 5: Configure HTTP Service Clients](#step-5-configure-http-service-clients)
   - [Step 6: Update Exception Handling](#step-6-update-exception-handling)
   - [Step 7: Update Jackson Imports](#step-7-update-jackson-imports)
   - [Step 8: Update Test Configuration](#step-8-update-test-configuration)
   - [Step 9: Update Integration Tests](#step-9-update-integration-tests)
3. [Complete Migration Checklist](#complete-migration-checklist)
4. [Troubleshooting](#troubleshooting)
5. [Additional Resources](#additional-resources)

---

## Overview

This migration involves three major changes:

1. **Spring Boot 3.x → 4.0.2**: Requires Java 21+, includes Jakarta EE 10+
2. **Jackson 2.x → 3.x**: Package changes from `com.fasterxml.jackson` to `tools.jackson`
3. **OpenFeign → Spring HTTP Service Clients**: Native Spring HTTP clients instead of Feign

### Why Migrate from OpenFeign?

**Key reasons for migration:**
- ✅ **Official Spring Recommendation**: Spring Cloud team recommends using Spring HTTP Service Clients
- ✅ **Feature-Complete**: OpenFeign is in maintenance mode - no new features will be added
- ✅ **Native Spring Integration**: HTTP Service Clients are part of Spring Framework core
- ✅ **Better Performance**: Direct RestClient usage without additional abstraction layers
- ✅ **Future-Proof**: Aligned with Spring's long-term direction
- ✅ **Simplified Dependencies**: No need for Spring Cloud dependencies

**Source**: [Spring Cloud OpenFeign GitHub](https://github.com/spring-cloud/spring-cloud-openfeign)

**For additional Spring Boot 4.0 migration topics not covered in this guide, refer to the official [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).**

**For detailed HTTP Service Client documentation and advanced usage, see the [Spring HTTP Interface documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-service-client).**

### What's Changing

| Component            | Before                        | After                 |
|----------------------|-------------------------------|-----------------------|
| Spring Boot          | 3.5.x                         | 4.0.0+                |
| folio-spring-support | 9.x                           | 10.0.0+               |
| HTTP Clients         | OpenFeign                     | Spring HTTP Service   |
| Jackson              | 2.x (`com.fasterxml.jackson`) | 3.x (`tools.jackson`) |
| Servlet API          | javax.servlet                 | jakarta.servlet       |

---

## Migration Steps

## Step 1: Update Dependencies

### 1.1 Update Spring Boot Parent Version

**pom.xml - Before:**
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.5.7</version>
  <relativePath />
</parent>

<properties>
  <java.version>17</java.version>
  <folio-spring-support.version>9.x.x</folio-spring-support.version>
</properties>
```

**pom.xml - After:**
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.0.2</version>
  <relativePath />
</parent>

<properties>
  <java.version>21</java.version>
  <folio-spring-support.version>10.0.0-RC1</folio-spring-support.version>
</properties>
```

### 1.2 Replace Individual Dependencies with Spring Boot Starters

**Before:**
```xml
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>

<dependency>
  <groupId>com.fasterxml.jackson.module</groupId>
  <artifactId>jackson-module-jaxb-annotations</artifactId>
</dependency>

<dependency>
  <groupId>org.hibernate.validator</groupId>
  <artifactId>hibernate-validator</artifactId>
</dependency>
```

**After:**
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-kafka</artifactId>
</dependency>

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 1.3 Add Test Dependencies

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-webmvc-test</artifactId>
  <scope>test</scope>
</dependency>
```

### 1.4 Remove OpenFeign Dependencies

These are automatically removed when upgrading to folio-spring-support 10.0.0:
```xml
<!-- No longer needed - automatically removed -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**Notes:**
- Spring Boot 4.0.0 requires Java 21+
- Use Spring Boot starters instead of individual artifact dependencies
- Jackson 3.x is included automatically (package `tools.jackson`)

---

## Step 2: Update Configuration

### 2.1 Update application.yml

**Before:**
```yaml
spring:
  cloud:
    openfeign:
      okhttp:
        enabled: true
```

**After:**
```yaml
folio:
  exchange:
    enabled: true  # REQUIRED for HTTP service clients
  logging:
    request:
      enabled: false       # Optional: logs incoming HTTP requests
    exchange:
      enabled: true        # Optional: logs outgoing HTTP client requests
      level: BASIC         # Optional: NONE, BASIC, HEADERS, FULL (default: BASIC)
```

### 2.2 Logging Levels

Configure logging levels for request/response debugging:

| Level       | Logs                                    |
|-------------|-----------------------------------------|
| **NONE**    | No logging                              |
| **BASIC**   | URI, HTTP method, status code, duration |
| **HEADERS** | BASIC + request and response headers    |
| **FULL**    | HEADERS + request and response bodies   |

**Example configuration:**
```yaml
folio:
  exchange:
    enabled: true
  logging:
    request:
      enabled: true
      level: HEADERS     # For incoming requests
    exchange:
      enabled: true
      level: BASIC       # For outgoing HTTP client requests

logging:
  level:
    org.folio.spring.filter.LoggingRequestFilter: DEBUG
    org.folio.spring.client.ExchangeLoggingInterceptor: DEBUG
```

**Notes:**
- `folio.exchange.enabled: true` is **REQUIRED**
- Logging configuration is optional
- See [HTTP_CLIENT_LOGGING.md](doc/HTTP_CLIENT_LOGGING.md) for detailed configuration

---

## Step 3: Update Client Interfaces

### 3.1 Simple GET Request Example

**Before:**
```java
package org.folio.module.client;

import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "users", dismiss404 = true)
public interface UsersClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  Optional<UserDto> fetchUserById(@PathVariable("id") String id);

  record UserDto(String id, String username, String email) { }
}
```

**After:**
```java
package org.folio.module.client;

import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface UsersClient {

  @GetExchange(value = "/{id}")
  Optional<UserDto> fetchUserById(@PathVariable("id") String id);

  record UserDto(String id, String username, String email) { }
}
```

### 3.2 POST/PUT Request Example

**Before:**
```java
package org.folio.module.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "inventory")
public interface InventoryClient {

  @PostMapping(value = "/items", 
               consumes = MediaType.APPLICATION_JSON_VALUE,
               produces = MediaType.APPLICATION_JSON_VALUE)
  ItemDto createItem(@RequestBody ItemDto item);

  @PutMapping(value = "/items/{id}")
  void updateItem(@PathVariable("id") UUID id, @RequestBody ItemDto item);

  record ItemDto(UUID id, String title, String barcode) { }
}
```

**After:**
```java
package org.folio.module.client;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "inventory", 
              contentType = MediaType.APPLICATION_JSON_VALUE,
              accept = MediaType.APPLICATION_JSON_VALUE)
public interface InventoryClient {

  @PostExchange(value = "/items")
  ItemDto createItem(@RequestBody ItemDto item);

  @PutExchange(value = "/items/{id}")
  void updateItem(@PathVariable("id") UUID id, @RequestBody ItemDto item);

  record ItemDto(UUID id, String title, String barcode) { }
}
```

### 3.3 Annotation Mapping Reference

| OpenFeign                         | Spring HTTP Service              | Notes                                                   |
|-----------------------------------|----------------------------------|---------------------------------------------------------|
| `@FeignClient(value = "service")` | `@HttpExchange(url = "service")` | No `http://` prefix needed                              |
| `@GetMapping`                     | `@GetExchange`                   |                                                         |
| `@PostMapping`                    | `@PostExchange`                  |                                                         |
| `@PutMapping`                     | `@PutExchange`                   |                                                         |
| `@DeleteMapping`                  | `@DeleteExchange`                |                                                         |
| `@PatchMapping`                   | `@PatchExchange`                 |                                                         |
| `produces`                        | `accept`                         |                                                         |
| `consumes`                        | `contentType`                    |                                                         |
| `dismiss404 = true`               | _(not needed)_                   | Use `Optional<>`. Optional will be empty in case of 404 |

### 3.4 Keep These Annotations Unchanged

- ✅ `@RequestBody` - is required for interfaces were the body is used
- ✅ `@PathVariable`
- ✅ `@RequestParam`
- ✅ `@RequestHeader`

---

## Step 4: Update Main Application Class

**Before:**
```java
package org.folio.module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
public class ModuleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModuleApplication.class, args);
  }
}
```

**After:**
```java
package org.folio.module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ModuleApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModuleApplication.class, args);
  }
}
```

**Changes:**
- ❌ Remove `import org.springframework.cloud.openfeign.EnableFeignClients;`
- ❌ Remove `@EnableFeignClients` annotation

---

## Step 5: Configure HTTP Service Clients

Create a new configuration class to register your HTTP service clients.

**HttpClientConfiguration.java:**
```java
package org.folio.module.config;

import org.folio.module.client.InventoryClient;
import org.folio.module.client.UsersClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpClientConfiguration {

  @Bean
  public UsersClient usersClient(HttpServiceProxyFactory factory) {
    return factory.createClient(UsersClient.class);
  }

  @Bean
  public InventoryClient inventoryClient(HttpServiceProxyFactory factory) {
    return factory.createClient(InventoryClient.class);
  }

  // Add a bean for each HTTP service client interface
}
```

**Important Notes:**
- ✅ The `HttpServiceProxyFactory` bean is provided by `folio-spring-base`
- ✅ Each client interface gets a dedicated bean
- ✅ All clients automatically use the `EnrichUrlAndHeadersInterceptor`
- ✅ No additional configuration needed for Okapi headers or URL enrichment

---

## Step 6: Update Exception Handling

### 6.1 Update Exception Handler

**Before:**
```java
package org.folio.module.controller;

import static feign.Util.UTF_8;
import feign.FeignException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandling {

  @ExceptionHandler(FeignException.class)
  public Error handleFeignException(FeignException e, HttpServletResponse response) {
    var status = e.status();
    if (status != -1) {
      var message = e.responseBody()
        .map(byteBuffer -> new String(byteBuffer.array(), UTF_8))
        .orElse(e.getMessage());
      response.setStatus(status);
      return buildError(status, message);
    } else {
      response.setStatus(500);
      return buildError(500, e.getMessage());
    }
  }
}
```

**After:**
```java
package org.folio.module.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;

@RestControllerAdvice
public class ErrorHandling {

  @ExceptionHandler(HttpStatusCodeException.class)
  public Error handleHttpStatusException(HttpStatusCodeException e, HttpServletResponse response) {
    var status = e.getStatusCode().value();
    var message = e.getResponseBodyAsString();

    if (message.isEmpty()) {
      message = e.getMessage();
    }

    response.setStatus(status);
    return buildError(status, message);
  }
}
```

### 6.2 Exception Mapping Reference

| OpenFeign | Spring HTTP | Method Changes |
|-----------|-------------|----------------|
| `FeignException` | `HttpStatusCodeException` | |
| `e.status()` | `e.getStatusCode().value()` | |
| `e.responseBody().map(...)` | `e.getResponseBodyAsString()` | |
| `FeignException.NotFound` | `HttpClientErrorException.NotFound` | |
| `FeignException.UnprocessableEntity` | `HttpClientErrorException.UnprocessableEntity` | |

**Changes:**
- ✅ Replace `FeignException` with `HttpStatusCodeException`
- ✅ Use `e.getStatusCode().value()` instead of `e.status()`
- ✅ Use `e.getResponseBodyAsString()` instead of `e.responseBody()`
- ✅ Simplified exception handling logic

---

## Step 7: Update Jackson Imports

Spring Boot 4.0 uses Jackson 3.x, which moved from `com.fasterxml.jackson` to `tools.jackson` package.

### 7.1 Update ObjectMapper and Exception Imports

**Before:**
```java
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
  
  public static String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize", e);
    }
  }

  public static <T> T fromJson(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to deserialize", e);
    }
  }
}
```

**After:**
```java
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class JsonUtils {
  
  public static String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize", e);
    }
  }

  public static <T> T fromJson(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to deserialize", e);
    }
  }
}
```

### 7.2 Jackson Migration Reference

| Before (Jackson 2.x)                 | After (Jackson 3.x)          |
|--------------------------------------|------------------------------|
| `com.fasterxml.jackson.core.*`       | `tools.jackson.core.*`       |
| `com.fasterxml.jackson.databind.*`   | `tools.jackson.databind.*`   |
| `JsonProcessingException`            | `JacksonException`           |

---


---

## Troubleshooting

### Problem: Tests Fail with 404 Errors

**Symptom:**
```
HttpClientErrorException$NotFound: 404 Not Found
```

**Possible Causes:**
1. WireMock stubs not configured correctly
2. Client URL missing base path
3. HTTP exchange configuration incorrect

**Solutions:**
1. Verify WireMock stubs use correct paths
2. Check that `@HttpExchange(url = "service-name")` uses service name only
3. Ensure `folio.exchange.enabled: true` is set

### Problem: Bean Creation Errors

**Symptom:**
```
BeanCreationException: Error creating bean with name 'httpServiceProxyFactory'
```

**Solution:**
Add to `application.yml`:
```yaml
folio:
  exchange:
    enabled: true
```

### Problem: Jackson Serialization Errors

**Symptom:**
```
NoClassDefFoundError: com/fasterxml/jackson/databind/ObjectMapper
```

**Solution:**
Update all Jackson imports from `com.fasterxml.jackson` to `tools.jackson`

### Problem: Logging Not Working

**Symptom:**
Request/response logging not appearing in logs

**Solutions:**
1. Verify logging is enabled in configuration:
   ```yaml
   folio:
     logging:
       exchange:
         enabled: true
   ```
2. Set DEBUG level for logging classes:
   ```yaml
   logging:
     level:
       org.folio.spring.client.ExchangeLoggingInterceptor: DEBUG
       org.folio.spring.filter.LoggingRequestFilter: DEBUG
   ```

---

## Additional Resources

### Documentation
- [Spring HTTP Interface (HTTP Service Client)](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-service-client) - **Official Spring documentation for HTTP Service Clients**
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) - **Official Spring Boot migration guide for cases not covered here**
- [Spring Boot 4.0 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes)
- [Jackson 3.0 Migration Guide](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.0)
- [HTTP Client Logging Guide](REQUEST_LOGGING.md) - Detailed logging configuration
