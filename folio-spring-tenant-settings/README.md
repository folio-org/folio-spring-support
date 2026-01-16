# FOLIO Spring Tenant Settings

A Spring Boot library module for managing tenant-specific configuration settings in FOLIO applications.

## Overview

This module provides a RESTful API for managing configuration settings organized into groups. It supports:

- Configuration groups organized by domain (e.g., `authorities`, `audit.authority`, `audit.inventory`)
- Settings with different value types (BOOLEAN, INTEGER, STRING)
- Read and Update operations (GET and PATCH endpoints)
- JPA/PostgreSQL persistence with text value storage
- Metadata tracking (created/updated dates and user IDs)
- Permission-based access control for settings operations

## API Endpoints

The base path is determined by the `folio.tenant.settings.domain` property (e.g., `/authorities`).

| Method | Endpoint | Description | Required Permission |
|--------|----------|-------------|---------------------|
| GET | `/{domain}/config/groups` | List all configuration groups | None |
| GET | `/{domain}/config/groups/{groupId}/settings` | Get all settings for a specific group | `{domain}.config.groups.settings.{groupId}.collection.get` |
| PATCH | `/{domain}/config/groups/{groupId}/settings/{key}` | Update a specific setting | `{domain}.config.groups.settings.{groupId}.{key}.item.patch` |

**Example:** If `domain=authorities`, endpoints are:
- `GET /authorities/config/groups`
- `GET /authorities/config/groups/authorities/settings`
- `PATCH /authorities/config/groups/authorities/settings/mapping.extended`

## Integration

### 1. Add Dependency

Add the following dependency to your module's `pom.xml`:

```xml
<dependency>
    <groupId>org.folio</groupId>
    <artifactId>folio-spring-tenant-settings</artifactId>
    <version>${folio-spring-support.version}</version>
</dependency>
```

### 2. Configure Application Properties

Add the tenant settings configuration to your `application.yaml`:

```yaml
folio:
  tenant:
    settings:
      domain: authorities  # Your domain name (lowercase, hyphens allowed)
      permission-check-enabled: true  # Enable/disable permission checks (default: true)
```

**Important:** The `domain` property:
- Must match the pattern `[a-z][a-z-]+[a-z]` (lowercase letters and hyphens only)
- Defines the base path for the API endpoints (e.g., `/authorities/config/groups`)
- Is used to construct permission strings

### 3. Database Migration

Include the tenant-settings changelog in your Liquibase master changelog file:

**File:** `src/main/resources/db/changelog/changelog-master.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
  xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.5.xsd">

  <!-- Your existing changesets -->
  
  <!-- Include tenant-settings module database schema -->
  <include file="classpath:db/tenant-settings/changelog.xml"/>
  
  <!-- Your custom tenant settings initialization (optional) -->
  <include file="/changes/v5.0/populate-default-tenant-settings.xml" relativeToChangelogFile="true"/>
  
</databaseChangeLog>
```

**Note:** The tenant-settings module creates the `setting_group` and `setting` tables. You need to populate them with your default settings using custom changesets.

### 4. Component Scan Configuration

The module uses auto-configuration via `TenantSettingsConfig`. Ensure your Spring Boot application can scan the tenant-settings package:

#### Option A: Default Package Scanning (Recommended)

If your main application class is in a parent package of `org.folio`, Spring Boot will automatically scan the tenant-settings components:

```java
package org.folio.entlinks;  // Parent of org.folio.tenant.settings

@SpringBootApplication
public class EntityLinksApplication {
    public static void main(String[] args) {
        SpringApplication.run(EntityLinksApplication.class, args);
    }
}
```

#### Option B: Explicit Component Scan

If your application is in a different package, explicitly add the tenant-settings package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "your.application.package",
    "org.folio.tenant.settings"
})
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

**Note:** The `TenantSettingsConfig` class already configures:
- `@EnableJpaRepositories` for `org.folio.tenant.settings.repository`
- `@EntityScan` for `org.folio`
- `@ComponentScan` for `org.folio.tenant.settings`
- Configuration properties binding

### 5. Populate Default Settings (Required)

Create a Liquibase changeset to populate your default settings:

