# Pet Service

## Overview
Pet collection, evolution, and equipment management service for the game server.

**Port**: 8520  
**Database**: pet_db (MySQL port 3340)  
**Message IDs**: 2100-2139

## Features
- Pet collection management (add, level up, evolve, discard)
- Pet awakening/grade up system
- Skill system (learn, unlock, lock slots)
- Normal gem system (4 slots per pet)
- Special gem system (2 slots per pet, random attributes)
- Pet clothing/skin system
- Pet relics/remains system
- Combat capability calculation
- Active fighting pet management

## Architecture

### Entities
- **Pet**: Main pet with level, order, skills, gems, clothing
- **PetTSGem**: Special gems with random attributes
- **PetCloth**: Clothing/skins for pets
- **PetRemains**: Pet relics
- **PetFightIndex**: Active fighting pets (2 slots)

### Operations (20 types)
See `PetOpType` enum for full list:
- LEVEL_UP, GRADE_UP, SKILL_LEARN, SKILL_UNLOCK
- INLAY_GEM, GEM_LEVEL_UP_BAG, GEM_LEVEL_UP_PET
- INLAY_TS_GEM, TS_GEM_LEVEL_UP, TS_GEM_REFRESH
- SET_FIGHT, DISCARD, EVOLVE, etc.

## API Endpoints

### Pet Management
```
GET    /api/pet/{userId}                          # Get all pet info (login)
POST   /api/pet/{userId}/add?petId={petId}        # Add new pet
POST   /api/pet/{userId}/levelup                  # Level up pet
POST   /api/pet/{userId}/gradeup                  # Grade up (awaken)
POST   /api/pet/{userId}/evolve                   # Evolve pet
DELETE /api/pet/{userId}/{petIndex}               # Discard pet
```

### Skills
```
POST   /api/pet/{userId}/skill/learn              # Learn skill
POST   /api/pet/{userId}/skill/unlock             # Unlock slot
POST   /api/pet/{userId}/skill/lock               # Lock slots
```

### Combat
```
POST   /api/pet/{userId}/fight                    # Set fighting pet
GET    /api/pet/{userId}/capability/{petIndex}    # Get combat power
POST   /api/pet/{userId}/recalculate              # Recalculate all stats
```

## Database Schema

### pet_list
- user_id, pet_index (PK)
- pet_id, level, exp, order
- skill_list (JSON), gem_item_id (JSON), ts_gem_index (JSON)
- skill_lock_flag, cloth_id, capability

### pet_ts_gem
- user_id, gem_index (PK)
- gem_level, pet_index
- attr_type (JSON), attr_value (JSON)

### pet_cloth
- user_id, cloth_id (PK)
- level, pet_index

### pet_remains
- user_id, remains_index (PK)
- remains_id, grade, level, exp

### pet_fight_index
- user_id (PK)
- fight_pet_index, fight_pet_index2

## Configuration

### application.yml
```yaml
server:
  port: 8520

spring:
  datasource:
    url: jdbc:mysql://localhost:3340/pet_db
  
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

## Dependencies
- **config-service**: Pet configurations (pet.json, pet_gem.json)
- **role-service**: Role level validation
- **bag-service**: Item consumption/rewards

## Business Rules

### Pet Level
- Pet level cannot exceed role level
- Level up costs gold coins
- Experience required increases with level

### Pet Grade/Awakening
- Requires same-type pets as materials
- Consumes special upgrade items
- Increases base stats and skill slots

### Pet Evolution
- Requires level >= 200
- Changes pet to new type (new pet_id)
- Adds bonus levels (+10)

### Gems
- **Normal Gems**: 4 slots (attack, defense, hp, special)
- **Special Gems**: 2 slots with 4 random attributes
- Upgrade: 2 gems → 1 higher-level gem
- Special gems can refresh attributes with lock system

## Build & Run

### Build
```bash
cd GameServer/pet-service
mvn clean package
```

### Run
```bash
java -jar target/pet-service-1.0.0.jar
```

### Docker
```bash
docker build -t pet-service .
docker run -p 8520:8520 pet-service
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Test
```bash
# Get pet info
curl http://localhost:8520/api/pet/1001

# Add pet
curl -X POST http://localhost:8520/api/pet/1001/add?petId=10001

# Level up
curl -X POST "http://localhost:8520/api/pet/1001/levelup?petIndex=1&num=10"
```

## Monitoring
- Health check: http://localhost:8520/actuator/health
- Metrics: http://localhost:8520/actuator/metrics

## TODO
- [ ] Implement gem service (normal + special)
- [ ] Implement cloth service
- [ ] Implement remains service
- [ ] Add Feign clients (config, role, bag)
- [ ] Implement capability calculation with configs
- [ ] Add WebSocket handler integration
- [ ] Implement one-key gem upgrade algorithm
- [ ] Add comprehensive unit tests
- [ ] Add integration tests
- [ ] Performance optimization with Redis cache

## See Also
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) - Detailed implementation guide
- [PROJECT_PHASES_ANALYSIS.md](../../PROJECT_PHASES_ANALYSIS.md) - Overall project phases
- [AGENT_DEVELOPMENT_GUIDE.md](../../AGENT_DEVELOPMENT_GUIDE.md) - Development workflow
