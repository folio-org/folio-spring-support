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
