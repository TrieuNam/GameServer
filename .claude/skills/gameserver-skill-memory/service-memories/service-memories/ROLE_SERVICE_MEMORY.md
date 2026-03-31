# Role Service Memory

Service-specific operational memory cho `role-service` trong GameServer.

## Identity
- Service name: `role-service`
- Path: `GameServer/role-service`
- Main port: 9019
- Database: role_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Manage roles and permissions (RBAC)
- Assign roles to users
- Permission checks for resources
- Role hierarchy (admin > moderator > user)

## Key Files & Anchors
- Controller: `role-service/src/main/java/com/SouthMillion/role_service/controller/RoleController.java`
- Service: `role-service/src/main/java/com/SouthMillion/role_service/service/RoleService.java`
- Entity: `role-service/src/main/java/com/SouthMillion/role_service/entity/Role.java`

## Important APIs
```
POST   /api/role/create             - Create new role
GET    /api/role/{id}               - Get role details
PUT    /api/role/{id}               - Update role permissions
GET    /api/role/check-permission   - Check if user has permission
POST   /api/user/{userId}/role      - Assign role to user
DELETE /api/user/{userId}/role/{roleId} - Remove role from user
```

## Database Schema
```sql
CREATE TABLE roles (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  description TEXT,
  level INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,
  resource VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL
);

CREATE TABLE role_permissions (
  role_id VARCHAR(36),
  permission_id VARCHAR(36),
  PRIMARY KEY (role_id, permission_id),
  FOREIGN KEY (role_id) REFERENCES roles(id),
  FOREIGN KEY (permission_id) REFERENCES permissions(id)
);
```

## Common Bugs & Patterns
- **Bug 1**: User permission checked by name instead of validated
  - Fix: Always validate permission exists before check
- **Bug 2**: Permission changes don't take effect immediately
  - Fix: Invalidate cache after permission update
- **Bug 3**: Role cannot be deleted if used
  - Fix: Check for users with this role before deletion

## Cross-Service Dependencies
- **user-service**: Validate user has role
- Cached in user context

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/role_service_db
    username: root
    password: root
  cache:
    type: redis
    redis:
      time-to-live: 3600000

server:
  port: 9019
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\role-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] Permission check always validated?
- [ ] Cache invalidated on role change?
- [ ] Role hierarchy enforced?
- [ ] Super admin cannot be deleted?
- [ ] Permission names unique and immutable?

## Update Log
- 2026-03-21 | Scope: role-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/ROLE_SERVICE_MEMORY.md`

