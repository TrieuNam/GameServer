# Pet Service Documentation

## Overview

**Service Name**: `pet-service`  
**Port**: `8110`  
**Database**: `game_pet`  
**Status**: ✅ **Backend Complete**  

Pet service quản lý hệ thống thú cưng trong game, bao gồm kích hoạt, nâng cấp, tiến hóa và trang bị pet.

---

## Architecture

```
Browser (TypeScript)
    ↓ WebSocket (Binary Proto)
webSocket-server
    ↓ PetHandler
    ↓ PetFeign (HTTP REST)
pet-service (Spring Boot)
    ↓ PetService (Business Logic)
    ↓ PetRepository (JPA)
MySQL (game_pet.t_pet)
```

---

## Database Schema

### Table: `t_pet`

```sql
CREATE TABLE t_pet (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pet_id BIGINT NOT NULL UNIQUE COMMENT 'Pet unique ID',
    role_id BIGINT NOT NULL COMMENT 'Owner role ID',
    pet_template_id INT NOT NULL COMMENT 'Pet template/species ID',
    level INT NOT NULL DEFAULT 1 COMMENT 'Pet level (1-100)',
    pet_exp INT NOT NULL DEFAULT 0 COMMENT 'Pet experience points',
    pet_star INT NOT NULL DEFAULT 1 COMMENT 'Pet star rating (1-5)',
    pet_status INT NOT NULL DEFAULT 0 COMMENT '0=inactive, 1=active, 2=locked',
    pet_name VARCHAR(50) COMMENT 'Custom pet name',
    hp INT DEFAULT 0 COMMENT 'Health points',
    attack INT DEFAULT 0 COMMENT 'Attack power',
    defense INT DEFAULT 0 COMMENT 'Defense power',
    speed INT DEFAULT 0 COMMENT 'Speed stat',
    create_time DATETIME NOT NULL,
    update_time DATETIME,
    INDEX idx_role_id (role_id),
    INDEX idx_pet_template_id (pet_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Indexes**:
- `idx_role_id`: Query pets by owner
- `idx_pet_template_id`: Query by species

---

## REST API Endpoints

### 1. Get All Pets
```http
GET /api/pet/{roleId}
```

**Response**:
```json
{
    "success": true,
    "pets": [
        {
            "petId": 1,
            "petTemplateId": 1001,
            "level": 25,
            "petExp": 500,
            "petStar": 2,
            "petStatus": 1,
            "petName": "FireDragon",
            "hp": 350,
            "attack": 60,
            "defense": 60,
            "speed": 30
        }
    ],
    "count": 1
}
```

---

### 2. Activate Pet
```http
POST /api/pet/{roleId}/activate/{petTemplateId}
```

**Description**: Kích hoạt pet mới lần đầu (unlock)

**Response**:
```json
{
    "success": true,
    "petData": {
        "petId": 2,
        "petTemplateId": 1002,
        "level": 1,
        "petExp": 0,
        "petStar": 1,
        "petStatus": 0,
        "hp": 100,
        "attack": 10,
        "defense": 10,
        "speed": 10
    }
}
```

**Business Logic**:
- Generate new `petId` = max(petId) + 1
- Base stats: HP=100, ATK=10, DEF=10, SPD=10
- Initial status: inactive (0)
- Initial level: 1, star: 1

---

### 3. Upgrade Pet
```http
POST /api/pet/{roleId}/upgrade
Content-Type: application/json

