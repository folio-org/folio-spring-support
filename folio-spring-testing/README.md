# folio-spring-testing

<!-- TOC -->
* [folio-spring-testing](#folio-spring-testing)
  * [Introduction](#introduction)
  * [Adding this submodule to your project](#adding-this-submodule-to-your-project)
  * [Usage](#usage)
    * [KafkaContainerExtension](#kafkacontainerextension)
    * [OkapiExtension](#okapiextension)
    * [PostgresContainerExtension](#postgrescontainerextension)
    * [RandomParametersExtension](#randomparametersextension)
    * [DatabaseCleanupExtension](#databasecleanupextension)
    * [UnitTest and IntegrationTest Annotations](#unittest-and-integrationtest-annotations)
<!-- TOC -->

## Introduction

This submodule is designed to facilitate test management by providing various extensions and utilities for testing purposes.

## Adding this submodule to your project

To add this module to your project, add the following to your `pom.xml` in `<dependencies>`:

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-spring-testing</artifactId>
  <version>${folio-spring-support.version}</version>
  <scope>test</scope>
</dependency>
```
## Usage

### KafkaContainerExtension
The KafkaContainerExtension class manages a Kafka Docker container for testing. To utilize this extension, annotate test classes or methods with `@EnableKafka`.
Extension populates `spring.kafka.bootstrap-servers` env variable.

### OkapiExtension
The OkapiExtension class handles an Okapi WireMock server. To activate this extension, annotate test classes or methods with `@EnableOkapi`. 
Extension populates `folio.okapi-url` env variable and configures `OkapiConfiguration` into test class. 

### PostgresContainerExtension
The PostgresContainerExtension class manages a PostgreSQL Docker container. To employ this extension, annotate test classes or methods with `@EnablePostgres`.
Extension populates `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` env variables.

### RandomParametersExtension
The RandomParametersExtension class resolves parameters annotated with `@Random`, `@RandomInt`, or `@RandomLong` by generating random values for testing purposes.

### DatabaseCleanupExtension
The DatabaseCleanupExtension class offers database cleanup functionality after test execution. 
It can be activated by annotating test methods or test classes with `@DatabaseCleanup(tables = {...}, tenants = {...})` to specify tables and tenants for cleanup.


### UnitTest and IntegrationTest Annotations
`@UnitTest`: Use this annotation to mark test classes or methods as unit tests focusing on individual units or components.
`@IntegrationTest`: Use this annotation to mark test classes or methods as integration tests involving multiple components or systems.
