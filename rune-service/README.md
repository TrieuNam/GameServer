# Rune Service

**Version: 2.0.0 (Phase 1-3 Complete)** ✅

Rune enhancement system microservice for equipment power boost with config integration and power calculations.

## Features

- **Rune Creation**: Generate runes with random attributes based on quality
- **Quality System**: 5 quality tiers (White, Green, Blue, Purple, Orange)
- **Multiple Upgrade Paths**: Level, Quality, Star, Refinement
- **Attribute System**: 1 main attribute + up to 3 sub attributes (based on quality)
- **Equipment System**: Equip runes to gear slots with power sync
- **Attribute Refresh**: Re-roll sub attributes
- **Config Integration**: Redis-first caching with config-service loading
- **Power Calculation**: Comprehensive config-driven formula
- **Role Capability Sync**: Automatic player power updates on equip/unequip

## API Endpoints

### Basic Operations
- `GET /api/rune/{userId}` - Get all runes
- `GET /api/rune/{userId}/{runeIndex}` - Get specific rune
- `POST /api/rune/{userId}/create` - Create new rune
- `DELETE /api/rune/{userId}/{runeIndex}` - Delete rune (must be unequipped)

### Upgrade Operations
- `POST /api/rune/{userId}/{runeIndex}/levelup` - Level up rune
- `POST /api/rune/{userId}/{runeIndex}/quality` - Upgrade quality tier
- `POST /api/rune/{userId}/{runeIndex}/star` - Upgrade star rating
- `POST /api/rune/{userId}/{runeIndex}/refine` - Refinement (å¼ºåŒ–)
- `POST /api/rune/{userId}/{runeIndex}/exp` - Add experience

### Equipment Operations
- `POST /api/rune/{userId}/{runeIndex}/equip` - Equip rune to slot
- `DELETE /api/rune/{userId}/{runeIndex}/equip` - Unequip rune
- `DELETE /api/rune/{userId}/equipslot/{equipSlot}` - Unequip from specific slot
- `GET /api/rune/{userId}/equipped` - Get all equipped runes

### Attribute Operations
- `POST /api/rune/{userId}/{runeIndex}/refresh` - Refresh sub attributes

### Power & Validation
- `GET /api/rune/{userId}/{runeIndex}/power` - Calculate rune power
- `GET /api/rune/{userId}/power` - Calculate total equipped runes power
- `GET /api/rune/{userId}/{runeIndex}/canlevelup` - Check if can level up
- `GET /api/rune/{userId}/{runeIndex}/canupgradequality` - Check if can upgrade quality
- `GET /api/rune/{userId}/{runeIndex}/canupgradestar` - Check if can upgrade star

## Database

- **rune**: Rune items with attributes and equipment status

## Configuration

Port: 8093
Database: game_rune
Config File: `gameworld/rune/rune.json`

### Rune Config Structure (rune.json)
```json
{
  "runeList": [
    {
      "runeId": 1,
      "name": "Basic Rune",
      "description": "A basic enhancement rune",
      "baseAttributes": {
        "hp": 100,
        "attack": 20,
        "defense": 15,
        "speed": 5,
        "critRate": 2,
        "critDamage": 5
      },
      "levelCost": {
        "goldPerLevel": 1000,
        "materialItemId": 2001,
        "materialPerLevel": 5
      },
      "qualityCost": {
        "gold": 10000,
        "materialItemId": 2002,
        "materialCount": 10
      },
      "starCost": {
        "goldPerStar": 5000,
        "materialItemId": 2003,
        "materialPerStar": 3
      },
      "refinementCost": {
        "goldPerLevel": 2000,
        "materialItemId": 2004,
        "materialPerLevel": 2
      }
    }
  ]
}
```

### Redis Configuration
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 0

rune:
  config:
    path: gameworld/rune/rune.json
    redis-enabled: true
    redis-ttl-hours: 24
    refresh-interval-ms: 3600000  # 1 hour
