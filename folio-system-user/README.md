# Documentation for folio-service-tools-spring-dev features

## System user creation and utilization

### Creation

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
    * login.item.post
    * perms.users.get
    * perms.users.item.post
    * perms.users.assign.immutable
    * perms.users.assign.mutable
* Update ModuleDescriptor with requires interfaces:
    * login
    * permissions
    * users

### Utilization

If system user was created during enabling for tenant, then the system user could be used to make request
to other modules. To do so `SystemUserScopedExecutionService` could be used.

****