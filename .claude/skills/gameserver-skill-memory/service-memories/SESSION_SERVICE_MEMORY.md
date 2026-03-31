# Session Service Memory

Service-specific operational memory cho `session-service` trong GameServer.

## Identity
- Service name: `session-service`
- Path: `GameServer/session-service`
- Main port: 9020
- Database: session_db (Redis/MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- User session management
- Session timeout handling
- Concurrent session control
- Session state tracking (login time, last activity)

## Key Files & Anchors
- Service: `session-service/src/main/java/com/SouthMillion/session_service/service/SessionService.java`
- Repository: `session-service/src/main/java/com/SouthMillion/session_service/repository/SessionRepository.java`

## Important APIs
```
POST   /api/session/create          - Create session after login
GET    /api/session/{sessionId}     - Get session info
DELETE /api/session/{sessionId}     - Logout, delete session
POST   /api/session/validate        - Validate session valid
GET    /api/session/user/{userId}   - Get user active sessions
```

## Session Schema
```sql
CREATE TABLE sessions (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  token VARCHAR(255) UNIQUE NOT NULL,
  ip_address VARCHAR(45),
  user_agent TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  last_activity DATETIME DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME NOT NULL,
  INDEX idx_user (user_id),
  INDEX idx_token (token)
);
```

## Common Bugs & Patterns
- **Bug 1**: Session not invalidated after timeout
  - Fix: Check expires_at before responding
- **Bug 2**: User can access with expired token
  - Fix: Validate token expiration on every request
- **Bug 3**: Multiple login sessions not allowed
  - Fix: Check/delete old sessions on new login

## Cross-Service Dependencies
- **user-service**: Validate user before creating session
- **gateway-service**: Validate session on each request

## Config & Environment
```yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: spring:session
      timeout: 1800000  # 30 minutes
  redis:
    host: localhost
    port: 6379

server:
  port: 9020
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\session-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] Session timeout enforced?
- [ ] Token validation on every request?
- [ ] Session data encrypted?
- [ ] Concurrent login limit set?
- [ ] Old sessions cleaned up?

## Update Log
- 2026-03-21 | Scope: session-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/SESSION_SERVICE_MEMORY.md`

