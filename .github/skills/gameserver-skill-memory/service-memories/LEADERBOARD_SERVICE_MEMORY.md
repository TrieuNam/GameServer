# Service Memory Template

Copy file này và điền theo service của bạn.
Thay `{SERVICE_NAME}` bằng tên service thực tế.

---

# {SERVICE_NAME} Service Memory

Service-specific operational memory cho `{SERVICE_NAME}` trong GameServer.
Dung file nay de tranh quen context khi lam viec voi service nay.

## Identity
- Service name: `{SERVICE_NAME}`
- Path: `GameServer/{SERVICE_NAME}`
- Main port: {PORT} (check SERVICE-PORT-DB-MAPPING.md)
- Database: {DB_NAME} (check application.yml)
- Build: Maven (`mvn clean install`)

## Core Scope
- **Primary responsibility**: {MAIN_FUNCTION}
- **Key entities**: {ENTITY_NAMES}
- **Key workflows**: {WORKFLOW_DESCRIPTIONS}

Example:
- **Primary responsibility**: User profile management, authentication
- **Key entities**: User, UserProfile, UserRole
- **Key workflows**: Register → Login → Update Profile → Logout

## Key Files & Anchors
- Controller: `{SERVICE_NAME}/src/main/java/com/SouthMillion/{SERVICE_NAME}/controller/{CONTROLLER_NAME}.java`
- Service: `{SERVICE_NAME}/src/main/java/com/SouthMillion/{SERVICE_NAME}/service/{SERVICE_CLASS_NAME}.java`
- Entity: `{SERVICE_NAME}/src/main/java/com/SouthMillion/{SERVICE_NAME}/entity/{ENTITY_NAME}.java`
- DTO: `{SERVICE_NAME}/src/main/java/com/SouthMillion/{SERVICE_NAME}/dto/{DTO_NAME}.java`
- Repository: `{SERVICE_NAME}/src/main/java/com/SouthMillion/{SERVICE_NAME}/repository/{REPOSITORY_NAME}.java`
- Test: `{SERVICE_NAME}/src/test/java/com/SouthMillion/{SERVICE_NAME}/service/{TEST_CLASS_NAME}.java`
- Config: `{SERVICE_NAME}/src/main/resources/application.yml`

## Important APIs

### Endpoints (Common Pattern)
```
POST   /api/{service_name}          - Create
GET    /api/{service_name}/{id}     - Get by ID
GET    /api/{service_name}          - List all
PUT    /api/{service_name}/{id}     - Update
DELETE /api/{service_name}/{id}     - Delete
```

### Actual Endpoints (Fill In)
```
POST   /api/{service_name}/create         - {DESCRIPTION}
GET    /api/{service_name}/{id}           - {DESCRIPTION}
PUT    /api/{service_name}/{id}           - {DESCRIPTION}
DELETE /api/{service_name}/{id}           - {DESCRIPTION}
{OTHER_ENDPOINTS}
```

### Key Request/Response
```json
{MAIN_DTO} {
  "id": "string",
  "name": "string",
  "status": "ENUM_VALUES",
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime"
  // ... more fields
}
```

## Database Schema (Quick Ref)
```sql
CREATE TABLE {TABLE_NAME} (
  id VARCHAR(36) PRIMARY KEY,
  {COLUMN1} {TYPE} {CONSTRAINT},
  {COLUMN2} {TYPE} {CONSTRAINT},
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  INDEX idx_{COLUMN1} ({COLUMN1})
);
```

## Common Bugs & Patterns
- **Bug 1**: {COMMON_ISSUE_1}
  - Fix: {SOLUTION_1}
- **Bug 2**: {COMMON_ISSUE_2}
  - Fix: {SOLUTION_2}
- **Bug 3**: {COMMON_ISSUE_3}
  - Fix: {SOLUTION_3}

## Test Patterns

Key test file: `{SERVICE_NAME}/src/test/java/com/SouthMillion/{SERVICE_NAME}/service/{TEST_CLASS_NAME}.java`

```java
@Test
public void test{SCENARIO}() {
  // Setup
  {ENTITY_NAME} obj = new {ENTITY_NAME}(...);
  
  // Execute
  {RESULT_TYPE} result = {service}.{methodName}(obj);
  
  // Assert
  assertNotNull(result);
  assertEquals({EXPECTED}, result.{property});
}
```

## Cross-Service Dependencies
- **Service A**: {INTERACTION_TYPE} (REST/Event)
- **Service B**: {INTERACTION_TYPE}
- Example: user-service ← role-service (REST), → event-bus (Event)

## Config & Environment

File: `{SERVICE_NAME}/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: {SERVICE_NAME}
  datasource:
    url: jdbc:mysql://localhost:3306/{DB_NAME}
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

server:
  port: {PORT}

logging:
  level:
    com.SouthMillion: DEBUG
```

## Command Snippets
```powershell
# Build {SERVICE_NAME}
cd D:\project\serverGame\GameServer\{SERVICE_NAME}
mvn clean install

# Run tests
mvn test

# Run specific test
mvn -Dtest={TEST_CLASS_NAME} test

# Build without tests
mvn clean install -DskipTests

# Build parent + this service
cd D:\project\serverGame\GameServer
mvn -pl {SERVICE_NAME} clean install
```

## Risk Checklist (Truoc khi submit)
- [ ] {SPECIFIC_RISK_1}?
- [ ] {SPECIFIC_RISK_2}?
- [ ] {SPECIFIC_RISK_3}?
- [ ] Database constraints OK?
- [ ] Cross-service calls validated?
- [ ] Error handling robust?
- [ ] Test coverage increased?

## Common Workflows

### Workflow 1: Create/Update Flow
1. Receive request
2. Validate input
3. {CUSTOM_STEPS}
4. Save to DB
5. Publish event (if needed)
6. Return response

### Workflow 2: {OTHER_WORKFLOW}
1. {STEP_1}
2. {STEP_2}
3. {STEP_3}

## Integration Points
- **Event bus topics**: {TOPIC_1}, {TOPIC_2}
- **Feign clients**: [Service A], [Service B]
- **Redis cache**: {CACHE_KEYS}

## Weekly Review Items
- [ ] Check service logs for error patterns
- [ ] Review pending PRs related to service
- [ ] Test cross-service integration
- [ ] Check database performance

## Update Log
- YYYY-MM-DD | Scope: {SERVICE_NAME} | Change: {CHANGE} | Why: {WHY} | Ref: {REF}
- 2026-03-21 | Scope: {SERVICE_NAME} | Change: create memory template | Why: de tranh quen context | Ref: `service-memories/{SERVICE_NAME}_MEMORY.md`

---

## How to Use This Template

1. Copy this file
2. Replace `{SERVICE_NAME}` with actual service name
3. Replace placeholders with actual values from pom.xml, code, configs
4. Fill in APIs, DB schema, tests, bugs
5. Save as `{SERVICE_NAME}_MEMORY.md`
6. Move to `service-memories/{SERVICE_NAME}_MEMORY.md`
7. Test: `@gameserver Show {SERVICE_NAME}_MEMORY`

---

## Quick Reference - Where to Find Info

| Info | Location |
|------|----------|
| Port | `GameServer/docs/SERVICE-PORT-DB-MAPPING.md` |
| Database | `{service}/src/main/resources/application.yml` |
| Endpoints | `{service}/src/main/java/.../Controller.java` |
| Entities | `{service}/src/main/java/.../entity/` |
| Tests | `{service}/src/test/java/.../service/` |
| Common bugs | Previous PRs, commit messages, logs |

