# folio-spring-base

Copyright (C) 2020 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

This is a library (jar) that contains the basic functionality and main dependencies required for development FOLIO modules using Spring framework.

## Properties

| Property | Description | Example |
| -------- | ----------- | --------|
| `header.validation.x-okapi-tenant.exclude.base-paths` | Specifies base paths to exclude form `x-okapi-tenant` header validation.  See [TenantOkapiHeaderValidationFilter.java](src/main/java/org/folio/spring/filter/TenantOkapiHeaderValidationFilter.java)  | `/admin,/swagger-ui` |
