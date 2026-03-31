# Gateway Service Memory

Service-specific operational memory cho `gateway-service` trong GameServer.

## Identity
- Service name: `gateway-service`
- Path: `GameServer/gateway-service`
- Main port: 9001
- Database: N/A (no database, only routing)
- Build: Maven (`mvn clean install`)

## Core Scope
- API Gateway - route requests to backend services
- Authentication validation (JWT token check)
- Request/response transformation
- Rate limiting
- Circuit breaker protection

## Key Files & Anchors
- Config: `gateway-service/src/main/resources/application.yml`
- Routes: `gateway-service/src/main/java/com/SouthMillion/gateway_service/config/GatewayConfig.java`
- Filters: `gateway-service/src/main/java/com/SouthMillion/gateway_service/filter/`
- Test: `gateway-service/src/test/java/com/SouthMillion/gateway_service/`

## Important APIs
Gateway không có business API, chỉ route:
```
/task/**          → task-service:9015
/user/**          → user-service:9016
/guild/**         → guild-service:9017
/chat/**          → chat-service:9018
/notification/**  → notification-service:9025
```

## Route Configuration
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: task-service
          uri: http://localhost:9015
          predicates:
            - Path=/task/**
          filters:
            - RewritePath=/task(?<segment>/?.*), /api/task$\{segment}
        
        - id: user-service
          uri: http://localhost:9016
          predicates:
            - Path=/user/**
          filters:
            - RewritePath=/user(?<segment>/?.*), /api/user$\{segment}
```

## Common Bugs & Patterns
- **Bug 1**: JWT validation missing - all requests reach backend
  - Fix: Add AuthenticationFilter to validate token
- **Bug 2**: Route not matching, 404 errors
  - Fix: Check Path predicates and RewritePath filters
- **Bug 3**: Timeout when backend service slow
  - Fix: Increase timeout, add circuit breaker

## Cross-Service Dependencies
- All backend services (task, user, guild, chat, etc)
- No DB, stateless service

## Config & Environment
```yaml
spring:
  application:
    name: gateway-service
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials
        - name: CircuitBreaker
          args:
            name: myCircuitBreaker
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenish-rate: 100
              burst-capacity: 200

server:
  port: 9001
```

## Command Snippets
```powershell
cd D:\project\serverGame\GameServer\gateway-service
mvn clean install
mvn test
```

## Risk Checklist
- [ ] All backend service URLs configured?
- [ ] JWT validation enabled?
- [ ] Rate limiting configured?
- [ ] Circuit breaker timeout OK?
- [ ] CORS headers set?
- [ ] Request/response logging configured?

## Update Log
- 2026-03-21 | Scope: gateway-service | Change: create memory | Why: for all-service memory | Ref: `service-memories/GATEWAY_SERVICE_MEMORY.md`

