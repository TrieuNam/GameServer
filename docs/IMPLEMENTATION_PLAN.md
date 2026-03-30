# Pet Service Implementation Plan

## 1. Service Overview

**Service Name**: pet-service (P4.1)  
**Port**: 8520  
**Database**: pet_db (MySQL port: 3340)  
**Message IDs**: 2100-2139  
**Dependencies**: config-service, role-service, bag-service, item-service

### Purpose
Manages pet collection, evolution, equipment (gems/clothing), combat stats, and progression systems.

---

## 2. C++ Analysis Summary

### 2.1 Core Modules Analyzed
- **pet/pet.cpp** (3082 lines) - Main implementation
- **pet/petconfig.cpp** - Configuration loader
- **pet/petclothconfig.cpp** - Pet clothing system
- **pet/petremainsconfig.cpp** - Pet relics/remains system
- **proto/msgpet.proto** - Protocol definitions

### 2.2 Data Structures

#### Four Main Collections:
1. **m_pet_map** - Pet collection (pet_index → PetData)
2. **m_ts_gem_map** - Special gems (gem_index → TSGemData)
3. **m_cloth_map** - Pet clothing (cloth_id → ClothData)
4. **m_remains_map** - Pet relics (remains_index → RemainsData)

#### PetData Structure (from C++)
```cpp
struct PetData {
    int pet_index;              // Unique index in player's collection
    int pet_id;                 // Pet type ID (references config)
    int level;                  // Pet level (max = role level)
    int exp;                    // Experience points
    int order;                  // Awakening tier (grade)
    int skill_list[PET_SKILL_MAXINUM];      // Skill slots (6 max)
    int gem_item_id[PET_GEM_SLOT_NUM];      // Normal gems (4 slots)
    int ts_gem_index[PET_TS_GEM_SLOT_NUM];  // Special gems (2 slots)
    int skill_lock_flag;        // Skill lock bitmask
    int cloth_id;               // Equipped clothing ID
    int64 capability;           // Combat power (calculated)
}
```

#### TSGemData (Special Gems)
```cpp
struct TSGemData {
    int gem_index;              // Unique index
    int gem_level;              // Gem level
    int pet_index;              // Equipped on which pet (0=not equipped)
    int attr_type[4];           // Attribute types (random)
    int attr_value[4];          // Attribute values (random)
}
```

#### ClothData (Pet Clothing)
```cpp
struct ClothData {
    int item_id;                // Clothing ID
    int level;                  // Clothing level
    int pet_index;              // Worn by which pet (0=not equipped)
}
```

#### RemainsData (Pet Relics)
```cpp
struct RemainsData {
    int index;                  // Unique index
    int id;                     // Relic type ID
    int grade;                  // Relic grade
    int level;                  // Relic level
    int exp;                    // Experience
}
```

### 2.3 Core Operations (from TypeScript PET_OP_TYPE)

| Operation | Code | Parameters | Description |
|-----------|------|------------|-------------|
| LEVEL_UP | 0 | p1:pet_index, p2:1/10 | Upgrade pet level (1 or 10 times) |
| GRADE_UP | 1 | p1:pet_index, p_list:materials | Awaken pet (increase order/tier) |
| SKILL_LEARN | 2 | p1:pet_index, p2:lock_flag, p3:skill_book_id | Learn/replace skill |
| INLAY_GEM | 3 | p1:pet_index, p2:slot[0-3], p3:gem_id | Equip normal gem |
| GEM_LEVEL_UP_BAG | 4 | p1:item_id, p_list:materials | Upgrade gem in bag |
| GEM_LEVEL_UP_PET | 5 | p1:pet_index, p2:slot[0-3], p_list:materials | Upgrade equipped gem |
| INLAY_TS_GEM | 6 | p1:pet_index, p2:slot[0-1], p3:gem_index | Equip special gem |
| TS_GEM_LEVEL_UP | 7 | p1:gem_index, p_list:materials | Upgrade special gem |
| TS_GEM_REFRESH | 8 | p1:gem_index, p2:lock_flag | Reroll gem attributes |
| SET_FIGHT | 9 | p1:pet_index | Set active pet for battle |
| DISCARD | 10 | p1:pet_index | Release pet |
| SKILL_LOCK | 11 | p1:pet_index, p2:lock_flag | Lock skill slots |
| TREASURE | 12 | p1:type | Pet gacha draw |
| GRADE_UP_EVO | 13 | p1:pet_index | Evolve pet (new pet_id) |
| OK_GEM_LEVEL_UP_PET | 14 | p1:pet_index, p2:slot | One-key gem upgrade (auto) |
| OK_TS_GEM_LEVEL_UP | 15 | p1:gem_index | One-key special gem upgrade |
| SEND_EVO_ATTR | 16 | p1:pet_index | Preview evolution attributes |
| CLOTH_UP | 17 | p1:cloth_id, p2:use_diamonds | Upgrade clothing |
| CLOTH_WEAR | 18 | p1:pet_index, p2:cloth_id | Wear clothing |
| SKILL_UNLOCK | 19 | p1:pet_index, p2:seq | Unlock skill slot |

