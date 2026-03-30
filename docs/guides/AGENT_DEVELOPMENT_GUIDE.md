# 🤖 AGENT DEVELOPMENT GUIDE - Game Server Migration

> **Purpose**: Complete reference for AI agents to implement Java microservices  
> **Project**: C++ Game Server → Java Spring Boot Microservices Migration  
> **Last Updated**: 2026-01-19  
> **Status**: 43/43 services migrated (100% complete) ✅

---

## 📋 TABLE OF CONTENTS

1. [Quick Start](#quick-start)
2. [Project Architecture](#project-architecture)
3. [Development Workflow](#development-workflow)
4. [Service Implementation Guide](#service-implementation-guide)
5. [Protocol Message Mapping](#protocol-message-mapping)
6. [Database Schema Guide](#database-schema-guide)
7. [Configuration Management](#configuration-management)
8. [Testing Requirements](#testing-requirements)
9. [Common Patterns](#common-patterns)
10. [Service Catalog](#service-catalog)

---

# 🚀 QUICK START

## What You Need to Know

### Project Context
- **Legacy System**: C++ monolith (~200K LOC) with 6 server components
- **Target System**: 43 Java Spring Boot microservices
- **Frontend**: Cocos Creator 3.5.1 + TypeScript (no changes needed)
- **Protocol**: Protocol Buffers 3.x (client-server communication)
- **Migration Goal**: Decompose C++ monolith into independently deployable services

### Technology Stack
```yaml
Runtime: Java 21 (Virtual Threads enabled)
Framework: Spring Boot 3.5.3 + Spring Cloud 2025.0.0
Service Discovery: Eureka (8761)
API Gateway: Spring Cloud Gateway (8080)
WebSocket: Custom WebSocket server (8094)
Config: Spring Cloud Config (8888)
Cache: Redis 7.x
Database: MySQL 8.x (per-service schemas)
Messaging: Kafka 3.x
Monitoring: Prometheus + Grafana
```

### Current Status
- ✅ **P0 Infrastructure**: 8/8 services (100%)
- ✅ **P1 Economy**: 9/9 services (100%)
- ✅ **P2 Combat**: 9/9 services (100%)
- ✅ **P3 Social**: 8/8 services (100%)
- ✅ **P4 Supporting**: 9/9 services (100%)

---

# 🏗️ PROJECT ARCHITECTURE

## System Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                     CLIENT (Cocos Creator)                       │
│  TypeScript Controllers → Protocol Buffers → WebSocket/HTTP     │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                        GATEWAY LAYER                             │
│  ┌────────────────┐  ┌────────────────┐  ┌─────────────────┐   │
│  │ API Gateway    │  │ WebSocket      │  │ Eureka Server   │   │
│  │ (8080)         │  │ Server (8094)  │  │ (8761)          │   │
│  └────────────────┘  └────────────────┘  └─────────────────┘   │
└───────────────────────────┬──────────────────────────────────────┘
                            │ Feign (REST) / gRPC (internal)
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                      BUSINESS SERVICES                           │
│                                                                  │
│  P1: Economy (9 services)    P2: Combat (9 services)            │
│  ┌───────┐ ┌───────┐         ┌───────┐ ┌───────┐               │
│  │wallet │ │ bag   │         │battle │ │skill  │               │
│  │(8210) │ │(8230) │         │(8320) │ │(8300) │               │
│  └───────┘ └───────┘         └───────┘ └───────┘               │
│                                                                  │
│  P3: Social (8 services)     P4: Supporting (9 services)        │
│  ┌───────┐ ┌───────┐         ┌───────┐ ┌───────┐               │
│  │ role  │ │guild  │         │  pet  │ │mount  │               │
│  │(8410) │ │(8440) │         │(TBD)  │ │(TBD)  │               │
│  └───────┘ └───────┘         └───────┘ └───────┘               │
└───────────────────────────┬──────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                     DATA & MESSAGING LAYER                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐                │
│  │ MySQL      │  │ Redis      │  │ Kafka      │                │
│  │ (per-svc)  │  │ (6379)     │  │ (9092)     │                │
│  └────────────┘  └────────────┘  └────────────┘                │
└──────────────────────────────────────────────────────────────────┘
```

## Port Allocation Strategy

| Service Type | Port Range | Example |
|-------------|-----------|---------|
| Infrastructure | 8000-8199 | gateway(8080), eureka(8761), config(8888) |
| Economy (P1) | 8200-8299 | wallet(8210), bag(8230), shop(8260) |
| Combat (P2) | 8300-8399 | battle(8320), skill(8300), monster(8310) |
| Social (P3) | 8400-8499 | role(8410), guild(8440), mail(8460) |
| Supporting (P4) | 8500-8599 | arena(8500), event(8510), pet(TBD) |

---

# ⚙️ DEVELOPMENT WORKFLOW

## Step-by-Step Service Implementation

### 1. Analyze Legacy C++ Code

**Location**: `D:\project\serverGame\开箱h5\server\server\src\gameworld\gameworld\other\`

**What to Look For**:
```cpp
// Find the C++ module for your service
// Example: box-service → src/gameworld/gameworld/other/box/

1. Message Handlers:
   MSG_HANDLER_REHIST(MT_BOX_REQ_CS, OnBoxReq);
   → Identifies message IDs (MT_BOX_REQ_CS = 1610)

2. Business Logic:
   void BoxModule::OnBoxReq(const PB_CSBoxReq& req) {
     // Core logic to migrate
   }

3. Database Tables:
   - Check dataaccess/table/tabledef.h for table definitions
   - Look for TABLE_* enums

4. Config Files:
   - Check config/logicconfig/*.json
   - Check config/gameworld/*.xml
```

### 2. Map Frontend Controller

**Location**: `D:\project\serverGame\client\LineR\assets\script\modules\`

**What to Extract**:
```typescript
// Find the TypeScript controller
// Example: box-service → modules/box/BoxCtrl.ts

1. Protocol Messages:
   public static MT_BOX_REQ_CS = 1610;      // Client sends
   public static MT_BOX_INFO_SC = 1616;     // Server responds

2. Operations/Commands:
   export enum BoxOperation {
     OPEN_BOX = 0,
     WEAR_EQUIP = 1,
     SELL = 2,
     LEVEL_UP = 3
   }

3. Data Structures:
   class BoxData {
     // Client-side data model
   }
```

### 3. Create Java Service Structure

**Base Directory**: `D:\project\serverGame\GameServer\{service-name}\`

**Required Files**:
```
{service-name}/
├── pom.xml                          # Maven dependencies
├── src/main/
│   ├── java/com/game/{service}/
│   │   ├── {Service}Application.java          # Spring Boot entry point
│   │   ├── controller/
│   │   │   └── {Service}Controller.java       # REST API endpoints
│   │   ├── service/
│   │   │   └── {Service}Service.java          # Business logic
│   │   ├── repository/
│   │   │   └── {Entity}Repository.java        # JPA repositories
│   │   ├── entity/
│   │   │   └── {Entity}.java                  # Database entities
│   │   ├── dto/
│   │   │   ├── {Request}Dto.java              # Request DTOs
│   │   │   └── {Response}Dto.java             # Response DTOs
│   │   ├── config/
│   │   │   └── {Service}Config.java           # Configuration classes
│   │   └── client/
│   │       └── {External}ServiceClient.java   # Feign clients
│   └── resources/
│       ├── application.yml                     # Service configuration
│       └── db/migration/
│           └── V1__init_{service}.sql          # Flyway migration
└── Dockerfile                                   # Docker build
```

### 4. Implement Core Components

#### A. Spring Boot Application
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
public class BoxServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BoxServiceApplication.class, args);
    }
}
```

#### B. REST Controller
```java
@RestController
@RequestMapping("/api/box")
@RequiredArgsConstructor
@Slf4j
public class BoxController {
    
    private final BoxService boxService;
    
    @PostMapping("/open")
    public ResponseEntity<BoxOpenResponse> openBox(
        @RequestBody @Valid BoxOpenRequest request
    ) {
        log.info("Box open request: {}", request);
        BoxOpenResponse response = boxService.openBox(request);
        return ResponseEntity.ok(response);
    }
}
```

#### C. Service Layer
```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoxService {
    
    private final BoxRepository boxRepository;
    private final DropServiceClient dropServiceClient;
    private final BagServiceClient bagServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public BoxOpenResponse openBox(BoxOpenRequest request) {
        // 1. Validate request
        // 2. Call drop-service for RNG (gRPC/Feign)
        // 3. Grant items via bag-service
        // 4. Publish event to Kafka
        // 5. Return response
    }
}
```

#### D. Database Entity
```java
@Entity
@Table(name = "box_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoxData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private Integer boxLevel;
    
    private Integer enchantLevel;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### E. Feign Client (for inter-service calls)
```java
@FeignClient(name = "drop-service", path = "/api/drop")
public interface DropServiceClient {
    
    @PostMapping("/roll")
    DropRollResponse rollDropTable(
        @RequestBody DropRollRequest request
    );
}
```

#### F. Configuration
```yaml
# src/main/resources/application.yml
spring:
  application:
    name: box-service
  datasource:
    url: jdbc:mysql://localhost:3310/box_db
    username: root
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Use Flyway for schema
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes

server:
  port: 8290

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 5. Create WebSocket Handler (if needed)

**Location**: `webSocket-server/src/main/java/com/game/websocket/handler/`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BoxHandler {
    
    private final BoxServiceClient boxServiceClient;
    
    @MessageHandler(msgId = 1610)  // MT_BOX_REQ_CS
    public void handleBoxRequest(Session session, PB_CSBoxReq req) {
        log.info("Box request from session {}: {}", session.getId(), req);
        
        try {
            // Convert protobuf to DTO
            BoxOpenRequest apiReq = convertToRequest(req);
            
            // Call REST API
            BoxOpenResponse apiResp = boxServiceClient.openBox(apiReq);
            
            // Convert response to protobuf
            PB_SCBoxInfo scMsg = convertToProtobuf(apiResp);
            
            // Send to client
            session.sendMessage(1616, scMsg);  // MT_BOX_INFO_SC
            
        } catch (Exception e) {
            log.error("Error handling box request", e);
            sendErrorResponse(session, e.getMessage());
        }
    }
}
```

### 6. Add Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class BoxServiceTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DropServiceClient dropServiceClient;
    
    @Test
    void testOpenBox() throws Exception {
        // Arrange
        BoxOpenRequest request = new BoxOpenRequest();
        request.setUserId("test-user");
        request.setMode(BoxMode.OPEN);
        
        // Mock external service
        when(dropServiceClient.rollDropTable(any()))
            .thenReturn(mockDropResponse());
        
        // Act & Assert
        mockMvc.perform(post("/api/box/open")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("SUCCESS"));
    }
}
```

### 7. Create Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8290
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

# 📨 PROTOCOL MESSAGE MAPPING

## Message ID Ranges (Client ↔ Server)

| Range | Purpose | C++ Handler | Java Service | Status |
|-------|---------|-------------|--------------|--------|
| **1000-1099** | Heartbeat, login | `world.cpp` | gateway-service | ✅ Done |
| **1400-1499** | Role attributes | `roleproperty/` | role-service | ✅ Done |
| **1500-1599** | Inventory | `item/itembag.cpp` | bag-service | ✅ Done |
| **1600-1619** | Equipment | `equip/` | equip-service | ✅ Done |
| **1610-1618** | Box/Gacha | `box/` | box-service | ✅ Done |
| **1620-1639** | Shop | `shop/`, `clothshop/` | shop-service | ✅ Done |
| **1670-1689** | Rune | `rune/` | rune-service | ✅ P4 Done |
| **1675-1680** | Artifact | `shenqi/` | artifact-service | ✅ P4 Done |
| **2000-2099** | Dungeon | `main_fb/` | dungeon-service | ✅ Done |
| **2100-2139** | Pet | `pet/` | pet-service | ✅ P4 Done |
| **2140-2149** | Mount | `mount/` | mount-service | ✅ P4 Done |
| **3000-3099** | Battle | `battle/` | battle-service | ✅ Done |
| **3200-3299** | Arena | `arena/` | arena-service | ✅ Done |
| **7000-7199** | Login/Auth | `loginmanager/` | session-service | ✅ Done |
| **9000-9099** | Server ops | `world.cpp` | gateway-service | ✅ Done |
| **9500-9599** | Mail | `rolemail/` | mail-service | ✅ Done |
| **9640-9699** | Guild | `roleguild/` | guild-service | ✅ Done |

## Protocol Naming Convention

```
CS Prefix = Client → Server (Request)
SC Prefix = Server → Client (Response)

Example:
- PB_CSBoxReq          → Client sends box request
- PB_SCBoxInfo         → Server responds with box info
- PB_SCBoxEquipInfo    → Server sends equipment from box
```

## Common Protocol Pattern

### Frontend (TypeScript):
```typescript
export class BoxCtrl {
    public static MT_BOX_REQ_CS = 1610;
    public static MT_BOX_INFO_SC = 1616;
    
    public openBox(mode: number): void {
        let req = new PB_CSBoxReq();
        req.setMode(mode);
        NetworkMgr.sendMsg(BoxCtrl.MT_BOX_REQ_CS, req);
    }
    
    private onBoxInfo(msg: PB_SCBoxInfo): void {
        // Handle response
        console.log("Box opened:", msg);
    }
}
```

### C++ Backend (Legacy):
```cpp
void BoxModule::OnBoxReq(const PB_CSBoxReq& req) {
    int mode = req.mode();
    
    switch(mode) {
        case BOX_MODE_OPEN:
            HandleOpenBox(req);
            break;
        case BOX_MODE_WEAR:
            HandleWearEquip(req);
            break;
    }
    
    PB_SCBoxInfo resp;
    resp.set_result(BOX_RESULT_SUCCESS);
    SendMsg(MT_BOX_INFO_SC, resp);
}
```

### Java Backend (Target):
```java
// WebSocket Handler
@MessageHandler(msgId = 1610)
public void handleBoxReq(Session session, PB_CSBoxReq req) {
    BoxOpenRequest apiReq = new BoxOpenRequest();
    apiReq.setMode(req.getMode());
    
    BoxOpenResponse apiResp = boxServiceClient.openBox(apiReq);
    
    PB_SCBoxInfo.Builder builder = PB_SCBoxInfo.newBuilder();
    builder.setResult(apiResp.isSuccess() ? 0 : 1);
    
    session.sendMessage(1616, builder.build());
}

// REST Controller in box-service
@PostMapping("/api/box/open")
public BoxOpenResponse openBox(@RequestBody BoxOpenRequest req) {
    return boxService.openBox(req);
}
```

---

# 🗄️ DATABASE SCHEMA GUIDE

## Schema Separation Strategy

Each microservice owns its own database schema. **No shared tables**.

### Example: box-service

**Database**: `box_db` (port 3310)

**Flyway Migration**: `src/main/resources/db/migration/V1__init_box.sql`

```sql
CREATE TABLE box_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(26) NOT NULL,
    box_level INT NOT NULL DEFAULT 1,
    enchant_level INT NOT NULL DEFAULT 0,
    auto_sell BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    UNIQUE KEY uk_role_box (role_id)
);

CREATE TABLE box_equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(26) NOT NULL,
    item_id INT NOT NULL,
    equipment_type VARCHAR(20),
    quality INT,
    enchant_level INT DEFAULT 0,
    is_sold BOOLEAN DEFAULT FALSE,
    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id),
    INDEX idx_not_sold (role_id, is_sold)
);
```

## Database Ports Allocation

| Service | Database | Port |
|---------|----------|------|
| user-service | user_db | 3307 |
| role-service | role_db | 3308 |
| report-service | report_db | 3309 |
| box-service | box_db | 3310 |
| bag-service | bag_db | 3311 |
| equip-service | equip_db | 3312 |
| drop-service | drop_db | 3313 |
| shop-service | shop_db | 3314 |
| gift-service | gift_db | 3315 |
| crafting-service | crafting_db | 3316 |

## Common Table Patterns

### User-scoped Data (most services):
```sql
CREATE TABLE {entity_name} (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,        -- User UUID
    role_id VARCHAR(26) NOT NULL,        -- ULID for character
    -- entity-specific fields --
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_role_id (role_id)
);
```

### Configuration/Metadata (read-only services):
```sql
-- Item metadata (item-service)
-- No database, reads from config-service
-- Uses Redis for caching only
```

---

# ⚙️ CONFIGURATION MANAGEMENT

## Config Service Integration

All game configuration files are served by **config-service** (port 8888).

### C++ Legacy:
```cpp
// Reads local files
std::string configPath = "config/logicconfig/shop_cfg.json";
JsonConfig config = LoadConfig(configPath);
```

### Java Target:
```java
@FeignClient(name = "config-service", path = "/api/config")
public interface ConfigServiceClient {
    
    @GetMapping("/file")
    String getConfigFile(
        @RequestParam("path") String path,
        @RequestHeader(value = "If-None-Match", required = false) String etag
    );
}

// Usage in service:
@Service
@Cacheable("config")
public class ShopConfigCache {
    
    @Autowired
    private ConfigServiceClient configClient;
    
    public ShopConfig getShopConfig() {
        String json = configClient.getConfigFile("shop_cfg.json", null);
        return objectMapper.readValue(json, ShopConfig.class);
    }
}
```

## Configuration File Mapping

| C++ Config File | Config Service Path | Consumer Services |
|----------------|---------------------|-------------------|
| `item/equipment.json` | `item/equipment.json` | item-service, equip-service |
| `shop_cfg.json` | `shop_cfg.json` | shop-service |
| `guild.json` | `guild.json` | guild-service |
| `arena.json` | `arena.json` | arena-service |
| `task.json` | `task.json` | task-service |
| `drop/*.xml` | `drop/dropmanager.xml` | drop-service |
| `monster/*.xml` | `monster/battlemonstermanager.xml` | monster-service |
| `skill/*.xml` | `skill/skill_warrior.xml` | skill-service |

## Caching Strategy

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))  // Config files: 30min
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

---

# 🧪 TESTING REQUIREMENTS

## Testing Standards

Every service must meet these quality gates:

- ✅ **Unit Tests**: 80%+ code coverage
- ✅ **Integration Tests**: All API endpoints tested
- ✅ **Contract Tests**: Feign client contracts verified
- ✅ **Load Tests**: 1000+ concurrent requests
- ✅ **Security Tests**: Authentication & authorization

## Unit Test Template

```java
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb"
})
class BoxServiceTest {
    
    @Autowired
    private BoxService boxService;
    
    @MockBean
    private DropServiceClient dropServiceClient;
    
    @MockBean
    private BagServiceClient bagServiceClient;
    
    @Test
    @DisplayName("Should open box and grant items")
    void testOpenBox() {
        // Arrange
        BoxOpenRequest request = BoxOpenRequest.builder()
            .userId("test-user")
            .roleId("01HQRST1234567890ABCDEFGH")
            .mode(BoxMode.OPEN)
            .build();
        
        when(dropServiceClient.rollDropTable(any()))
            .thenReturn(mockDropResponse());
        
        when(bagServiceClient.grantItems(any()))
            .thenReturn(mockBagResponse());
        
        // Act
        BoxOpenResponse response = boxService.openBox(request);
        
        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getItems()).hasSize(3);
        verify(dropServiceClient).rollDropTable(any());
        verify(bagServiceClient).grantItems(any());
    }
}
```

## Integration Test Template

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@Sql(scripts = "/test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BoxControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testOpenBoxEndpoint() {
        BoxOpenRequest request = new BoxOpenRequest();
        request.setUserId("test-user");
        request.setMode(BoxMode.OPEN);
        
        ResponseEntity<BoxOpenResponse> response = restTemplate.postForEntity(
            "/api/box/open",
            request,
            BoxOpenResponse.class
        );
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
```

---

# 🎨 COMMON PATTERNS

## 1. Service-to-Service Communication

### Synchronous (Feign/REST):
```java
@FeignClient(name = "drop-service", path = "/api/drop")
public interface DropServiceClient {
    @PostMapping("/roll")
    DropRollResponse rollDropTable(@RequestBody DropRollRequest request);
}

// Usage:
DropRollResponse drops = dropServiceClient.rollDropTable(request);
```

### Asynchronous (Kafka):
```java
@Service
public class BoxEventPublisher {
    
    @Autowired
    private KafkaTemplate<String, BoxEvent> kafkaTemplate;
    
    public void publishBoxOpened(BoxOpenedEvent event) {
        kafkaTemplate.send("gameh5.box.opened", event);
    }
}
```

## 2. Idempotency Pattern

**Critical for wallet, bag, and item operations:**

```java
@Service
public class WalletService {
    
    @Transactional
    public CreditResponse credit(CreditRequest request) {
        String idempotencyKey = request.getIdempotencyKey();
        
        // Check if already processed
        Optional<Transaction> existing = transactionRepo
            .findByIdempotencyKey(idempotencyKey);
        
        if (existing.isPresent()) {
            return existing.get().toResponse();  // Return cached result
        }
        
        // Process transaction
        Transaction tx = processCredit(request);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepo.save(tx);
        
        return tx.toResponse();
    }
}
```

## 3. Caching Pattern

```java
@Service
public class ItemService {
    
    @Cacheable(value = "items", key = "#itemId")
    public ItemMetadata getItem(int itemId) {
        String json = configServiceClient.getConfigFile("item/equipment.json");
        return parseItem(json, itemId);
    }
    
    @CacheEvict(value = "items", allEntries = true)
    public void refreshCache() {
        // Evict all cached items
    }
}
```

## 4. Error Handling Pattern

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .code("RESOURCE_NOT_FOUND")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message(ex.getMessage())
            .timestamp(Instant.now())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
```

## 5. Audit Logging Pattern

```java
@Aspect
@Component
public class AuditAspect {
    
    @Around("@annotation(com.game.common.annotation.Audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        log.info("Audit: {} called with args: {}", methodName, args);
        
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("Audit: {} completed in {}ms", methodName, duration);
        
        return result;
    }
}
```

---

# 📦 SERVICE CATALOG

## P0: Infrastructure Services (✅ 8/8 Complete)

### 1. gateway-service (8080)
- **Purpose**: API Gateway, request routing
- **Dependencies**: eureka-server
- **Tech**: Spring Cloud Gateway
- **Status**: ✅ Complete

### 2. eureka-server (8761)
- **Purpose**: Service discovery
- **Dependencies**: None
- **Tech**: Spring Cloud Eureka
- **Status**: ✅ Complete

### 3. webSocket-server (8094)
- **Purpose**: WebSocket message routing
- **Dependencies**: All domain services
- **Tech**: Spring WebSocket + Protocol Buffers
- **Status**: ✅ Complete

### 4. config-service (8888)
- **Purpose**: Centralized configuration
- **Dependencies**: None
- **Tech**: Spring Cloud Config
- **Status**: ✅ Complete

### 5. session-service (8096)
- **Purpose**: JWT authentication, session management
- **Dependencies**: Redis
- **Database**: None (Redis only)
- **Status**: ✅ Complete

### 6. user-service (8110)
- **Purpose**: User account management
- **Dependencies**: session-service
- **Database**: user_db (3307)
- **Status**: ✅ Complete

### 7. report-service (8098)
- **Purpose**: Analytics, event logging
- **Dependencies**: Kafka
- **Database**: report_db (3309)
- **Status**: ✅ Complete

### 8. role-service (8410)
- **Purpose**: Character management, leveling
- **Dependencies**: config-service, Redis
- **Database**: role_db (3308)
- **Status**: ✅ Complete

---

## P1: Economy & Inventory Services (✅ 9/9 Complete)

### 1. wallet-service (8210)
- **Purpose**: Currency management (gold, diamond, points)
- **C++ Module**: `other/wallet/`
- **Frontend**: RoleCtrl (implicit)
- **Database**: wallet_db (3342)
- **Idempotency**: ✅ Required
- **Key Endpoints**:
  - `POST /api/wallet/credit` - Add currency
  - `POST /api/wallet/debit` - Deduct currency
  - `GET /api/wallet/{userId}` - Get balances
- **Status**: ✅ Complete

### 2. item-service (8220)
- **Purpose**: Item metadata catalog (read-only)
- **C++ Module**: `other/item/`
- **Frontend**: BagCtrl, EquipBagCtrl
- **Database**: None (config-service only)
- **Cache**: Redis 30-300s TTL
- **Config Files**: `item/*.json`, `itemmanager.xml`
- **Key Endpoints**:
  - `GET /api/item/{itemId}` - Get item metadata
  - `GET /api/item/list?type={type}` - List items by type
- **Status**: ✅ Complete

### 3. bag-service (8230)
- **Purpose**: Inventory management, item operations
- **C++ Module**: `other/item/itembag.cpp`
- **Frontend**: BagCtrl, ItemRecyclingCtrl
- **Database**: bag_db (3311)
- **Message IDs**: 1500→1505-1507
- **Key Operations**: USE, SELL, SPLIT, MERGE
- **Dependencies**: item-service, gift-service, wallet-service
- **Idempotency**: ✅ Required
- **Key Endpoints**:
  - `POST /api/bag/grant` - Add items
  - `POST /api/bag/consume` - Remove items
  - `POST /api/bag/use` - Use item
  - `POST /api/bag/sell` - Sell items
  - `GET /api/bag/{userId}` - Get inventory
- **Kafka Events**: `gameh5.bag.grant`, `gameh5.bag.changed`
- **Status**: ✅ Complete

### 4. equip-service (8240)
- **Purpose**: Equipment management, wear/unwear
- **C++ Module**: `other/equip/`
- **Frontend**: EquipBagCtrl, FashionCtrl
- **Database**: equip_db (3312)
- **Message IDs**: 1600→1605-1606
- **Key Operations**: WEAR, UNWEAR, ENCHANT
- **Dependencies**: bag-service, item-service
- **Key Endpoints**:
  - `POST /api/equip/wear` - Equip item
  - `POST /api/equip/unequip` - Remove equipment
  - `POST /api/equip/enchant` - Upgrade equipment
  - `GET /api/equip/{roleId}` - Get equipped items
- **Status**: ✅ Complete

### 5. drop-service (8250)
- **Purpose**: RNG/Loot engine
- **C++ Module**: `other/drop/`
- **Frontend**: N/A (server-side only)
- **Database**: drop_db (3313)
- **Config Files**: `drop/*.xml`, `dropmanager.xml`
- **Key Features**: Weight-based rolling, pity system, anti-duplication
- **Key Endpoints**:
  - `POST /api/drop/roll` - Execute drop roll
  - `GET /api/drop/table/{tableId}` - Get drop table info
- **Called By**: battle-service, box-service, dungeon-service
- **Status**: ✅ Complete

### 6. shop-service (8260)
- **Purpose**: Shop purchases, catalog management
- **C++ Module**: `other/shop/`, `other/clothshop/`, `other/mysteryshop/`
- **Frontend**: ShopCtrl, ClothShopCtrl, MysteryShopCtrl
- **Database**: shop_db (3314)
- **Message IDs**: 1620→1621, 1622, 1630→1631
- **Config Files**: `shop_cfg.json`, `cloth_shop.json`, `shop_shenmi.json`
- **Dependencies**: wallet-service, bag-service, item-service
- **Key Endpoints**:
  - `GET /api/shop/catalog` - Get shop items
  - `POST /api/shop/buy` - Purchase item
  - `POST /api/shop/refresh` - Refresh mystery shop
  - `GET /api/shop/limits/{userId}` - Check purchase limits
- **Status**: ✅ Complete

### 7. gift-service (8270)
- **Purpose**: Gift box system
- **C++ Module**: `other/gift/`
- **Frontend**: BagCtrl (USE operation)
- **Database**: gift_db (3315)
- **Config Files**: `item/gift.json`, `server_mail.json`
- **Dependencies**: bag-service, drop-service
- **Gift Types**: Fixed, Random, Choice
- **Key Endpoints**:
  - `POST /api/gift/open` - Open gift box
  - `GET /api/gift/info/{giftId}` - Get gift contents preview
- **Status**: ✅ Complete

### 8. crafting-service (8280)
- **Purpose**: Crafting system, recipe execution
- **C++ Module**: `other/craft/`, `other/gem/`
- **Frontend**: GemAtelierCtrl, EnchantCtrl
- **Database**: crafting_db (3316)
- **Config Files**: `gemstone_drawing.json`, `fumo.json`
- **Dependencies**: bag-service, item-service, wallet-service
- **Key Endpoints**:
  - `POST /api/craft/execute` - Craft item from recipe
  - `GET /api/craft/recipes` - List available recipes
  - `POST /api/craft/salvage` - Break down items
- **Status**: ✅ Complete

### 9. box-service (8290)
- **Purpose**: Gacha/chest orchestration
- **C++ Module**: `other/box/`
- **Frontend**: BoxCtrl
- **Database**: box_db (3310)
- **Message IDs**: 1610→1615-1618
- **Config Files**: `unpack.json`, `kaixiangdaji.json`
- **Dependencies**: drop-service, bag-service, wallet-service, equip-service
- **Key Operations**: OPEN_BOX, WEAR_EQUIP, SELL, ENCHANT
- **Key Endpoints**:
  - `POST /api/box/open` - Open chest/gacha
  - `POST /api/box/enchant` - Enchant equipment
  - `POST /api/box/sell` - Sell box contents
  - `GET /api/box/settings` - Get user preferences
- **Status**: ✅ Complete

---

## P2: Combat & World Services (✅ 9/9 Complete)

### 1. skill-service (8300)
- **Purpose**: Skill metadata catalog (read-only)
- **C++ Module**: `gameworld/skill/`
- **Frontend**: N/A (server-side only)
- **Database**: None (config-service only)
- **Config Files**: `skill/*.xml`, `buff.xml`, `passive.xml`
- **Cache**: Redis 5min TTL
- **Key Endpoints**:
  - `GET /api/skill/{skillId}` - Get skill details
  - `GET /api/skill/list?type={type}` - List skills by type
  - `GET /api/skill/role/{roleId}` - Get learned skills
- **Called By**: battle-service, role-service, client
- **Status**: ✅ Complete

### 2. monster-service (8310)
- **Purpose**: Monster metadata catalog (read-only)
- **C++ Module**: `gameworld/monster/`
- **Frontend**: MonsterCtrl
- **Database**: None (config-service only)
- **Config Files**: `monster/*.xml`, `battlemonstermanager.xml`, `boss.xml`
- **Cache**: Redis 10min TTL
- **Key Endpoints**:
  - `GET /api/monster/{monsterId}` - Get monster details
  - `GET /api/monster/group/{groupId}` - Get spawn group
  - `GET /api/monster/dungeon/{dungeonId}` - Get dungeon monsters
- **Called By**: battle-service, dungeon-service, world-service
- **Status**: ✅ Complete

### 3. battle-service (8320)
- **Purpose**: Server-authoritative combat simulation
- **C++ Module**: `battleserver/battle/`
- **Frontend**: BattleCtrl
- **Database**: battle_db (3320)
- **Message IDs**: 3000-3099
- **Dependencies**: skill-service, monster-service, role-service, equip-service, drop-service, bag-service
- **Combat Types**: PVE_NORMAL, PVE_DUNGEON, PVP_ARENA, PVP_TERRITORY
- **Key Endpoints**:
  - `POST /api/battle/start` - Initialize combat session
  - `POST /api/battle/{battleId}/action` - Submit combat action
  - `POST /api/battle/{battleId}/end` - Finalize battle
  - `GET /api/battle/{battleId}` - Get battle state
  - `GET /api/battle/{battleId}/log` - Get combat log
- **Kafka Events**: `gameh5.battle.started`, `gameh5.battle.ended`
- **Status**: ✅ Complete

### 4. dungeon-service (8340)
- **Purpose**: Instance management, dungeon progression
- **C++ Module**: `other/main_fb/`, `other/petfb/`
- **Frontend**: DungeonCtrl, AdventureCtrl
- **Database**: dungeon_db (3321)
- **Message IDs**: 2005-2015
- **Dependencies**: battle-service, drop-service, bag-service
- **Key Endpoints**:
  - `POST /api/dungeon/enter` - Enter dungeon instance
  - `POST /api/dungeon/{instanceId}/complete` - Complete dungeon
  - `GET /api/dungeon/progress/{roleId}` - Get player progress
- **Kafka Events**: `gameh5.dungeon.cleared`
- **Status**: ✅ Complete

### 5. arena-service (8500)
- **Purpose**: PvP matchmaking, ELO ratings
- **C++ Module**: `other/arena/`, `crossserver/crossarena/`
- **Frontend**: ArenaCtrl, PeakArenaCtrl
- **Database**: arena_db (3333)
- **Message IDs**: 3200-3399
- **Dependencies**: battle-service, role-service, leaderboard-service, wallet-service
- **Matchmaking**: ELO-based (±200 rating)
- **Key Endpoints**:
  - `POST /api/arena/matchmake` - Find opponent
  - `GET /api/arena/rank?roleId={roleId}` - Get player rank
  - `GET /api/arena/leaderboard?top={n}` - Get top N players
  - `GET /api/arena/history?roleId={roleId}` - Match history
  - `POST /api/arena/season/rewards` - Claim season rewards
- **Kafka Events**: `gameh5.pvp.match`
- **Status**: ✅ Complete

### 6-9. Additional Combat Services
- **matchmaking-service** (8350): Queue management
- **combatlog-service** (8360): Battle replay storage
- **crossrealm-service** (8380): Cross-server coordination
- **world-service** (8390): Open world management
- **Status**: ✅ All Complete

---

## P3: Social & Progression Services (✅ 8/8 Complete)

### 1. role-service (8410)
- **Purpose**: Character management, leveling, EXP
- **C++ Module**: `other/roleproperty/`
- **Frontend**: RoleCtrl, LevelupCtrl, CreateRoleCtrl
- **Database**: role_db (3308)
- **Message IDs**: 1400-1405, 1460-1461, 7150
- **Dependencies**: config-service, Redis
- **Key Features**: Auto level-up, stat scaling, unique name generation
- **Key Endpoints**:
  - `GET /api/role/list?userId={userId}` - Get characters
  - `GET /api/role/{roleId}` - Get character details
  - `POST /api/role/` - Create character
  - `POST /api/role/exp/add` - Add EXP (auto level-up)
  - `POST /api/role/{roleId}/rename` - Rename character
- **Cache**: Redis 5min TTL
- **Status**: ✅ Complete (Fully documented in P3.1)

### 2. task-service (8420)
- **Purpose**: Quest/achievement system
- **C++ Module**: `other/task/`
- **Frontend**: TaskCtrl
- **Database**: task_db (3326)
- **Message IDs**: 1451-1452
- **Config Files**: `task.json`, `achievement.json`, `daily_task.json`
- **Dependencies**: bag-service, wallet-service, role-service
- **Task Types**: MAIN_STORY, DAILY, WEEKLY, ACHIEVEMENT
- **Key Endpoints**:
  - `GET /api/task/list?roleId={roleId}` - Get all tasks
  - `POST /api/task/{taskId}/accept` - Accept quest
  - `POST /api/task/{taskId}/update` - Update progress
  - `POST /api/task/{taskId}/complete` - Mark complete
  - `POST /api/task/{taskId}/claim` - Claim rewards
- **Auto-features**: Daily auto-accept, weekly reset (Sunday 00:00)
- **Kafka Events**: `gameh5.task.completed`, `gameh5.achievement.unlocked`
- **Status**: ✅ Complete

### 3. guild-service (8440)
- **Purpose**: Guild management, buildings, wars
- **C++ Module**: `other/roleguild/`, `globalserver/guild/`
- **Frontend**: GuildCtrl, CommodityGuildCtrl
- **Database**: guild_db (3327)
- **Message IDs**: 9640-9646
- **Config Files**: `guild.json`, `guild_tech.json`, `guild_war.json`
- **Dependencies**: wallet-service, role-service, leaderboard-service
- **Key Features**: Buildings, contributions, ranks, territory wars
- **Key Endpoints**:
  - `POST /api/guild/create` - Create guild
  - `GET /api/guild/{guildId}` - Get guild info
  - `POST /api/guild/{guildId}/join` - Join request
  - `POST /api/guild/{guildId}/contribute` - Donate resources
  - `POST /api/guild/{guildId}/building/upgrade` - Upgrade building
- **Business Rules**: Name 2-20 chars unique, auto-disband if leader offline 30+ days
- **Kafka Events**: `gameh5.guild.created`, `gameh5.guild.contribution`, `gameh5.guild.levelup`
- **Status**: ✅ Complete

### 4. friend-service (8450)
- **Purpose**: Friend list, invitations
- **C++ Module**: `other/friend/`
- **Frontend**: FriendsRankCtrl, InviteFriendCtrl
- **Database**: friend_db (3328)
- **Message IDs**: 4000-4099
- **Key Endpoints**:
  - `GET /api/friend/list?userId={userId}` - Get friend list
  - `POST /api/friend/invite` - Send friend request
  - `POST /api/friend/accept` - Accept request
  - `DELETE /api/friend/{friendId}` - Remove friend
- **Status**: ✅ Complete

### 5. mail-service (8460)
- **Purpose**: System mail, attachments, expiry
- **C++ Module**: `other/rolemail/`
- **Frontend**: N/A (push notifications)
- **Database**: mail_db (3329)
- **Message IDs**: 9501-9507
- **Config Files**: `server_mail.json`, `gift.json`
- **Dependencies**: bag-service, wallet-service, gift-service
- **Key Features**: Attachments, 7-day expiry, broadcast mail
- **Key Endpoints**:
  - `GET /api/mail/inbox?roleId={roleId}` - Get all mail
  - `POST /api/mail/{mailId}/claim` - Claim attachments
  - `DELETE /api/mail/{mailId}` - Delete mail
  - `POST /api/mail/send` - Send mail (admin)
  - `POST /api/mail/broadcast` - Broadcast to all (admin)
- **Business Rules**: Max 100 mails, auto-delete oldest, unclaimed attachments block deletion
- **Cache**: Redis 5min TTL
- **Status**: ✅ Complete

### 6. chat-service (8470)
- **Purpose**: Multi-channel chat, filters
- **C++ Module**: `other/chat/`
- **Frontend**: N/A (WebSocket only)
- **Database**: chat_db (3330)
- **Channels**: World, Guild, Private, System
- **Key Features**: Profanity filter, rate limiting, message history
- **Status**: ✅ Complete

### 7. leaderboard-service (8480)
- **Purpose**: Global/guild rankings
- **C++ Module**: `other/ranking/`, `globalserver/leaderboard/`
- **Frontend**: RankCtrl
- **Database**: leaderboard_db (3331)
- **Message IDs**: 9601-9602
- **Key Features**: Real-time ranking updates, score tracking
- **Key Endpoints**:
  - `GET /api/leaderboard/{type}/top?n={n}` - Get top N
  - `GET /api/leaderboard/{type}/rank?userId={userId}` - Get user rank
  - `POST /api/leaderboard/{type}/update` - Update score
- **Leaderboard Types**: Level, Power, Arena, Guild
- **Cache**: Redis 2min TTL
- **Status**: ✅ Complete

### 8. activity-service (8490)
- **Purpose**: Server-wide activities, participation tracking
- **C++ Module**: `other/roleactivity/`, `other/openserveractivity/`
- **Frontend**: ActivityCtrl, ServerActivityCtrl, TodayShareCtrl, MoreServerActivityCtrl
- **Database**: activity_db (3332)
- **Message IDs**: 4200-4499
- **Key Endpoints**:
  - `GET /api/activity/list` - Get active activities
  - `POST /api/activity/{activityId}/participate` - Record participation
  - `POST /api/activity/{activityId}/claim` - Claim rewards
- **Status**: ✅ Complete

---

## P4: Supporting Services (✅ 9/9 Complete)

### 1. pet-service (8112 / 8088)
- **Purpose**: Pet collection, evolution, combat
- **C++ Module**: `other/pet/`
- **Frontend**: PetCtrl, PetClothCtrl, PetGuardCtrl, PetRelicsCtrl
- **Database**: game_pet / db_pet_service
- **Message IDs**: 2100-2120
- **Config Files**: `pet.json`
- **Key Features**: 
  - 5 entities: Pet, TSGem, Cloth, Remains, FightIndex
  - Pet leveling, evolution (5 tiers), skills (4 slots)
  - Equipment system (clothes, gems)
  - Combat pets with fight power calculation
  - Breeding and remains collection
- **Key Endpoints**: 42 REST endpoints
- **Status**: ✅ Complete

### 2. mount-service (8089)
- **Purpose**: Mount riding, upgrades
- **C++ Module**: `other/mount/`
- **Frontend**: MountCtrl
- **Database**: game_mount
- **Message IDs**: 2140-2145
- **Config Files**: `harness.json`
- **Key Features**: 
  - 2 entities: Mount, MountHarness
  - Mount collection with speed bonuses
  - Harness equipment with 4 attributes (ATK, DEF, HP, SPD)
  - Level/star/grade upgrade system
  - Combat mounts
- **Key Endpoints**: 27 REST endpoints
- **Status**: ✅ Complete

### 3. angel-service (8090)
- **Purpose**: Angel companion system
- **C++ Module**: `other/angel/`
- **Frontend**: AngelCtrl, AngelFesCtrl
- **Database**: game_angel
- **Message IDs**: 2130-2132
- **Config Files**: `angel.json`
- **Key Features**: 
  - 1 entity: Angel with 4 skill slots
  - Angel summon, upgrades, evolution
  - Blessing system (3 types)
  - Angel dungeons
  - Power calculation
- **Key Endpoints**: 17 REST endpoints
- **Status**: ✅ Complete

### 4. artifact-service (8091)
- **Purpose**: Divine artifact (shenqi)
- **C++ Module**: `other/shenqi/`
- **Frontend**: ShenqiCtrl, ShenQiDrawCtrl
- **Database**: game_artifact
- **Message IDs**: 1675-1680
- **Config Files**: `shenqi.json`
- **Key Features**: 
  - 1 entity: Artifact with 5 upgrade paths
  - Artifact collection with 4 attributes (ATK, DEF, HP, CRIT)
  - Power-ups (level, star, grade, breakthrough)
  - Gacha system for artifact acquisition
- **Key Endpoints**: 19 REST endpoints
- **Status**: ✅ Complete (has 1 typo: @PathFamily → @PathVariable)

### 5. starmap-service (8092)
- **Purpose**: Star map progression system
- **C++ Module**: `other/starmap/`
- **Frontend**: StarMapCtrl, StarMapFesCtrl
- **Database**: game_starmap
- **Message IDs**: 2150-2152
- **Config Files**: `starmap.json`
- **Key Features**: 
  - 2 entities: Star, Constellation
  - Constellation unlocking (12 stars each)
  - Stars max level 50, Constellations max level 20
  - Stat bonuses and power calculation
  - Completion tracking
- **Key Endpoints**: 13 REST endpoints
- **Status**: ✅ Complete

### 6. rune-service (8093)
- **Purpose**: Rune enhancement system
- **C++ Module**: `other/rune/`
- **Frontend**: RuneCtrl
- **Database**: game_rune
- **Message IDs**: 1670-1672
- **Config Files**: `rune.json`
- **Key Features**: 
  - 1 entity: Rune with 24 fields
  - 5 quality tiers (White→Orange)
  - Rune socketing with 10 equipment slots
  - Upgrades: level (100), star (10), quality (5), refinement (20)
  - Dynamic sub-attributes system (0-3 based on quality)
  - Set bonuses
- **Key Endpoints**: 20 REST endpoints
- **Status**: ✅ Complete

### 7. trial-service (8094)
- **Purpose**: Trial tower challenges
- **C++ Module**: `other/shilianpagoda/`, `other/gumopagoda/`
- **Frontend**: TrialCtrl
- **Database**: game_trial
- **Message IDs**: 3400-3499
- **Key Features**: 
  - 1 entity: TrialRecord with 17 fields
  - Tower climbing with stage progression
  - Daily attempts (3/day) with auto-reset
  - Star ratings (0-3 per stage)
  - Floor rewards with bit flags (supports 64 stages)
  - Speed records and leaderboards
- **Key Endpoints**: 21 REST endpoints
- **Status**: ✅ Complete

### 8. territory-service (8095)
- **Purpose**: Territory warfare and base management
- **C++ Module**: `crossserver/territory/`
- **Frontend**: TerritoryCtrl
- **Database**: game_territory
- **Message IDs**: 3600-3699
- **Key Features**: 
  - 2 entities: Territory (25 fields), TerritoryBuilding (14 fields)
  - Guild vs guild territory battles
  - Base management with building system (10+ slots)
  - Hourly production (gold, resources) with storage caps
  - Time-based construction (2 hours)
  - Building statuses: Empty, Constructing, Built, Upgrading
  - Max territory level 50
- **Key Endpoints**: 24 REST endpoints
- **Status**: ✅ Complete

### 9. escort-service (8096)
- **Purpose**: Escort missions
- **C++ Module**: `crossserver/escort/`
- **Frontend**: EscortCtrl
- **Database**: game_escort
- **Message IDs**: 3500-3599
- **Key Features**: 
  - 2 entities: EscortMission (22 fields), EscortStats (14 fields)
  - Transport missions with 5 quality tiers
  - PvP intercept mechanics (10% attack chance)
  - Time-limited missions (2 hours expiry)
  - Daily limits (10 missions, 3 refreshes)
  - Weighted random generation (50%W, 30%G, 15%B, 4%P, 1%O)
  - Perfect completion bonuses (1.5× if no attacks)
  - Comprehensive statistics tracking
- **Key Endpoints**: 23 REST endpoints
- **Status**: ✅ Complete

**Note**: Port conflict detected - session-service and escort-service both use 8096. Consider reassigning pet-service from 8112 to 8088 for consistency.

---

## P5: Production Deployment (✅ Complete)

### Infrastructure Ready:
- ✅ Docker Compose configurations
- ✅ Kubernetes manifests (K8s)
- ✅ Prometheus + Grafana monitoring
- ✅ Kafka message broker
- ✅ Redis cluster
- ✅ MySQL replication setup
- ✅ CI/CD pipelines
- ✅ Backup & disaster recovery

---

# 📚 REFERENCE DOCUMENTATION

## Key Files

| File | Purpose | Location |
|------|---------|----------|
| **PROJECT_PHASES_ANALYSIS.md** | Complete project documentation | `D:\project\serverGame\` |
| **FRONTEND_CONTROLLER_PROTOCOL_MAPPING.md** | Frontend to backend mapping | `D:\project\serverGame\` |
| **Migration_C++_to_Java.md** | Migration strategy | `D:\project\serverGame\开箱h5\server\server\src\` |
| **PORT_COMPARISON_REPORT.md** | Service port assignments | `D:\project\serverGame\GameServer\` |
| **WEBSOCKET_INTEGRATION_PLAN.md** | WebSocket architecture | `D:\project\serverGame\GameServer\` |

## Source Code Locations

| Component | Path |
|-----------|------|
| C++ Backend | `D:\project\serverGame\开箱h5\server\server\src\` |
| TypeScript Frontend | `D:\project\serverGame\client\LineR\assets\script\` |
| Java Services | `D:\project\serverGame\GameServer\{service-name}\` |
| Common Library | `D:\project\serverGame\GameServer\common-lib\` |
| Config Files | `D:\project\serverGame\开箱h5\server\server\src\config\` |

---

# ✅ CHECKLIST FOR NEW SERVICE

Use this checklist when implementing a new service:

## Planning Phase
- [ ] Identify C++ module in `gameworld/other/`
- [ ] Map frontend TypeScript controller
- [ ] Document protocol message IDs (CS/SC)
- [ ] List config files needed
- [ ] Identify database tables from C++
- [ ] Map dependencies (which services to call)

## Implementation Phase
- [ ] Create Maven project structure
- [ ] Configure pom.xml with dependencies
- [ ] Create application.yml with correct port
- [ ] Implement domain entities (JPA)
- [ ] Create Flyway migration (V1__init_{service}.sql)
- [ ] Implement repository layer
- [ ] Implement service layer with business logic
- [ ] Create REST controller with endpoints
- [ ] Add Feign clients for dependencies
- [ ] Implement WebSocket handler (if needed)
- [ ] Add caching (Redis)
- [ ] Add Kafka event publishing (if needed)

## Testing Phase
- [ ] Write unit tests (80%+ coverage)
- [ ] Write integration tests
- [ ] Test Feign client contracts
- [ ] Load test (1000+ concurrent requests)
- [ ] Security testing (auth/authz)

## Deployment Phase
- [ ] Create Dockerfile
- [ ] Add to docker-compose.yml
- [ ] Register with Eureka
- [ ] Configure in gateway routes
- [ ] Add Prometheus metrics
- [ ] Update documentation

## Validation Phase
- [ ] Test with frontend client
- [ ] Verify protocol messages
- [ ] Check database performance
- [ ] Monitor logs & metrics
- [ ] Run smoke tests

---

# 🎯 AGENT INSTRUCTIONS

## When asked to implement a service:

1. **Analyze first**: Read C++ code + TypeScript controller
2. **Plan thoroughly**: Document message IDs, endpoints, dependencies
3. **Code systematically**: Follow the service implementation guide
4. **Test rigorously**: Meet 80%+ coverage requirement
5. **Document clearly**: Update this guide with findings

## Code Quality Standards:

- ✅ Clean Code: SonarLint violations = 0
- ✅ Test Coverage: 80%+ (unit + integration)
- ✅ Performance: <100ms p95 response time
- ✅ Security: No SQL injection, XSS, or auth bypass vulnerabilities
- ✅ Logging: Structured logs with correlation IDs
- ✅ Monitoring: Prometheus metrics exposed

## Communication Patterns:

- **Sync calls**: Use Feign for REST APIs (read operations, validation)
- **Async calls**: Use Kafka for events (notifications, analytics)
- **Caching**: Use Redis for hot data (config, metadata, session)
- **Idempotency**: Always use idempotency keys for write operations

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-19  
**Maintained By**: AI Development Team  
**Status**: ACTIVE - Ready for P4 implementation

---

*This guide is a living document. Update it as you learn more about the system.*
