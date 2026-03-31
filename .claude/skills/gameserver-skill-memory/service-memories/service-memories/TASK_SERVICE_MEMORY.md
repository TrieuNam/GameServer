# Task Service Memory

Service-specific operational memory cho `task-service` trong GameServer.
Dung file nay de tranh quen context khi lam viec voi task-service.

## Identity
- Service name: `task-service`
- Path: `GameServer/task-service`
- Main port: 9015 (check `SERVICE-PORT-DB-MAPPING.md`)
- Database: task_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Task CRUD va lifecycle management (create, update, complete, cancel).
- Task status workflow: PENDING -> RUNNING -> COMPLETED/FAILED/CANCELLED.
- Integration voi user-service (task ownership).
- Integration voi event bus (task events).

## Key Files & Anchors
- Controller: `task-service/src/main/java/com/SouthMillion/task_service/controller/TaskController.java`
- Service: `task-service/src/main/java/com/SouthMillion/task_service/service/TaskDomainService.java`
- Entity: `task-service/src/main/java/com/SouthMillion/task_service/entity/Task.java`
- DTO: `task-service/src/main/java/com/SouthMillion/task_service/dto/TaskDTO.java`
- Test: `task-service/src/test/java/com/SouthMillion/task_service/service/TaskDomainServiceTest.java`
- Config: `task-service/src/main/resources/application.yml`

## Important APIs
### Endpoints
```
POST   /api/task/create         - Create new task
GET    /api/task/{id}           - Get task details
PUT    /api/task/{id}           - Update task
DELETE /api/task/{id}           - Delete task
GET    /api/task/user/{userId}  - List user tasks
POST   /api/task/{id}/complete  - Mark task as complete
POST   /api/task/{id}/cancel    - Cancel task
```

### Key Request/Response
```json
TaskDTO {
  "id": "string",
  "userId": "string",
  "title": "string",
  "description": "string",
  "status": "PENDING|RUNNING|COMPLETED|FAILED|CANCELLED",
  "priority": "LOW|MEDIUM|HIGH",
  "dueDate": "ISO-8601 datetime",
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime"
}
```

## Database Schema (Quick Ref)
```sql
CREATE TABLE tasks (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  status VARCHAR(20) DEFAULT 'PENDING',
  priority VARCHAR(10) DEFAULT 'MEDIUM',
  due_date DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX(user_id),
  INDEX(status)
);
```

## Common Bugs & Patterns
- **Bug 1**: Task status khong update khi event bus phat hien loi
  - Fix: Add try-catch around event publishing, log error, fallback to sync update.
- **Bug 2**: Concurrent updates tao race condition
  - Fix: Add version field, use optimistic locking.
- **Bug 3**: User co the update task cua user khac
  - Fix: Verify user ownership truoc khi update.

## Test Patterns
Xem file: `task-service/src/test/java/com/SouthMillion/task_service/service/TaskDomainServiceTest.java`

```java
@Test
public void testCreateTask() {
  TaskDTO dto = new TaskDTO("User1", "My Task", "Desc", "PENDING");
  TaskDTO result = taskService.createTask(dto);
  assertNotNull(result.getId());
  assertEquals("PENDING", result.getStatus());
}

@Test
public void testUpdateTaskStatus() {
  TaskDTO task = taskService.createTask(...);
  taskService.updateStatus(task.getId(), "COMPLETED");
  TaskDTO updated = taskService.getTask(task.getId());
  assertEquals("COMPLETED", updated.getStatus());
}

@Test
public void testDeleteTask() {
  TaskDTO task = taskService.createTask(...);
  taskService.deleteTask(task.getId());
  assertNull(taskService.getTask(task.getId()));
}
```

## Cross-Service Dependencies
- **user-service**: Task ownership validation.
- **event-bus**: Task creation/update events (async).
- **notification-service**: Send task reminders (via event).

## Config & Environment
File: `task-service/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/task_service_db
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
server:
  port: 9015
```

## Command Snippets
```powershell
# Build task-service
cd D:\project\serverGame\GameServer\task-service
mvn clean install

# Run test
mvn test

# Run specific test
mvn -Dtest=TaskDomainServiceTest test

# Build without test
mvn clean install -DskipTests
```

## Risk Checklist (Truoc khi submit task-service changes)
- [ ] Co cap nhat owner/user validation khong?
- [ ] Status transition logic co dung khong (chi allow valid transitions)?
- [ ] Database constraints dung khong (unique key, foreign key)?
- [ ] Event bus publishing co robust khong (retry/fallback)?
- [ ] Test coverage da tang khong?

## Weekly Review Items
- [ ] Check task service logs cho error patterns.
- [ ] Review pending PRs related to task-service.
- [ ] Test cross-service integration voi user/event bus.

## Update Log
- 2026-03-21 | Scope: task-service | Change: create memory template | Why: de tranh quen context | Ref: `service-memories/TASK_SERVICE_MEMORY.md`