### 2.4 Protocol Messages (msgpet.proto)

```protobuf
// Request (MsgId: 2100)
message PB_CSRolePetReq {
    optional int32 req_type = 1;       // PET_OP_TYPE
    optional int32 param_1 = 2;
    optional int32 param_2 = 3;
    optional int32 param_3 = 4;
    repeated int32 param_list = 5;
}

// Full pet info on login (MsgId: 2101)
message PB_SCRolePetAllInfo {
    repeated int32 fight_pet_index = 1;
    repeated PB_SCRolePetData pet_list = 2;
    repeated PB_SCRoleTSGemData ts_gem_list = 3;
    repeated PB_SCRoleClothData cloth_list = 4;
}

// Single pet update (MsgId: 2102)
message PB_SCRolePetSignleInfo {
    optional PB_SCRolePetData pet_node = 1;
}

// Single gem update (MsgId: 2103)
message PB_SCRoleTSGemSignleInfo {
    optional PB_SCRoleTSGemData ts_gem_node = 1;
}

// Operation result (MsgId: 2104)
message PB_SCRolePetRetInfo {
    optional int32 ret_type = 1;    // PET_RET_TYPE
    optional int32 ret_p1 = 2;
    optional int32 ret_p2 = 3;
}

// One-key gem upgrade (MsgId: 2105)
message PB_CSPetOneKeyUpLevelGemReq {
    repeated PB_OneKeyPetGemInfo items = 1;
}

// Evolution preview (MsgId: 2106)
message PB_SCPetSendEvoAttr {
    optional int32 pet_index = 1;
    repeated int32 attr_list = 2;
}

// Remains list (MsgId: 2107)
message PB_SCPetRemainsList {
    optional int32 send_type = 1;  // 0:all, 1:single
    repeated PB_SCPetRemainsNode remains_list = 2;
}
```

### 2.5 Business Logic Highlights

#### Level Up Logic
```cpp
void Pet::OnLevelUp(int pet_index, int num) {
    // 1. Validate pet exists
    // 2. Loop num times (1 or 10)
    // 3. Check level <= role.level (pet can't exceed role)
    // 4. Calculate gold cost (GOLD_COIN_ITEM_ID)
    // 5. Consume gold
    // 6. Update pet level
    // 7. Send update to client
    // 8. Trigger event OnPetLevelUp
}
```

#### Grade Up (Awakening) Logic
```cpp
void Pet::OnGradeUp(int pet_index, int cost_count, int cost_index[]) {
    // 1. Validate pet exists
    // 2. Check next order config exists
    // 3. Get advance config for current order
    // 4. Validate material count matches config
    // 5. Check can't use same pet as material
    // 6. Validate all material pets meet requirements
    // 7. Consume upgrade item (soul stones)
    // 8. Increase pet.order += 1
    // 9. Delete material pets
    // 10. Send update
}
```

#### Evolution Logic (New Pet)
```cpp
void Pet::OnGradeUpEvo(int pet_index) {
    // 1. Check pet level >= evo_level (e.g., 200)
    // 2. Get evolution config (pet_id → new_pet_id)
    // 3. Consume evolution materials
    // 4. Change pet_id to new_pet_id
    // 5. Add bonus levels (e.g., +10)
    // 6. Send PET_RET_UP_EVO with new pet_id
}
```

#### Gem System
- **Normal Gems**: 4 slots per pet, stored by item_id
- **Special Gems**: 2 slots per pet, stored by gem_index (separate collection)
- **Upgrade**: Requires 2 gems of same/higher level → 1 higher-level gem
- **One-Key Upgrade**: Algorithm finds all materials in bag + equipped, upgrades automatically