**Example:** `src/main/resources/db/changelog/changes/v5.0/populate-default-tenant-settings.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

  <changeSet id="populate-default-tenant-settings" author="system">
    <!-- Create a setting group -->
    <insert tableName="setting_group">
      <column name="id" value="authorities"/>
      <column name="name" value="Authorities"/>
      <column name="description" value="Authorities settings group"/>
    </insert>

    <!-- Add settings to the group -->
    <insert tableName="setting">
      <column name="key" value="mapping.extended"/>
      <column name="value" value='false'/>
      <column name="type" value="BOOLEAN"/>
      <column name="description" value="Enable extended mapping for authorities"/>
      <column name="group_id" value="authorities"/>
      <column name="created_date" valueComputed="CURRENT_TIMESTAMP"/>
      <column name="updated_date" valueComputed="CURRENT_TIMESTAMP"/>
    </insert>

    <insert tableName="setting">
      <column name="key" value="retention.days"/>
      <column name="value" value='30'/>
      <column name="type" value="INTEGER"/>
      <column name="description" value="Data retention period in days"/>
      <column name="group_id" value="authorities"/>
      <column name="created_date" valueComputed="CURRENT_TIMESTAMP"/>
      <column name="updated_date" valueComputed="CURRENT_TIMESTAMP"/>
    </insert>
  </changeSet>

</databaseChangeLog>
```

**Supported value types:**
- `BOOLEAN`: Store boolean values as `true` or `false` (no quotes)
- `INTEGER`: Store integer values as numbers (no quotes)
- `STRING`: Store string values with single quotes (e.g., `'my-value'`)

### 6. Update Module Descriptor (Required for Permission Checks)

If you keep `permission-check-enabled: true` (default), you **must** update your `ModuleDescriptor.json` to include:

1. **Endpoints** in the `provides` section - Declare all tenant settings API routes
2. **Permissions** in the `permissionSets` section - Define all permission names, display names, and descriptions

