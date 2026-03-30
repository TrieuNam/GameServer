# [SERVICE_NAME] Service Documentation

> **Template Version**: 1.0  
> **Copy this template to**: `docs/services/[service-name]-service.md`

---

## Overview

**Service Name**: `[service-name]-service`  
**Port**: `[PORT_NUMBER]`  
**Database**: `game_[service]`  
**Status**: ⏳ **Not Started** / 🚧 **In Progress** / ✅ **Complete**  

[Brief description of what this service does - 1-2 sentences]

---

## Architecture

```
Browser (TypeScript)
    ↓ WebSocket (Binary Proto)
webSocket-server
    ↓ [ServiceName]Handler
    ↓ [ServiceName]Feign (HTTP REST)
[service-name]-service (Spring Boot)
    ↓ [ServiceName]Service (Business Logic)
    ↓ [ServiceName]Repository (JPA)
MySQL (game_[service].t_[entity])
```

---

## Database Schema

### Table: `t_[entity_name]`

```sql
CREATE TABLE t_[entity] (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    [field1] BIGINT NOT NULL COMMENT '[description]',
    [field2] INT NOT NULL DEFAULT 0 COMMENT '[description]',
    create_time DATETIME NOT NULL,
    update_time DATETIME,
    INDEX idx_[field] ([field])
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Indexes**:
- `idx_[field]`: [Description of why this index]

**Relationships**:
- Foreign key to [other_table]

---

## REST API Endpoints

### 1. [Operation Name]
```http
GET|POST /api/[service]/[path]
```

**Description**: [What this endpoint does]

**Request**:
```json
{
    "field1": "value",
    "field2": 123
}
```

**Response**:
```json
{
    "success": true,
    "data": { },
    "message": "Success"
}
```

**Business Logic**:
- Step 1: [Description]
- Step 2: [Description]
- Step 3: [Description]

---

### 2. Health Check
```http
GET /api/[service]/health
```

**Response**:
```json
{
    "status": "UP",
    "service": "[service-name]-service",
    "timestamp": 1738368000000
}
```

---

## Java Service Structure

### Entity: `[EntityName].java`
```java
@Data
@Entity
@Table(name = "t_[entity]")
public class [EntityName] {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "[field_name]", nullable = false)
    private [Type] [fieldName];
    
    // ... other fields
    
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
```

---

### Repository: `[EntityName]Repository.java`
```java
@Repository
public interface [EntityName]Repository extends JpaRepository<[EntityName], Long> {
    
    List<[EntityName]> findBy[Field]([Type] [field]);
    
    Optional<[EntityName]> findBy[Field1]And[Field2]([Type1] [field1], [Type2] [field2]);
    
    @Query("SELECT ... FROM [EntityName] e WHERE ...")
    List<[EntityName]> customQuery([Type] param);
}
```

---

### Service: `[ServiceName]Service.java`
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class [ServiceName]Service {
    
    private final [EntityName]Repository repository;
    
    public List<[DTO]> getAll([Type] param) {
        // Implementation
    }
    
    @Transactional
    public Map<String, Object> create([Type] param) {
        // Implementation
    }
    
    @Transactional
    public Map<String, Object> update([Type] id, [Type] param) {
        // Implementation
    }
    
    @Transactional
    public Map<String, Object> delete([Type] id) {
        // Implementation
    }
}
```

---

### Controller: `[ServiceName]Controller.java`
```java
@Slf4j
@RestController
@RequestMapping("/api/[service]")
@RequiredArgsConstructor
public class [ServiceName]Controller {
    
    private final [ServiceName]Service service;
    
    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable [Type] id) {
        // Implementation
    }
    
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> request) {
        // Implementation
    }
    
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "[service-name]-service",
            "timestamp", System.currentTimeMillis()
        );
    }
}
```

---

## Configuration

### application.yml
```yaml
server:
  port: [PORT]

spring:
  application:
    name: [service-name]-service
  datasource:
    url: jdbc:mysql://localhost:3306/game_[service]
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

---

### pom.xml Dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

---

## Testing

### Start Service
```bash
cd D:\project\serverGame\GameServer\[service-name]-service
mvn clean install
mvn spring-boot:run
```

### Test Endpoints
```bash
# Health check
curl http://localhost:[PORT]/api/[service]/health

# Test operation 1
curl http://localhost:[PORT]/api/[service]/[path]

# Test operation 2 (POST)
curl -X POST http://localhost:[PORT]/api/[service]/[path] \
  -H "Content-Type: application/json" \
  -d '{"field":"value"}'
```

---

## Status Checklist

- [ ] Entity created
- [ ] Repository created
- [ ] Service logic implemented
- [ ] Controller endpoints created
- [ ] Configuration ready
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Deployment tested
- [ ] WebSocket handler integrated
- [ ] Frontend client created

---

## Next Steps

1. [Step 1]
2. [Step 2]
3. [Step 3]

---

## Related Documentation

- [[ServiceName]Handler Documentation](../handlers/[ServiceName]Handler.md)
- [[ServiceName]Service.ts Client](../clients/[ServiceName]Service.md)
- [Service Index](../SERVICE_INDEX.md)

---

*Last Updated: [DATE]*
