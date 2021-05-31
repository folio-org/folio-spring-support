# folio-spring-base

Copyright (C) 2020 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

This is a library (jar) that contains the basic functionality and main dependencies required for development FOLIO modules using Spring framework.

## Properties

| Property | Description | Default | Example |
| -------- | ----------- | --------| --------|
| `header.validation.x-okapi-tenant.exclude.base-paths` | Specifies base paths to exclude form `x-okapi-tenant` header validation.  See [TenantOkapiHeaderValidationFilter.java](src/main/java/org/folio/spring/filter/TenantOkapiHeaderValidationFilter.java) | `/admin` | `/admin,/swagger-ui` |
| `folio.jpa.repository.base-packages` | Specifies base packages to scan for repositories  | `org.folio.*` | `org.folio.qm.dao` |

## CQL support
To have ability to search entities in databases by CQL-queries:
 * create repository interface for needed entity 
 * extend it from `JpaCqlRepository<T, ID>`, where `T` is entity class and `ID` is entity's id class.
 * the implementation of the repository will be created by Spring
```java
public interface PersonRepository extends JpaCqlRepository<Person, Integer> {

}
```

Two methods are available for CQL-queries:
```java
public interface JpaCqlRepository<T, ID> extends JpaRepository<T, ID> {

  Page<T> findByCQL(String cql, OffsetRequest offset);

  long count(String cql);
}
```