See the [Permissions](#permissions) section below for detailed module descriptor configuration examples.

**Note:** If you disable permission checks (`permission-check-enabled: false`), the module descriptor configuration is optional but still recommended for proper API documentation.

### 7. Update API Documentation (Optional)

To include tenant settings endpoints in your module's API documentation, you need to update api-doc.yml.
Add the step after "Checkout" step:
```yaml
      - name: Copy and update tenant-settings from dependency
        run: |
          mvn dependency:unpack-dependencies -DincludeArtifactIds=folio-spring-tenant-settings -DoutputDirectory=target/dependencies
          mkdir -p target/api/openapi

          # Extract domain value from application.yaml
          DOMAIN=$(grep -A 2 "tenant:" src/main/resources/application.yaml | grep "domain:" | sed 's/.*domain: *//' | tr -d ' ')

          # Copy and update tenant-settings.yaml with domain prefix
          python3 - <<EOF
          import yaml
          import sys

          domain = "${DOMAIN}"

          with open('target/dependencies/swagger.api/tenant-settings.yaml', 'r') as f:
              spec = yaml.safe_load(f)

          # Prepend domain to all paths
          if 'paths' in spec:
              new_paths = {}
              for path, methods in spec['paths'].items():
                  new_path = f"/{domain}{path}"
                  new_paths[new_path] = methods
              spec['paths'] = new_paths

          with open('target/api/openapi/tenant-settings.yaml', 'w') as f:
              yaml.dump(spec, f, default_flow_style=False, sort_keys=False)
          EOF
```

## Database Schema

The module creates the following tables:

### `setting_group`

| Column | Type | Description |
|--------|------|-------------|
| id | VARCHAR(255) | Primary key, group identifier (e.g., `audit.authority`) |
| name | VARCHAR(255) | Human-readable group name |
| description | TEXT | Group description |

### `setting`

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key, auto-generated |
| key | VARCHAR(255) | Setting key within the group |
| value | JSONB | Setting value (stored as JSON) |
| type | VARCHAR(50) | Value type: BOOLEAN, INTEGER, or STRING |
| description | TEXT | Setting description |
| group_id | VARCHAR(255) | Foreign key to setting_group |
| created_date | TIMESTAMP | Creation timestamp |
| created_by_user_id | UUID | User who created the setting |
| updated_date | TIMESTAMP | Last update timestamp |
| updated_by_user_id | UUID | User who last updated the setting |

**Constraints:**
- Unique constraint on `(group_id, key)` combination
- Foreign key from `setting.group_id` to `setting_group.id` with CASCADE delete
- Indexes on `group_id` and `key` for performance

## Usage Example

Assuming `folio.tenant.settings.domain=authorities`:

### Get All Configuration Groups

```bash
curl -X GET "http://localhost:8081/authorities/config/groups" \
  -H "X-Okapi-Tenant: diku"
```

**Response:**
```json
{
  "settingGroups": [
    {
      "id": "authorities",
      "name": "Authorities",
      "description": "Authorities settings group"
    }
  ],
  "totalRecords": 1
}
```

### Get Settings for a Group

```bash
curl -X GET "http://localhost:8081/authorities/config/groups/authorities/settings" \
  -H "X-Okapi-Tenant: diku" \
  -H "X-Okapi-Permissions: [\"authorities.config.groups.settings.authorities.collection.get\"]"
```

**Response:**
```json
{
  "settings": [
    {
      "key": "mapping.extended",
      "value": false,
      "type": "BOOLEAN",
      "description": "Enable extended mapping for authorities",
      "groupId": "authorities",
      "metadata": {
        "createdDate": "2026-01-13T01:53:39.496+00:00",
        "createdByUserId": null,
        "updatedDate": "2026-01-13T01:53:39.496+00:00",
        "updatedByUserId": null
      }
    }
  ],
  "totalRecords": 1
}
```

### Update a Setting

```bash
curl -X PATCH "http://localhost:8081/authorities/config/groups/authorities/settings/mapping.extended" \
  -H "Content-Type: application/json" \
  -H "X-Okapi-Tenant: diku" \
  -H "X-Okapi-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "X-Okapi-Permissions: [\"authorities.config.groups.settings.authorities.mapping.extended.item.patch\"]" \
  -d '{
    "value": true
  }'
```

**Response:**
```json
{
  "key": "mapping.extended",
  "value": true,
  "type": "BOOLEAN",
  "description": "Enable extended mapping for authorities",
  "groupId": "authorities",
  "metadata": {
    "createdDate": "2026-01-13T01:53:39.496+00:00",
    "createdByUserId": null,
    "updatedDate": "2026-01-13T10:15:22.123+00:00",
    "updatedByUserId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

## Permissions

The module implements permission-based access control (enabled by default). When `permission-check-enabled: true`, permissions are automatically derived from the domain and setting identifiers.

### Permission Patterns

#### Collection Access (GET settings)
Pattern: `{domain}.config.groups.settings.{groupId}.collection.get`

Example: `authorities.config.groups.settings.authorities.collection.get`

#### Item Update (PATCH setting)
Pattern: `{domain}.config.groups.settings.{groupId}.{key}.item.patch`

Example: `authorities.config.groups.settings.authorities.mapping.extended.item.patch`

### Module Descriptor Configuration

**Important:** When permission checks are enabled (default), you **must** declare the endpoints and permissions in your module's `ModuleDescriptor.json`.

#### 1. Declare the Provides Interface

Add a `provides` interface entry for your tenant settings:

```json
{
  "id": "authorities-config",
  "version": "1.0",
  "handlers": [
    {
      "methods": ["GET"],
      "pathPattern": "/authorities/config/groups",
      "permissionsRequired": [
        "authorities.config.groups.collection.get"
      ]
    },
    {
      "methods": ["GET"],
      "pathPattern": "/authorities/config/groups/{groupId}/settings",
      "permissionsRequired": [
        "authorities.config.groups.settings.collection.get"
      ],
      "permissionsDesired": [
        "authorities.config.groups.settings.*"
      ]
    },
    {
      "methods": ["PATCH"],
      "pathPattern": "/authorities/config/groups/{groupId}/settings/{key}",
      "permissionsRequired": [
        "authorities.config.groups.settings.item.patch"
      ],
      "permissionsDesired": [
        "authorities.config.groups.settings.*"
      ]
    }
  ]
}
```

**Note:** Replace `authorities` with your domain name and adjust the `pathPattern` accordingly.

#### 2. Define Permissions

Declare all permissions in the `permissionSets` array:

```json
{
  "permissionSets": [
    {
      "permissionName": "authorities.config.groups.collection.get",
      "displayName": "Authorities Configuration - get settings groups",
      "description": "Get settings groups"
    },
    {
      "permissionName": "authorities.config.groups.settings.collection.get",
      "displayName": "Authorities Configuration - get settings for a group",
      "description": "Get settings for a group"
    },
    {
      "permissionName": "authorities.config.groups.settings.authorities.collection.get",
      "displayName": "Authorities Configuration - get settings for authorities group",
      "description": "Get settings for the authorities group"
    },
    {
      "permissionName": "authorities.config.groups.settings.item.patch",
      "displayName": "Authorities Configuration - update setting for a group",
      "description": "Update setting for a group"
    },
    {
      "permissionName": "authorities.config.groups.settings.authorities.mapping.extended.item.patch",
      "displayName": "Authorities Configuration - update mapping.extended setting",
      "description": "Update the mapping.extended setting for authorities"
    }
  ]
}
```

**Permission Naming Convention:**
- Generic collection access: `{domain}.config.groups.settings.collection.get`
- Specific group access: `{domain}.config.groups.settings.{groupId}.collection.get`
- Generic item update: `{domain}.config.groups.settings.item.patch`
- Specific setting update: `{domain}.config.groups.settings.{groupId}.{key}.item.patch`

**Tip:** Define both generic and specific permissions. Generic permissions allow access to all groups/settings, while specific permissions provide fine-grained control.

### Disabling Permission Checks

If there is no need for a group / setting-specific permission check, you can disable permission checks. It will disable check only for desired permissions. In this case there is no need to define these permissions in descriptor.

```yaml
folio:
  tenant:
    settings:
      permission-check-enabled: false
```

## Validation and Error Handling

The module performs validation on setting updates:

### Type Validation
- BOOLEAN values must be `true` or `false`
- INTEGER values must be valid integers
- STRING values can be any string

### Error Responses

**403 Forbidden** – Missing desired permission:
```json
{
  "errors": [
    {
      "message": "User does not have permission: authorities.config.groups.settings.authorities.mapping.extended.item.patch"
    }
  ]
}
```

**404 Not Found** – Group or setting not found

**422 Unprocessable Entity** – Validation error:
```json
{
  "errors": [
    {
      "message": "Invalid value type for setting"
    }
  ]
}
```

## Troubleshooting

### Settings API not accessible

**Problem:** API endpoints return 404

**Solution:** Verify:
1. `folio.tenant.settings.domain` is set in `application.yaml`
2. The domain matches your API path (e.g., domain `authorities` → path `/authorities/config/groups`)
3. Component scanning includes `org.folio.tenant.settings`

### Permission denied errors

**Problem:** 403 Forbidden response

**Solution:**
1. Check `X-Okapi-Permissions` header contains the required permission
2. Verify permissions are declared in your `ModuleDescriptor.json` (see [Permissions](#permissions) section)
3. Permission format for PATCH: `{domain}.config.groups.settings.{groupId}.{key}.item.patch`
4. Permission format for GET: `{domain}.config.groups.settings.{groupId}.collection.get`
5. For testing, set `folio.tenant.settings.permission-check-enabled: false`

### Module descriptor not working

**Problem:** Permissions or endpoints not recognized by FOLIO

**Solution:**
1. Ensure the `provides` interface includes all tenant settings endpoints
2. Verify `permissionSets` includes all required permissions (both generic and specific)
3. Check the HTTP method in handlers matches the API (PATCH, not PUT)
4. Ensure pathPattern matches your domain (e.g., `/authorities/config/groups/{groupId}/settings/{key}`)
5. Redeploy the module after updating the descriptor

### Database tables not created

**Problem:** `setting` or `setting_group` tables missing

**Solution:**
1. Verify `<include file="classpath:db/tenant-settings/changelog.xml"/>` is in your master changelog
2. Check Liquibase runs successfully during application startup
3. Ensure PostgreSQL database is accessible

### Settings not appearing in API

**Problem:** GET request returns empty list

**Solution:**
1. Check your custom Liquibase changeset populates the tables
2. Verify the changeset runs after the tenant-settings schema creation
3. Query the database directly: `SELECT * FROM setting_group; SELECT * FROM setting;`

