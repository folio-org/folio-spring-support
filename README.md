# folio-spring-base

Copyright (C) 2020 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

This is a library (jar) that contains the basic functionality and main dependencies required for development FOLIO modules using Spring framework.

# Supported features

## Setup a system user for a module

It is possible to register a system user for the module with desired permissions.
Please add following properties to your application property file:
```yaml
application:
  system-user:
    username: <username-for-the-user>
    password: ${SYSTEM_USER_PASSWORD}
    lastname: <lastname> # e.g. System
    permissionsFilePath: <path-to-csv-file-with-permissions> # e.g. classpath:user-permissions.csv
```

Please note that `username` should be unique for FOLIO. Also, make sure to add following 
API dependencies to your module descriptor file:
* `users`
* `login`
* `permissions`

The `POST` `/_/tenant` must have permissions:
```javascript
"modulePermissions": [
    "users.collection.get",
    "users.item.post",
    "login.item.post",
    "perms.users.item.post"
]
```

Permissions file is a simple CSV file with each permission on a separate file, e.g.:
```
inventory-storage.instance.item.post
inventory-storage.instance.item.get
```

### Storing system user parameters in DB vs in memory

If your module supports DB (i.e. there is a dataSource bean) we will create a new table `system_user_parameters` 
to persist user credentials caching is also supported if it is enabled by the module.

If DB is not supported, then credentials are stored in memory.

### System user naming convention

Please use following rules for your module system user:
* `lastname` should be specified, it can be `System`, `Automated process`, `etc` value. 
It is required to have this property so that it is properly displayed on UI in 'actions' 
table (e.g. loan actions, fee/fine actions, etc);
* `username` should be module name or a domain identifier (e.g. `mod-search`, `pub-sub`, etc.).

### Security concerns

The user password **must not** be committed directly to git, please inject them via env variable (e.g. 
for spring `application.system-user.password: ${SYSTEM_USER_PASSWORD}`) or in some another secure way.