```

## Protocol Messages

MsgIDs: 1670-1672
C++ Source: gameworld/other/rune/
Config: gameworld/rune/rune.json

## Game Mechanics

- **Level**: Max 100, increases main attribute
- **Quality**: 1-5 (White/Green/Blue/Purple/Orange), determines sub attribute count
  - Quality Multiplier: `1.0 + (quality - 1) × 0.25` (25% per tier)
- **Star**: Max 10, overall power multiplier
  - Star Multiplier: `1.0 + (star - 1) × 0.15` (15% per star)
- **Refinement**: Max 20, boosts all attributes (强化等级)
  - Refinement Bonus: `refinementLevel × 0.02` (2% per level, additive)
- **Sub Attributes**: 0-3 based on quality (White=0, Green=1, Blue=2, Purple/Orange=3)
- **Equipment Slots**: Multiple slots for equipping runes on gear

## Power Calculation

The rune power formula balances multiple progression systems:

### Formula
```
Total Power = (Main Attribute + Sub Attributes) × Quality Multiplier × Star Multiplier × (1 + Refinement Bonus)
```

### Attribute Weights
- **HP**: 1x
- **Attack**: 5x
- **Defense**: 3x
- **Speed**: 2x
- **Crit Rate**: 10x
- **Crit Damage**: 8x

### Example Calculation
Purple Rune (Quality 4), 5 Stars, Refinement 10:
- Main Attribute: 500 ATK → 500 × 5 = 2,500
- Sub Attributes: 100 HP + 50 DEF + 10 SPD → (100×1 + 50×3 + 10×2) = 320
- Base Power: 2,500 + 320 = 2,820
- Quality Multiplier: 1.0 + (4-1) × 0.25 = 1.75
- Star Multiplier: 1.0 + (5-1) × 0.15 = 1.60
- Refinement Bonus: 10 × 0.02 = 0.20 (20%)
- **Total Power**: 2,820 × 1.75 × 1.60 × 1.20 = **9,475**

## Implementation Phases

### Phase 1: Config Integration ✅
- [x] **ConfigServiceClient**: Feign client for loading `rune.json`
  - ETag-based conditional GET for bandwidth optimization
  - Fallback handling for service unavailability
- [x] **RuneConfigProvider**: Redis-first caching strategy
  - Pattern: Redis → config-service → fallback defaults
  - `@PostConstruct` warmup on startup
  - `@Scheduled` refresh every 1 hour (configurable)
  - Redis key: `cfg:file:gameworld:rune:rune.json`
  - TTL: 24 hours (configurable)
- [x] **RuneConfig POJO**: Jackson-mapped configuration model
  - RuneItem, Attributes, LevelCost, QualityCost, StarCost, RefinementCost
- [x] **RoleServiceClient**: Feign client for player capability sync
  - Updates player combat power on equip/unequip
- [x] **Redis Configuration**: Spring Data Redis with connection pooling
- [x] **Sample Config**: rune.json with 4 example rune types

### Phase 2: Enhanced Business Logic ✅
- [x] **RunePowerCalculator**: Comprehensive power calculation utility
  - Config-driven formula with attribute weights
  - Quality, star, and refinement multipliers
  - Handles main + sub attributes with proper weighting
- [x] **Service Integration**: Enhanced RuneServiceImpl
  - Power calculation on equip/unequip
  - Automatic capability sync with role-service
  - Graceful error handling for inter-service calls
- [x] **Attribute Formula**: Weighted power system
  - Crit stats (10x/8x) > Attack (5x) > Defense (3x) > Speed (2x) > HP (1x)

### Phase 3: Documentation & Validation ✅
- [x] **README Update**: Complete Phase 1-3 documentation
  - Configuration examples
  - Power calculation formula with examples
  - Game mechanics with multiplier formulas
- [ ] **Enhanced Validation**: Extended error handling
- [ ] **Attribute Refresh Enhancement**: Advanced sub-attribute re-roll system

## Architecture

### Service Dependencies
```
rune-service
├── config-service (Feign) - Load rune.json configuration
├── role-service (Feign) - Sync player capability on equip/unequip
├── bag-service (Feign) - Validate materials for upgrades
├── wallet-service (Feign) - Deduct gold costs
└── Redis - Cache configuration files
```

### Configuration Loading Strategy
1. **Redis Cache** (fastest): Check `cfg:file:gameworld:rune:rune.json`
2. **Config-Service** (fallback): HTTP GET with ETag validation
3. **Graceful Degradation**: Log warning, return null (caller uses defaults)

### Power Sync Flow
```
equipRune()
  ├── Validate rune ownership & slot availability
  ├── Update rune.isEquipped = true
  ├── Calculate total rune power (RunePowerCalculator)
  └── Sync to role-service via RoleServiceClient
       └── POST /api/role/{roleId}/capability/update
           {source: "rune", deltaValue: +9475, reason: "rune_equip"}

unequipRune()
  ├── Validate rune is equipped
  ├── Calculate power delta (negative)
  ├── Update rune.isEquipped = false
  └── Sync to role-service (remove power)
       └── POST /api/role/{roleId}/capability/update
           {source: "rune", deltaValue: -9475, reason: "rune_unequip"}
```

## TODO

- [ ] Bag-service integration for material validation
- [ ] Wallet-service integration for gold costs
- [ ] WebSocket handler integration
- [ ] Auto level-up when exp threshold reached
- [ ] Enhanced attribute refresh with config-driven probabilities
- [ ] Unit and integration tests


