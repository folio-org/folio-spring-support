## 9.0.3

### folio-spring-base
* [FOLSPRINGS-214](https://folio-org.atlassian.net/browse/FOLSPRINGS-214) Bump Spring Boot from 3.4.3 to 3.4.11 fixing vulns

## 9.0.2 2025-11-18

### folio-spring-base
* [FOLSPRINGS-200](https://folio-org.atlassian.net/browse/FOLSPRINGS-200) commons-fileupload 1.6.0 fixing DoS from unlimited multipart headers CVE-2025-48976

## 9.0.1 2025-03-20

### cql submodule
* [FOLSPRINGS-185](https://folio-org.atlassian.net/browse/FOLSPRINGS-185) Implement case insensitive accents ignoring CQL queries - backport, opt in only. To opt in call Cql2JpaCriteria.setCaseAccentsHandlingEnabled(true). Then a CQL search in a `String` field ignores case (= is case insensitive) and ignores accents by default; this is for consistency with <a href="https://github.com/folio-org/raml-module-builder?tab=readme-ov-file#the-post-tenant-api">RMB based modules</a>. Use the annotations `@RespectCase` and/or `@RespectAccents` in the entity class to change this new default.

When enabling case insensitive and/or accents ignoring CQL queries you need to update database indices accordingly,
for example:

```
DROP INDEX IF EXISTS idx_medreq_requester_barcode;
CREATE INDEX idx_medreq_requester_barcode ON ${database.defaultSchemaName}.mediated_request(lower(f_unaccent(requester_barcode)));
```

## 9.0.0 2025-02-28
* [FOLSPRINGS-188](https://folio-org.atlassian.net/browse/FOLSPRINGS-188) Upgrade to Java 21
* [FOLSPRINGS-178](https://folio-org.atlassian.net/browse/FOLSPRINGS-178) spring-cloud-starter-openfeign 4.1.4 fixing spring-security-crypto Authorization Bypass

### cql submodule
* [FOLSPRINGS-185](https://folio-org.atlassian.net/browse/FOLSPRINGS-185) Implement case insensitive accents ignoring CQL queries

### folio-spring-system-user
* [FOLSPRINGS-180](https://folio-org.atlassian.net/browse/FOLSPRINGS-180) Token expiration off by 1 minute in test
* [FOLSPRINGS-183](https://folio-org.atlassian.net/browse/FOLSPRINGS-183) Improve resiliency of system user operations; auto-reactivate inactive system users; automatically refresh system user tokens on RTR expiration

#### Upgrade instructions

In the module descriptor add `users.item.put` to the `modulePermissions` list of the `POST /_/tenant` endpoint ([FOLSPRINGS-183](https://folio-org.atlassian.net/browse/FOLSPRINGS-183)).

## 8.2.1 2024-10-23
### Testing submodule
* [FOLSPRINGS-172](https://folio-org.atlassian.net/browse/FOLSPRINGS-172) Bump to postgres:16-alpine, suggest adding .github/workflows/postgres.yml

### folio-spring-system-user
* [FOLSPRINGS-179](https://folio-org.atlassian.net/browse/FOLSPRINGS-179) Fix error message: Cannot retrieve okapi token for tenant: \<username\>

## 8.2.0 2024-10-11
* [FOLSPRINGS-164](https://folio-org.atlassian.net/browse/FOLSPRINGS-164) Add "Update NEWS.md" to PULL\_REQUEST\_TEMPLATE.md

### Testing submodule
* [FOLSPRINGB-152](https://issues.folio.org/browse/FOLSPRINGB-152) Implement TESTCONTAINERS\_POSTGRES\_IMAGE
* [FOLSPRINGS-166](https://folio-org.atlassian.net/browse/FOLSPRINGS-166) Upgrade classgraph dependency from 4.8.90 to 4.8.175 fixing CVE-2021-47621

### folio-spring-system-user
* [FOLSPRINGS-157](https://issues.folio.org/browse/FOLSPRINGS-157) Add missing property to authn client, to allow for `fail-on-unknown-properties` in consuming modules

### i18n submodule
* [FOLSPRINGB-160](https://issues.folio.org/browse/FOLSPRINGB-160) Make translation service accept multiple keys
* [FOLSPRINGS-163](https://folio-org.atlassian.net/browse/FOLSPRINGS-163) Wrong Locale in TranslationService and TranslationMap
* [FOLSPRINGS-169](https://folio-org.atlassian.net/browse/FOLSPRINGS-169) Add additional convenience methods for providing locales, timezones, and using non-predefined templates

## 8.1.0 2024-02-29
* [FOLSPRINGB-144](https://issues.folio.org/browse/FOLSPRINGB-144) Add support for filtering by undefined field value
* [FOLSPRINGB-148](https://issues.folio.org/browse/FOLSPRINGB-148) Add Minio extension for testing submodule

## 8.0.0 2024-01-19
* [FOLSPRINGB-128](https://issues.folio.org/browse/FOLSPRINGB-128) System User POC
* [FOLSPRINGB-139](https://issues.folio.org/browse/FOLSPRINGB-139) Pass Accept-Language header in Feign clients
* [FOLSPRINGB-140](https://issues.folio.org/browse/FOLSPRINGB-140) Add Testing Submodule for Test Management
* [FOLSPRINGB-141](https://issues.folio.org/browse/FOLSPRINGB-141) Upgrade deps for Quesnelia
* [FOLSPRINGB-142](https://issues.folio.org/browse/FOLSPRINGB-142) Add support for Timestamp type field in Cql query
* [FOLSPRINGB-138](https://issues.folio.org/browse/FOLSPRINGB-138) Use more resilient method for translation module name extraction
* [FOLSPRINGB-137](https://issues.folio.org/browse/FOLSPRINGB-137) TranslationService javadoc: cannot find symbol onConstructor_ = @Autowired
* [FOLSPRINGB-134](https://issues.folio.org/browse/FOLSPRINGB-134) Create centralized internationalization library

## 7.2.2 2023-11-09
* [FOLSPRINGB-132](https://issues.folio.org/browse/FOLSPRINGB-132) Use half of access token expiration as user cache expiration

## 7.2.1 2023-11-06
* [FOLSPRINGB-126](https://issues.folio.org/browse/FOLSPRINGB-126) Reject empty and null system user password
* [FOLSPRINGB-127](https://issues.folio.org/browse/FOLSPRINGB-127) Update existing system-user credentials based on provided system user environment
* [FOLSPRINGB-130](https://issues.folio.org/browse/FOLSPRINGB-130) Dependencies update: Spring Boot 3.1.5, Spring Cloud Openfeign 4.0.4

## 7.2.0 2023-10-03
* [FOLSPRINGB-115](https://issues.folio.org/browse/FOLSPRINGB-115) Add submodule for creating and utilizing system-user
* [FOLSPRINGB-118](https://issues.folio.org/browse/FOLSPRINGB-118) Implement refresh token rotation method for edge module

## 7.1.0 2023-06-19
* [FOLSPRINGB-96](https://issues.folio.org/browse/FOLSPRINGB-96) Add executeWithinContext method
* [FOLSPRINGB-106](https://issues.folio.org/browse/FOLSPRINGB-106) Implementation for store/restore of FolioExecutionContext
* [FOLSPRINGB-108](https://issues.folio.org/browse/FOLSPRINGB-108) Upgrade dependencies for Poppy, fix vulnerabilities

## 7.0.0 2023-04-25
* [FOLSPRINGB-104](https://issues.folio.org/browse/FOLSPRINGB-104) Split CQL support into a separate artifact

## 6.1.0 2023-03-20
* [FOLSPRINGB-95](https://issues.folio.org/browse/FOLSPRINGB-95) non-public beginFolioExecutionContext avoids wrong tenant/user
* [FOLSPRINGB-99](https://issues.folio.org/browse/FOLSPRINGB-99) Add support for filtering by date range in JpaCqlRepository

## 6.0.2 2023-03-08
* [FOLSPRINGB-94](https://issues.folio.org/browse/FOLSPRINGB-94) Broken queryByLike masking, SQL injection
* [#bb353da](https://github.com/folio-org/folio-spring-support/commit/bb353dafbd0d6ca66f827d4933f66b24b4c8a09c) Fix issue with the race condition with CqlParser shared object

## 6.0.1 2023-02-08
* [FOLSPRINGB-89](https://issues.folio.org/browse/FOLSPRINGB-89) postgresql 42.5.3 (SocketException: Too many open files)

## 6.0.0 2023-02-06
* [FOLSPRINGB-45](https://issues.folio.org/browse/FOLSPRINGB-45) Improve logging
* [FOLSPRINGB-81](https://issues.folio.org/browse/FOLSPRINGB-81) Migration to Spring Boot v3.0.2
* Support for async execution using correct instance of FolioExecutionContext
* Update to Java 17

## 5.0.0 2022-10-04
 * [FOLSPRINGB-58](https://issues.folio.org/browse/FOLSPRINGB-58) Async FolioExecutionContext helper
 * [FOLSPRINGB-72](https://issues.folio.org/browse/FOLSPRINGB-72) Fixed fails if limit <= number of results on CQL-search

Breaking change: Replace calls to `FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext` and  `FolioExecutionScopeExecutionContextManager.endFolioExecutionContext` with `FolioExecutionContextSetter` usage.

## 4.1.0 2022-05-30
* [FOLSPRINGB-52](https://issues.folio.org/projects/FOLSPRINGB/issues/FOLSPRINGB-52) Spring Boot 2.7.x for Morning Glory R2 2022
* [FOLSPRINGB-53](https://issues.folio.org/projects/FOLSPRINGB/issues/FOLSPRINGB-53) Upgrade rhino and plexus-utils (CVE-2017-1000487)

## 4.0.0 2022-02-15
 * [FOLSPRINGB-42](https://issues.folio.org/projects/FOLSPRINGB/issues/FOLSPRINGB-42) Spring Boot 2.6.3, log4j-core 2.17.1 (CVE-2021-44832) 
 * [FOLSPRINGB-41](https://issues.folio.org/projects/FOLSPRINGB/issues/FOLSPRINGB-41) Added event methods to allow custom module logic on `/_/tenant`

## 3.0.0 2022-01-18
 * FOLSPRINGB-17 Migrate to Tenant API v2.0
 * FOLSPRINGB-32 Upgrade to Spring Boot v2.6.2

## 2.0.0 2021-09-01
 * FOLSPRINGB-19 Setup default logging
 * FOLSPRINGB-21 Add CQL support
 * FOLSPRINGB-23 Remove deprecated method getUserName() from FolioExecutionContext
 * FOLSPRINGB-25 Upgrade Spring Boot to v2.5.2

## 1.0.5 2021-03-25
 * FOLSPRINGB-15 Use x-okapi-user-id header to populate userId

## 1.0.4 2021-03-10
 * FOLSPRINGB-14 Fetch userId and userName from `x-okapi-token`

## 1.0.3 2021-03-04
 * FOLSPRINGB-13 Don't require x-okapi-tenant for admin endpoints

## 1.0.2 2021-03-01
 * FOLSPRINGB-11 Fix issue with unsatisfied dependency

## 1.0.1 2021-02-02
 * Fix spring data source adviser for proxy classes
 * Correct handling for Constraint Violation exceptions
 * Add equals()/hashCode()/toString() to OffsetRequest
 * Spring boot log4j2 dependency

## 1.0.0 2020-11-26
 * Initial module setup
