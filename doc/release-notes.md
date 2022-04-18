## Spring-base release notes

### Version 5.0.0
1. Database search_path was changed to only tenant-related schema, no public schema now. If it's needed to query from public schema, then it's required now to specify public schema in SQL-query.
It's also related to Liquibase scripts.
For example, if `pgcrypto` (should be installed to public schema) is used to generate UUIDs, then scripts should be changed:
```xml
    <changeSet id="id123" runOnChange="true">
        <addDefaultValue tableName="tags" columnName="id" defaultValueComputed="public.gen_random_uuid()"/>
    </changeSet>
```

### Version 4.0.0
1. Approach of specifying custom `/_/tenant` logic was changed. Details provided in [README](../README.md#custom-_tenant-logic)

### Version 3.0.0
Modules that use spring-base library have to do some actions when upgrading to the 2.0.0 version.
1. Update `spring-boot-starter-parent` version in pom.xml
```xml
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.6.2</version>
    <relativePath /> <!-- lookup parent from repository -->
  </parent>
```
2. Update `folio-spring-base` version in pom.xml
```xml
    <dependency>
      <groupId>org.folio</groupId>
      <artifactId>folio-spring-base</artifactId>
      <version>3.0.0</version>
    </dependency>
```
3. Module now supports Tenant API v2.0. Module descriptors updating is required:
```json
    {
      "id": "_tenant",
      "version": "2.0",
      "interfaceType": "system",
      "handlers": [
        {
          "methods": [ "POST" ],
          "pathPattern": "/_/tenant"
        },
        {
          "methods": [ "GET", "DELETE" ],
          "pathPattern": "/_/tenant/{id}"
        }
      ]
    }
```
4. If overridden `folioTenantController` exist in the module then changes in it is required:
   - replace `@RequestMapping(value = "/_/")` to `@RequestMapping`
   - override and extend methods `upgradeTenant`, `disableTenant`, `loadReferenceData`, `loadSampleData` if needed

### Version 2.0.0
Modules that use spring-base library have to do some actions when upgrading to the 2.0.0 version.

 1. Update `spring-boot-starter-parent` version in pom.xml
```xml
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.5.2</version>
    <relativePath /> <!-- lookup parent from repository -->
  </parent>
```
 2. Update `folio-spring-base` version in pom.xml
```xml
    <dependency>
      <groupId>org.folio</groupId>
      <artifactId>folio-spring-base</artifactId>
      <version>2.0.0</version>
    </dependency>
```
 3. `getUserName()` method is no longer supported in `FolioExecutionContext`. To retrieve username you could create feign client and use it to fetch needed user's data from `mod-users` by user's ID that is available in `FolioExecutionContext` by method `getUserId()`.
```java
@FeignClient(value = "users")
public interface UsersClient {

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  Optional<UserDto> fetchUserById(@PathVariable("id") String id);

  @Value
  class UserDto {

    String id;
    String username;
  }
}
```
 4. The default configurations for Folio log4j2 are introduced: `log4j2.properties` for plain and `log4j2-json.properties` for json format. So, you could remove log4j2 configurations from your module. See details in [README](../README.md#default-logging-format).
 5. Default logging for incoming and outgoing requests introduced. You should decide whether to use this mechanism or not. See details in [README](../README.md#logging-for-incoming-and-outgoing-requests).