#### Special Gem Attributes
- Each gem has 4 random attributes (type + value)
- Refresh operation re-rolls unlocked attributes
- Lock flag controls which attributes to keep
- Cost increases with number of locked attributes

#### Dirty Tracking System
```cpp
std::set<int> m_pet_dirty;          // Modified pet indices
std::set<int> m_ts_gem_dirty;       // Modified gem indices
std::set<int> m_cloth_dirty;        // Modified cloth IDs
std::set<int> m_remains_dirty;      // Modified remains indices
```
- Changes marked dirty
- GetChangeItemList() serializes dirty items for DB save
- CS_UPDATE / CS_DELETE change states
- ClearDirtyMark() after successful DB write

---

## 3. Database Schema Design

### 3.1 Tables

#### pet_list
```sql
CREATE TABLE pet_list (
    user_id BIGINT NOT NULL,
    pet_index INT NOT NULL,
    pet_id INT NOT NULL,
    level INT NOT NULL DEFAULT 1,
    exp BIGINT NOT NULL DEFAULT 0,
    `order` INT NOT NULL DEFAULT 1,
    skill_list VARCHAR(255),         -- JSON array: [skill1, skill2, ...]
    gem_item_id VARCHAR(255),        -- JSON array: [gem1, gem2, gem3, gem4]
    ts_gem_index VARCHAR(255),       -- JSON array: [gem_idx1, gem_idx2]
    skill_lock_flag INT NOT NULL DEFAULT 0,
    cloth_id INT NOT NULL DEFAULT 0,
    capability BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, pet_index),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### pet_ts_gem
```sql
CREATE TABLE pet_ts_gem (
    user_id BIGINT NOT NULL,
    gem_index INT NOT NULL,
    gem_level INT NOT NULL,
    pet_index INT NOT NULL DEFAULT 0,
    attr_type VARCHAR(255),          -- JSON array: [type1, type2, type3, type4]
    attr_value VARCHAR(255),         -- JSON array: [val1, val2, val3, val4]
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, gem_index),
    INDEX idx_user_pet (user_id, pet_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### pet_cloth
```sql
CREATE TABLE pet_cloth (
    user_id BIGINT NOT NULL,
    cloth_id INT NOT NULL,
    level INT NOT NULL DEFAULT 0,
    pet_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, cloth_id),
    INDEX idx_user_pet (user_id, pet_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### pet_remains
```sql
CREATE TABLE pet_remains (
    user_id BIGINT NOT NULL,
    remains_index INT NOT NULL,
    remains_id INT NOT NULL,
    grade INT NOT NULL DEFAULT 1,
    level INT NOT NULL DEFAULT 1,
    exp BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, remains_index),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### pet_fight_index (role data - may live in role-service)
```sql
CREATE TABLE pet_fight_index (
    user_id BIGINT NOT NULL PRIMARY KEY,
    fight_pet_index INT NOT NULL DEFAULT 0,
    fight_pet_index2 INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3.2 Flyway Migration Script

**File**: `V1__init_pet_schema.sql`

---

## 4. Java Service Structure

### 4.1 Project Structure
```
GameServer/pet-service/
├── pom.xml
├── src/main/java/com/game/pet/
│   ├── PetServiceApplication.java
│   ├── controller/
│   │   └── PetController.java
│   ├── service/
│   │   ├── PetService.java
│   │   ├── PetGemService.java
│   │   ├── PetClothService.java
│   │   └── PetRemainsService.java
│   ├── model/
│   │   ├── entity/
│   │   │   ├── Pet.java
│   │   │   ├── PetTSGem.java
│   │   │   ├── PetCloth.java
│   │   │   └── PetRemains.java
│   │   ├── dto/
│   │   │   ├── PetOperationRequest.java
│   │   │   ├── PetAllInfoResponse.java
│   │   │   └── PetStatsDTO.java
│   │   └── enums/
│   │       ├── PetOpType.java
│   │       └── PetRetType.java
│   ├── repository/
│   │   ├── PetRepository.java
│   │   ├── PetTSGemRepository.java
│   │   ├── PetClothRepository.java
│   │   └── PetRemainsRepository.java
│   ├── feign/
│   │   ├── ConfigClient.java
│   │   ├── RoleClient.java
│   │   └── BagClient.java
│   └── config/
│       └── FeignConfig.java
└── src/main/resources/
    ├── application.yml
    └── db/migration/
        └── V1__init_pet_schema.sql
```

### 4.2 Key Classes

#### PetOpType Enum
```java
public enum PetOpType {
    LEVEL_UP(0),
    GRADE_UP(1),
    SKILL_LEARN(2),
    INLAY_GEM(3),
    GEM_LEVEL_UP_BAG(4),
    GEM_LEVEL_UP_PET(5),
    INLAY_TS_GEM(6),
    TS_GEM_LEVEL_UP(7),
    TS_GEM_REFRESH(8),
    SET_FIGHT(9),
    DISCARD(10),
    SKILL_LOCK(11),
    TREASURE(12),
    GRADE_UP_EVO(13),
    OK_GEM_LEVEL_UP_PET(14),
    OK_TS_GEM_LEVEL_UP(15),
    SEND_EVO_ATTR(16),
    CLOTH_UP(17),
    CLOTH_WEAR(18),
    SKILL_UNLOCK(19);
    
    private final int code;
    // ... constructor, getters, fromCode()
}
```

#### Pet Entity
```java
@Entity
@Table(name = "pet_list")
public class Pet {
    @EmbeddedId
    private PetId id;
    
    private Integer petId;
    private Integer level = 1;
    private Long exp = 0L;
    @Column(name = "`order`")
    private Integer order = 1;
    
    @Convert(converter = IntListConverter.class)
    private List<Integer> skillList;
    
    @Convert(converter = IntListConverter.class)
    private List<Integer> gemItemId;
    
    @Convert(converter = IntListConverter.class)
    private List<Integer> tsGemIndex;
    
    private Integer skillLockFlag = 0;
    private Integer clothId = 0;
    private Long capability = 0L;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Getters, setters, methods
}

@Embeddable
class PetId implements Serializable {
    private Long userId;
    private Integer petIndex;
}
```

#### PetService Interface
```java
@Service
public interface PetService {
    // Core operations
    PetAllInfoResponse getAllPetInfo(Long userId);
    void addPet(Long userId, Integer petId);
    void levelUp(Long userId, Integer petIndex, Integer num);
    void gradeUp(Long userId, Integer petIndex, List<Integer> materialIndices);
    void evolve(Long userId, Integer petIndex);
    
    // Skill operations
    void learnSkill(Long userId, Integer petIndex, Integer skillIndex, Integer skillItemId);
    void unlockSkill(Long userId, Integer petIndex, Integer seq);
    void lockSkill(Long userId, Integer petIndex, Integer lockFlag);
    
    // Gem operations (delegates to PetGemService)
    void inlayGem(Long userId, Integer petIndex, Integer slotIndex, Integer gemItemId);
    void gemLevelUpBag(Long userId, Integer itemId, List<Integer> materials);
    void gemLevelUpPet(Long userId, Integer petIndex, Integer slotIndex, List<Integer> materials);
    
    // Combat operations
    void setFightPet(Long userId, Integer petIndex, Integer fightIndex);
    void discardPet(Long userId, Integer petIndex);
    
    // Stats calculation
    Long calculateCapability(Long userId, Integer petIndex);
    void recalculateAllStats(Long userId);
}
```

### 4.3 REST Endpoints

```java
@RestController
@RequestMapping("/api/pet")
public class PetController {
    
    @GetMapping("/{userId}")
    public ResponseEntity<PetAllInfoResponse> getAllInfo(@PathVariable Long userId);
    
    @PostMapping("/{userId}/add")
    public ResponseEntity<Void> addPet(@PathVariable Long userId, @RequestParam Integer petId);
    
    @PostMapping("/{userId}/levelup")
    public ResponseEntity<Void> levelUp(
        @PathVariable Long userId,
        @RequestParam Integer petIndex,
        @RequestParam Integer num
    );
    
    @PostMapping("/{userId}/gradeup")
    public ResponseEntity<Void> gradeUp(
        @PathVariable Long userId,
        @RequestParam Integer petIndex,
        @RequestBody List<Integer> materials
    );
    
    @PostMapping("/{userId}/evolve")
    public ResponseEntity<Void> evolve(@PathVariable Long userId, @RequestParam Integer petIndex);
    
    @PostMapping("/{userId}/fight")
    public ResponseEntity<Void> setFight(
        @PathVariable Long userId,
        @RequestParam Integer petIndex,
        @RequestParam Integer fightIndex
    );
    
    @GetMapping("/{userId}/capability/{petIndex}")
    public ResponseEntity<Long> getCapability(@PathVariable Long userId, @PathVariable Integer petIndex);
}
```

---

## 5. WebSocket Integration

### 5.1 Handler in webSocket-server

**File**: `webSocket-server/src/main/java/com/game/websocket/handler/PetHandler.java`

```java
@Component
public class PetHandler implements MessageHandler {
    
    @Autowired
    private PetClient petClient;  // Feign client to pet-service
    
    @Override
    public boolean supports(int messageType) {
        return messageType >= 2100 && messageType <= 2139;
    }
    
    @Override
    public void handle(WebSocketSession session, int messageType, byte[] payload) {
        Long userId = (Long) session.getAttributes().get("userId");
        
        switch (messageType) {
            case 2100:  // PB_CSRolePetReq
                handlePetRequest(session, userId, payload);
                break;
            case 2105:  // PB_CSPetOneKeyUpLevelGemReq
                handleOneKeyGemUpgrade(session, userId, payload);
                break;
            default:
                log.warn("Unhandled pet message type: {}", messageType);
        }
    }
    
    private void handlePetRequest(WebSocketSession session, Long userId, byte[] payload) {
        try {
            PB_CSRolePetReq req = PB_CSRolePetReq.parseFrom(payload);
            
            PetOperationRequest opReq = new PetOperationRequest();
            opReq.setReqType(req.getReqType());
            opReq.setParam1(req.getParam1());
            opReq.setParam2(req.getParam2());
            opReq.setParam3(req.getParam3());
            opReq.setParamList(req.getParamListList());
            
            // Call pet-service via Feign
            PetOperationResponse response = petClient.handleOperation(userId, opReq);
            
            // Send response back to client
            sendPetResponse(session, response);
            
        } catch (Exception e) {
            log.error("Error handling pet request", e);
            sendError(session, 2104, "Operation failed");
        }
    }
    
    private void sendPetResponse(WebSocketSession session, PetOperationResponse response) {
        PB_SCRolePetRetInfo.Builder builder = PB_SCRolePetRetInfo.newBuilder();
        builder.setRetType(response.getRetType());
        builder.setRetP1(response.getRetP1());
        builder.setRetP2(response.getRetP2());
        
        byte[] data = builder.build().toByteArray();
        sendMessage(session, 2104, data);
    }
}
```

---

## 6. Configuration Management

### 6.1 Config Files Needed (in config-service)

#### pet.json
```json
{
  "pets": [
    {
      "pet_id": 1001,
      "pet_type": 1,
      "name": "Fire Dragon",
      "max_level": 300,
      "skill_grid_max": 6,
      "skill_grid_unlock": [1, 1, -1, -1, -1, -1],
      "base_attr": {
        "hp": 1000,
        "attack": 150,
        "defense": 100
      }
    }
  ],
  "levels": [
    {
      "pet_type": 1,
      "level": 1,
      "up_exp": 100,
      "attr_add": { "hp": 10, "attack": 2 }
    }
  ],
  "advance": [
    {
      "pet_id": 1001,
      "order": 1,
      "need_myself": 1001,
      "need_myself_num": 2,
      "up_order_item_id": 50001,
      "item_id_num": 10
    }
  ],
  "evolution": [
    {
      "pet_id": 1001,
      "pet_up_id": 1002,
      "item_id": 50002,
      "item_id_num": 50
    }
  ],
  "other": {
    "bag_max": 100,
    "pet_evo_level": 200,
    "pet_evo_level_up": 10
  }
}
```

#### pet_gem.json
```json
{
  "gems": [
    {
      "item_id": 60001,
      "level": 1,
      "gem_type": 0,
      "gem_level1": 1,
      "gem_num": 2,
      "up_item_id": 60002,
      "attr": { "attack": 50 }
    }
  ],
  "ts_gems": [
    {
      "level": 1,
      "item_id": 70001,
      "gem_level1": 1,
      "gem_num": 2,
      "ts_gem_level": 1,
      "up_att_num": 2,
      "attr_pool": [
        { "type": 1, "min": 10, "max": 50, "weight": 100 },
        { "type": 2, "min": 5, "max": 30, "weight": 80 }
      ]
    }
  ]
}
```

### 6.2 Feign Client to config-service

```java
@FeignClient(name = "config-service")
public interface ConfigClient {
    
    @GetMapping("/api/config/pet/{petId}")
    PetConfigDTO getPetConfig(@PathVariable Integer petId);
    
    @GetMapping("/api/config/pet/level/{petType}/{level}")
    PetLevelConfigDTO getLevelConfig(@PathVariable Integer petType, @PathVariable Integer level);
    
    @GetMapping("/api/config/pet/advance/{petId}/{order}")
    PetAdvanceConfigDTO getAdvanceConfig(@PathVariable Integer petId, @PathVariable Integer order);
    
    @GetMapping("/api/config/pet/evolution/{petId}")
    PetEvolutionConfigDTO getEvolutionConfig(@PathVariable Integer petId);
}
```

---

## 7. Implementation Checklist

### Phase 1: Setup & Structure
- [ ] Create Maven module `pet-service`
- [ ] Configure pom.xml (dependencies: Spring Boot, JPA, MySQL, Redis, Feign, Protobuf)
- [ ] Create application.yml (port 8520, DB pet_db:3340, Eureka registration)
- [ ] Create Flyway migration V1__init_pet_schema.sql
- [ ] Generate entity classes (Pet, PetTSGem, PetCloth, PetRemains)
- [ ] Create repository interfaces

### Phase 2: Core Service Logic
- [ ] Implement PetService:
  - [ ] getAllPetInfo() - Load all pet data on login
  - [ ] addPet() - Add new pet to collection
  - [ ] levelUp() - Upgrade pet level
  - [ ] gradeUp() - Awaken pet (increase order)
  - [ ] evolve() - Transform pet to new type
  - [ ] calculateCapability() - Combat power calculation
- [ ] Implement PetGemService:
  - [ ] inlayGem() / inlayTSGem()
  - [ ] gemLevelUpBag() / gemLevelUpPet()
  - [ ] tsGemLevelUp() / tsGemRefresh()
  - [ ] oneKeyGemUpgrade() (complex algorithm)
- [ ] Implement PetClothService:
  - [ ] clothUp() - Upgrade clothing
  - [ ] clothWear() - Equip clothing
- [ ] Implement PetRemainsService:
  - [ ] addRemains()
  - [ ] remainsLevelUp()

### Phase 3: REST API
- [ ] Create PetController with all endpoints
- [ ] Add validation (@Valid, @NotNull)
- [ ] Add error handling (ControllerAdvice)
- [ ] Test with Postman/curl

### Phase 4: WebSocket Integration
- [ ] Create PetHandler in webSocket-server
- [ ] Implement message parsing (Protobuf)
- [ ] Create PetClient Feign interface
- [ ] Test bidirectional communication

### Phase 5: Feign Clients
- [ ] ConfigClient - Load pet configs
- [ ] RoleClient - Check role level, sync fight_pet_index
- [ ] BagClient - Consume/add items

### Phase 6: Testing
- [ ] Unit tests (service layer) - 80%+ coverage
- [ ] Integration tests (REST API)
- [ ] Feign contract tests
- [ ] Load test (100 concurrent operations)

### Phase 7: Deployment
- [ ] Create Dockerfile
- [ ] Update docker-compose.yml
- [ ] Configure Eureka registration
- [ ] Add Gateway routes (/pet/\*\*)
- [ ] Deploy and verify

---

## 8. Critical Implementation Notes

### 8.1 Dirty Tracking
- C++ uses dirty sets to minimize DB writes
- Java equivalent: Use `@PreUpdate` in entity + Redis cache
- Only save modified pets/gems to DB on transaction commit

### 8.2 Combat Power Calculation
```java
public Long calculateCapability(Pet pet) {
    // Base stats from pet config
    PetConfigDTO config = configClient.getPetConfig(pet.getPetId());
    PetLevelConfigDTO levelConfig = configClient.getLevelConfig(config.getPetType(), pet.getLevel());
    
    long capability = config.getBaseAttr().getHp() * 0.5 +
                      config.getBaseAttr().getAttack() * 2 +
                      config.getBaseAttr().getDefense() * 1.5;
    
    // Add level bonuses
    capability += levelConfig.getAttrAdd().getHp() * 0.5 * pet.getLevel();
    
    // Add gem bonuses
    for (Integer gemId : pet.getGemItemId()) {
        if (gemId != 0) {
            PetGemConfigDTO gemConfig = configClient.getGemConfig(gemId);
            capability += gemConfig.getAttr().getAttack() * 2;
        }
    }
    
    // Add special gem bonuses
    for (Integer gemIndex : pet.getTsGemIndex()) {
        if (gemIndex != 0) {
            PetTSGem tsGem = tsGemRepository.findByUserIdAndGemIndex(pet.getId().getUserId(), gemIndex);
            capability += calculateTSGemPower(tsGem);
        }
    }
    
    // Add clothing bonuses
    if (pet.getClothId() != 0) {
        PetCloth cloth = clothRepository.findByUserIdAndClothId(pet.getId().getUserId(), pet.getClothId());
        capability += calculateClothPower(cloth);
    }
    
    return capability;
}
```

### 8.3 Pet Level Cap
```java
// Pet level cannot exceed role level
Integer roleLevel = roleClient.getRoleLevel(userId);
if (newLevel > roleLevel) {
    throw new PetLevelExceedRoleLevelException("Pet level cannot exceed role level");
}
```

### 8.4 One-Key Gem Upgrade Algorithm
Complex algorithm from C++ (lines 1513-1805):
1. Parse all gems (in bag + equipped)
2. Group by item_id and level
3. Calculate upgrade path (2 gems → 1 higher level)
4. Prioritize consuming: bag gems → special gems → equipped gems
5. Auto-level up recursively until no more pairs
6. Return items to bag after upgrade

This is the **most complex** operation - allocate extra time for implementation and testing.

### 8.5 Special Gem Attribute Refresh
```java
public void refreshTSGemAttributes(Long userId, Integer gemIndex, Integer lockFlag) {
    PetTSGem gem = tsGemRepository.findByUserIdAndGemIndex(userId, gemIndex);
    
    int lockCount = Integer.bitCount(lockFlag);
    PetTSGemRefreshConfigDTO config = configClient.getTSGemRefreshConfig(lockCount);
    
    // Consume refresh item
    bagClient.consumeItem(userId, config.getUseItemId(), config.getUseItemNum());
    
    // Generate new attributes
    List<Integer> newAttrTypes = new ArrayList<>(4);
    List<Integer> newAttrValues = new ArrayList<>(4);
    generateRandomAttributes(gem.getGemLevel(), newAttrTypes, newAttrValues);
    
    // Keep locked attributes
    for (int i = 0; i < 4; i++) {
        if ((lockFlag & (1 << i)) != 0) {
            newAttrTypes.set(i, gem.getAttrType().get(i));
            newAttrValues.set(i, gem.getAttrValue().get(i));
        }
    }
    
    gem.setAttrType(newAttrTypes);
    gem.setAttrValue(newAttrValues);
    tsGemRepository.save(gem);
}
```

---

## 9. Next Steps

1. **Set up project structure** (Maven module, dependencies)
2. **Create database schema** (Flyway migration)
3. **Implement Pet entity and repository**
4. **Start with simple operations** (getAllPetInfo, addPet, levelUp)
5. **Test incrementally** (after each operation)
6. **Build up to complex operations** (gem system, one-key upgrade)
7. **Integrate WebSocket handler**
8. **Deploy and verify**

---

## 10. Estimated Effort

| Task | Estimated Time |
|------|----------------|
| Setup & DB schema | 2 hours |
| Core pet operations (add, level, grade, evolve) | 4 hours |
| Gem system (normal gems) | 3 hours |
| Special gem system + refresh | 3 hours |
| One-key gem upgrade algorithm | 4 hours |
| Clothing system | 2 hours |
| Remains system | 2 hours |
| REST API + validation | 2 hours |
| WebSocket integration | 2 hours |
| Feign clients | 2 hours |
| Testing (unit + integration) | 4 hours |
| Deployment + verification | 2 hours |
| **Total** | **32 hours** (4 days) |

---

**Status**: Ready for implementation  
**Last Updated**: 2024-01-XX  
**Author**: AI Development Agent
