# Documentation for folio-spring-system-user features

## Creation

If module need system user to communicate with other modules then it's required to create the system
user on enabling for tenant. To do so you should:

1. Extend `TenantService` from folio-spring-base
2. Inject `PrepareSystemUserService` bean to the class
3. Override `afterTenantUpdate` and use `setupSystemUser()` from injected service
4. Inject `SystemUserService` bean to the class 
5. Call `authSystemUser(SystemUser user)` for login with RTR-based approach 

Requirements:

* Prepare file with permissions that should be assigned to the user (one permission per line)
* Set-up application properties:
    * folio.okapi-url (suggested to pass it from environment variables)
    * folio.system-user.username (suggested to have the same name as module name)
    * folio.system-user.password (suggested to pass it from environment variables)
    * folio.system-user.lastname (suggested to set it to `System`)
    * folio.system-user.permissionsFilePath (path to prepared permissions-file in resources folder)
* Add `spring-boot-starter-cache` dependency to module if you want to cache system user authentication data
* Update ModuleDescriptor with modulePermissions for `POST /_/tenant` endpoint:
    * users.collection.get
    * users.item.post
    * users.item.put
    * login.item.post
    * login.item.delete
    * perms.users.get
    * perms.users.item.post
    * perms.users.assign.immutable
    * perms.users.assign.mutable
* Update ModuleDescriptor with requires interfaces:
    * login
    * permissions
    * users
* Add `caffeine` as a dependency to your module, if you want to use [caching](#notes-about-caching)

## Utilization

If system user was created during enabling for tenant, then the system user could be used to make request
to other modules. To do so `SystemUserScopedExecutionService` could be used.

## Notes about caching

A cache can be used to prevent excessive re-authentications of the system user. To use this cache, your project must include the `caffeine` dependency:

```xml
<dependency>
  <groupId>com.github.ben-manes.caffeine</groupId>
  <artifactId>caffeine</artifactId>
  <version>${caffeine version goes here}</version>
</dependency>
```

If the dependency is available in your project, the cache will be automatically configured.

## Notes about RTR

Refresh token rotation affects system user logins, just as any other logged-in user. This library attempts to mitigate this by automatically re-authenticating the system user if the token is expiring in the next thirty seconds and populating new tokens in the `FolioExecutionContext`. This should cover the majority of use cases, and makes it possible to do many requests over a longer period within one call of the `SystemUserScopedExecutionService`. However, any manual handling or use of the token within the context should be done with extra care, and this library provides no guarantees for this type of use.

## Disable system user functionality

Setting property `folio.system-user.enabled=false` will disable system user functionality:
* all actions called using `SystemUserScopedExecutionService` will be performed in `DefaultFolioExecutionContext`
* `X-Okapi-Token` header won't be populated with system user JWT token value
* All unused Spring components will be excluded from Spring context, including:
  * `AuthnClient`
  * `PermissionsClient`
  * `UsersClient`
  * `SystemUserService`