{
    "petId": 1,
    "materialIds": [30101, 30102, 30103]
}
```

**Description**: Nâng cấp level pet bằng cách sử dụng vật phẩm exp

**Response**:
```json
{
    "success": true,
    "newLevel": 26,
    "newExp": 600,
    "expGained": 300,
    "petData": { ... }
}
```

**Business Logic**:
- Mỗi material item = 100 exp
- 1000 exp = 1 level
- Max level = 100
- Level up rewards:
  - HP +10
  - Attack +2
  - Defense +2
  - Speed +1

---

### 4. Evolve Pet
```http
POST /api/pet/{roleId}/evolve/{petId}
```

**Description**: Tiến hóa pet (tăng số sao)

**Response**:
```json
{
    "success": true,
    "oldStar": 1,
    "newStar": 2,
    "petData": { ... }
}
```

**Business Logic**:
- Requirements:
  - Max star = 5
  - Level >= star * 20
    - 1★ → 2★: level >= 20
    - 2★ → 3★: level >= 40
    - 3★ → 4★: level >= 60
    - 4★ → 5★: level >= 80
- Evolution rewards:
  - HP +50
  - Attack +10
  - Defense +10
  - Speed +5

---

### 5. Set Active Pet
```http
POST /api/pet/{roleId}/setactive/{petId}
```

**Description**: Trang bị pet (chỉ 1 pet active tại một thời điểm)

**Response**:
```json
{
    "success": true,
    "activePetId": 1,
    "petData": { ... }
}
```

**Business Logic**:
- Deactivate current active pet (status=1 → 0)
- Activate new pet (status → 1)
- Only one pet can be active

---

### 6. Health Check
```http
GET /api/pet/health
```

**Response**:
```json
{
    "status": "UP",
    "service": "pet-service",
    "timestamp": 1738368000000
}
```

---

## Java Service Structure

### Entity: `Pet.java`
```java
@Data
@Entity
@Table(name = "t_pet")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pet_id", nullable = false, unique = true)
    private Long petId;
    
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    
    // ... other fields
}
```

### Repository: `PetRepository.java`
```java
@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByRoleIdOrderByPetIdAsc(Long roleId);
    Optional<Pet> findByRoleIdAndPetId(Long roleId, Long petId);
    Optional<Pet> findByRoleIdAndPetStatus(Long roleId, Integer petStatus);
    long countByRoleId(Long roleId);
    boolean existsByRoleIdAndPetTemplateId(Long roleId, Integer petTemplateId);
    
    @Query("SELECT MAX(p.petId) FROM Pet p WHERE p.roleId = :roleId")
    Long getMaxPetIdByRoleId(Long roleId);
}
```

### Service: `PetService.java`
```java
@Service
@RequiredArgsConstructor
public class PetService {
    private final PetRepository petRepository;
    
    public List<PetDTO> getRolePets(Long roleId);
    public Map<String, Object> activatePet(Long roleId, Integer petTemplateId);
    public Map<String, Object> upgradePet(Long roleId, Long petId, List<Long> materialIds);
    public Map<String, Object> evolvePet(Long roleId, Long petId);
    public Map<String, Object> setActivePet(Long roleId, Long petId);
}
```

### Controller: `PetController.java`
```java
@RestController
@RequestMapping("/api/pet")
@RequiredArgsConstructor
public class PetController {
    private final PetService petService;
    
    @GetMapping("/{roleId}")
    public Map<String, Object> getRolePets(@PathVariable Long roleId);
    
    @PostMapping("/{roleId}/activate/{petTemplateId}")
    public Map<String, Object> activatePet(...);
    
    @PostMapping("/{roleId}/upgrade")
    public Map<String, Object> upgradePet(...);
    
    @PostMapping("/{roleId}/evolve/{petId}")
    public Map<String, Object> evolvePet(...);
    
    @PostMapping("/{roleId}/setactive/{petId}")
    public Map<String, Object> setActivePet(...);
    
    @GetMapping("/health")
    public Map<String, Object> health();
}
```

---

## Configuration

### application.yml
```yaml
server:
  port: 8110

spring:
  application:
    name: pet-service
  datasource:
    url: jdbc:mysql://localhost:3306/game_pet
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
cd D:\project\serverGame\GameServer\pet-service
mvn clean install
mvn spring-boot:run
```

### Test Endpoints
```bash
# Health check
curl http://localhost:8110/api/pet/health

# Get pets (empty initially)
curl http://localhost:8110/api/pet/1

# Activate pet
curl -X POST http://localhost:8110/api/pet/1/activate/1001

# Upgrade pet
curl -X POST http://localhost:8110/api/pet/1/upgrade \
  -H "Content-Type: application/json" \
  -d '{"petId":1,"materialIds":[30101,30102]}'

# Evolve pet
curl -X POST http://localhost:8110/api/pet/1/evolve/1

# Set active
curl -X POST http://localhost:8110/api/pet/1/setactive/1
```

---

## Status

- ✅ Entity created
- ✅ Repository created
- ✅ Service logic implemented
- ✅ Controller endpoints created
- ✅ Configuration ready
- ⏳ Needs deployment testing
- ⏳ Needs integration with WebSocket handler

---

## Next Steps

1. Deploy and test pet-service
2. Update `PetFeign` with missing methods
3. Expand `PetHandler` to handle all 5 operations
4. Create TypeScript client `PetService.ts`
5. E2E test

---

## Related Documentation

- [PetHandler Documentation](../handlers/PetHandler.md)
- [PetService.ts Client](../clients/PetService.md)
- [Phase 1 Roadmap](../../WEBSOCKET_SERVICE_CLIENT_ROADMAP.md)

---

*Last Updated: 2026-01-31*
