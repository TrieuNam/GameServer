# Notification Service Memory

Service-specific operational memory cho `notification-service` trong GameServer.

## Identity
- Service name: `notification-service`
- Path: `GameServer/notification-service`
- Main port: 9025
- Database: notification_service_db (MySQL)
- Build: Maven (`mvn clean install`)

## Core Scope
- Send notifications to users (email, push, in-game)
- Store notification history
- Notification preferences management
- Integration với event-bus để nhận events từ services khác

## Key Files & Anchors
- Service: `notification-service/src/main/java/com/SouthMillion/notification_service/service/NotificationService.java`
- Listener: `notification-service/src/main/java/com/SouthMillion/notification_service/listener/EventListener.java`
- Template: `notification-service/src/main/resources/templates/`

## Important APIs
```
GET    /api/notification/{userId}   - Get user notifications
POST   /api/notification/preferences - Update notification preferences
DELETE /api/notification/{notifId}  - Delete notification
POST   /api/notification/send       - Send notification (admin)
```

## Database Schema
```sql
CREATE TABLE notifications (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  title VARCHAR(255),
  message TEXT NOT NULL,
  type VARCHAR(50),
  is_read BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id),
  INDEX idx_read (is_read)
);

CREATE TABLE notification_preferences (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) UNIQUE NOT NULL,
  email_enabled BOOLEAN DEFAULT TRUE,
  push_enabled BOOLEAN DEFAULT TRUE,
  in_game_enabled BOOLEAN DEFAULT TRUE
);
```

## Common Bugs & Patterns
- **Bug 1**: User receives notifications after opting out
  - Fix: Check preferences before sending
- **Bug 2**: Email queue overflowed, notifications lost
  - Fix: Implement batch sending, retry logic
- **Bug 3**: Event listener crash stops processing all events
  - Fix: Add error handling, don't re-throw exceptions

## Event Integration
Listens to events từ event-bus:
- `user.created` → Send welcome email
- `task.completed` → Send congratulation notification
- `guild.created` → Send guild announcement
- `payment.received` → Send payment confirmation

## Cross-Service Dependencies
- **event-bus**: Listen for events
- **user-service**: Get user email, preferences

## Config & Environment
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/notification_service_db
    username: root
    password: root
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-password
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true

server:
  port: 9025

rabbitmq:
  host: localhost
  port: 5672
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\notification-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] User preferences respected?
- [ ] Email credentials not in code?
- [ ] Event listener doesn't crash on error?
- [ ] Notification persistence before sending?
- [ ] Rate limiting on email sending?

## Update Log
- 2026-03-21 | Scope: notification-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/NOTIFICATION_SERVICE_MEMORY.md`

