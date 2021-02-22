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
    username: folio-spring-base
    password: folio-spring-base-pwd
    lastname: System
    permissionsFilePath: classpath:user-permissions.csv
```

Please note that `username` should be unique for FOLIO. Also, make sure to add following 
API dependencies to your module descriptor file:
* `users`
* `login`
* `permissions`

Permissions file is a simple CSV file with each permission on a separate file, e.g.:
```
inventory-storage.instance.item.post
inventory-storage.instance.item.get
```

We also create a new table `system_user_parameters` to persist user credentials. If your module
supports caching than it will be automatically applied for the `system_user_parameters` table.
