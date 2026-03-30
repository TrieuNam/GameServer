# User Service Memory

Service-specific operational memory cho `user-service` trong GameServer.

## Identity
- Service name: `user-service`
- Path: `GameServer/user-service`
- Main port: 9016
- Database: user_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- User account management (register, login, profile)
- User roles va permissions
- User authentication token management
- Integration voi role-service for permission checks

## Key Files & Anchors
- Controller: `user-service/src/main/java/com/SouthMillion/user_service/controller/UserController.java`
- Service: `user-service/src/main/java/com/SouthMillion/user_service/service/UserDomainService.java`
- Entity: `user-service/src/main/java/com/SouthMillion/user_service/entity/User.java`
- DTO: `user-service/src/main/java/com/SouthMillion/user_service/dto/UserDTO.java`
- Repository: `user-service/src/main/java/com/SouthMillion/user_service/repository/UserRepository.java`
- Test: `user-service/src/test/java/com/SouthMillion/user_service/service/UserDomainServiceTest.java`
- Config: `user-service/src/main/resources/application.yml`

## Important APIs
```
POST   /api/user/register          - Register new user
POST   /api/user/login             - User login, return token
GET    /api/user/{id}              - Get user profile
PUT    /api/user/{id}              - Update user profile
POST   /api/user/{id}/change-password - Change password
GET    /api/user/{id}/roles        - Get user roles
DELETE /api/user/{id}              - Delete user account
```

## Key Request/Response
```json
UserDTO {
  "id": "string",
  "username": "string",
  "email": "string",
  "status": "ACTIVE|INACTIVE|BANNED",
  "roles": ["string"],
  "createdAt": "ISO-8601 datetime",
  "updatedAt": "ISO-8601 datetime"
}

LoginRequest {
  "username": "string",
  "password": "string"
}

LoginResponse {
  "token": "JWT_TOKEN",
  "userId": "string",
  "username": "string"
}
```

## Database Schema
```sql
CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY,
  username VARCHAR(100) UNIQUE NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) DEFAULT 'ACTIVE',
  last_login DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted_at DATETIME,
  INDEX idx_username (username),
  INDEX idx_email (email),
  INDEX idx_status (status)
);

CREATE TABLE user_roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  role_id VARCHAR(36) NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_user_role (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Common Bugs & Patterns
- **Bug 1**: Password không được hash properly
  - Fix: Use BCryptPasswordEncoder, never store plain text
- **Bug 2**: Token không được validate trước khi trả response
  - Fix: Validate token format và expiration
- **Bug 3**: User có thể register với username/email duplicate
  - Fix: Add unique constraint, check before insert

## Test Patterns
```java
@Test
public void testRegisterUser() {
  UserDTO dto = new UserDTO("newuser", "user@test.com", "Password123");
  UserDTO result = userService.registerUser(dto);
  assertNotNull(result.getId());
  assertEquals("newuser", result.getUsername());
}

@Test
public void testLoginUser() {
  userService.registerUser(new UserDTO("testuser", "test@test.com", "Pass123"));
  LoginResponse response = userService.loginUser("testuser", "Pass123");
  assertNotNull(response.getToken());
}

@Test(expected = InvalidPasswordException.class)
public void testLoginWithWrongPassword() {
  userService.registerUser(new UserDTO("testuser", "test@test.com", "Pass123"));
  userService.loginUser("testuser", "WrongPass");
}
```

## Cross-Service Dependencies
- **role-service**: REST call to validate roles
- **session-service**: Store login sessions
- **event-bus**: Publish user events (register, login, logout)

## Config & Environment
```yaml
spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://localhost:3306/user_service_db
    username: root
    password: root

server:
  port: 9016

security:
  jwt:
    secret: your-secret-key
    expiration: 86400000  # 24 hours
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\user-service
mvn clean install
mvn test
mvn -Dtest=UserDomainServiceTest test
```

## Risk Checklist
- [ ] Password hashing sử dụng BCrypt?
- [ ] JWT token validation OK?
- [ ] SQL injection prevention (use prepared statements)?
- [ ] Rate limiting trên login endpoint?
- [ ] User có thể delete account của người khác?
- [ ] Email verification implemented?

## Update Log
- 2026-03-21 | Scope: user-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/USER_SERVICE_MEMORY.md`

