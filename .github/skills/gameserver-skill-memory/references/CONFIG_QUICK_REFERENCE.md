# Config Quick Reference

Cac mau configuration va environment setup cho GameServer services.

## Application.yml Template (All Services)

Mau co ban cho tung service:

```yaml
spring:
  application:
    name: task-service  # Doi theo service
  datasource:
    url: jdbc:mysql://localhost:3306/task_service_db  # Doi theo service
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
  jackson:
    serialization:
      write-dates-as-timestamps: false
    default-property-inclusion: non_null

server:
  port: 9015  # Doi theo service (see SERVICE-PORT-DB-MAPPING.md)
  servlet:
    context-path: /

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.SouthMillion: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/${spring.application.name}.log
    max-size: 10MB
    max-history: 30
```

## Environment Variables

```powershell
# PowerShell (set before running service)
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/task_service_db"
$env:SPRING_DATASOURCE_USERNAME = "root"
$env:SPRING_DATASOURCE_PASSWORD = "root"
$env:SERVER_PORT = "9015"
$env:SPRING_PROFILES_ACTIVE = "dev"

# Or in .env file (if Spring Boot uses it)
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/task_service_db
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
SERVER_PORT=9015
```

## Service Port Mapping

| Service | Port | Database |
|---------|------|----------|
| task-service | 9015 | task_service_db |
| user-service | 9016 | user_service_db |
| gateway-service | 9001 | - |
| event-bus | 9030 | - |
| session-service | 9020 | session_db |
| notification-service | 9025 | notification_db |

(See `GameServer/docs/SERVICE-PORT-DB-MAPPING.md` for full list)

## Feign Client Configuration

Khi service A call service B:

```java
@FeignClient(
  name = "user-service",
  url = "http://localhost:9016",  // Or use service discovery
  fallback = UserServiceFallback.class
)
public interface UserServiceClient {
  @GetMapping("/api/user/{id}")
  UserDTO getUser(@PathVariable String id);
}
```

Config trong `application.yml`:
```yaml
feign:
  client:
    config:
      user-service:
        connectTimeout: 10000
        readTimeout: 10000
        loggerLevel: full
        errorDecoder: com.SouthMillion.common.error.FeignErrorDecoder
```

## Event Bus Configuration

Para sa async task events:

```yaml
spring:
  rabbitmq:  # Neu gumagamit ng RabbitMQ
    host: localhost
    port: 5672
    username: guest
    password: guest
  kafka:  # Kung Kafka ang ginagamit
    bootstrap-servers: localhost:9092
    consumer:
      group-id: task-service-group
      auto-offset-reset: earliest
    producer:
      acks: all
```

Queue/Topic setup:

```java
@Configuration
public class EventConfig {
  // RabbitMQ
  @Bean
  public Queue taskQueue() {
    return new Queue("task.queue", true);  // durable queue
  }
  
  @Bean
  public Topic taskTopic() {
    return new Topic("task.topic");
  }
  
  // Or Kafka
  public static final String TASK_TOPIC = "task-events";
}
```

## Database Initialization

Script para sa fresh DB setup:

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS task_service_db;
USE task_service_db;

-- Create tasks table
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
  deleted_at DATETIME,
  INDEX idx_user (user_id),
  INDEX idx_status (status),
  INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create audit table (optional)
CREATE TABLE tasks_audit (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id VARCHAR(36),
  action VARCHAR(20),
  old_value JSON,
  new_value JSON,
  changed_by VARCHAR(36),
  changed_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Logging Configuration

```yaml
logging:
  level:
    com.SouthMillion.task_service: DEBUG
    com.SouthMillion.common: INFO
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  file:
    name: logs/task-service.log
    max-size: 10MB
    max-history: 30
    total-size-cap: 1GB
```

## Docker Compose (Optional)

Para sa local development (kung may docker):

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: task_service_db
    volumes:
      - mysql_data:/var/lib/mysql

  task-service:
    build: .
    ports:
      - "9015:9015"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/task_service_db
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root

volumes:
  mysql_data:
```

## Maven Properties (pom.xml)

```xml
<properties>
  <java.version>11</java.version>
  <maven.compiler.source>11</maven.compiler.source>
  <maven.compiler.target>11</maven.compiler.target>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <spring-boot.version>2.7.14</spring-boot.version>
  <mysql-connector.version>8.0.33</mysql-connector.version>
</properties>
```

## Security Configuration (Optional)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/auth
          jwk-set-uri: http://localhost:8080/auth/.well-known/jwks.json
    cors:
      allowed-origins: http://localhost:3000,http://localhost:8080
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      allowed-headers: "*"
      allow-credentials: true
```

## Actuator & Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

Access metrics:
```
http://localhost:9015/actuator/health
http://localhost:9015/actuator/metrics
http://localhost:9015/actuator/prometheus
```

## Troubleshooting Config

```yaml
# Increase timeout for slow operations
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true

# Connection pooling
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      max-lifetime: 1800000
      connection-timeout: 30000
```

## Checklist When Setting Up New Service
- [ ] Create database schema
- [ ] Set correct port in `application.yml`
- [ ] Configure datasource credentials
- [ ] Set `spring.application.name`
- [ ] Configure logging
- [ ] Set up health check endpoint
- [ ] Configure Feign clients (if calling other services)
- [ ] Set up event bus integration (if needed)
- [ ] Configure actuator endpoints
- [ ] Test health endpoint responds

