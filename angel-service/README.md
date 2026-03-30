# Angel Service

Angel/Wing companion system microservice for character enhancement and appearance.

## Features

- **Angel Management**: Unlock, level up, grade up angels
- **Equipment**: Equip/display angels
- **Skills**: 4 skill slots with upgrade system
- **Star Upgrade**: Additional power tier
- **Evolution**: Breakthrough stages
- **Blessing System**: Accumulate blessing points for bonuses
- **Appearance**: Customizable skins and appearances

## API Endpoints

### Angel Operations
- `GET /api/angel/{roleId}` - Get all angel data
- `POST /api/angel/{roleId}/levelup` - Level up angel
- `POST /api/angel/{roleId}/gradeup/{angelId}` - Grade up angel
- `POST /api/angel/{roleId}/activate/{angelId}` - Activate/equip angel
- `POST /api/angel/{roleId}/switch/{angelId}` - Switch active angel
- `POST /api/angel/{roleId}/skill/upgrade` - Upgrade skill
- `POST /api/angel/{roleId}/appearance-upgrade` - Upgrade appearance
- `POST /api/angel/{roleId}/blessing/{angelId}` - Add blessing points
- `POST /api/angel/{roleId}/transform` - Transform angel
- `POST /api/angel/{roleId}/rename` - Rename angel

## Database Schema

### angel
```sql
CREATE TABLE angel (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    angel_index     INT NOT NULL,
    angel_id        INT NOT NULL,
    level           INT NOT NULL DEFAULT 0,
    grade           INT NOT NULL DEFAULT 0,
    exp             BIGINT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    is_equipped     BOOLEAN NOT NULL DEFAULT FALSE,
    star_level      INT NOT NULL DEFAULT 0,
    skill1_id       INT,
    skill1_level    INT NOT NULL DEFAULT 0,
    skill2_id       INT,
    skill2_level    INT NOT NULL DEFAULT 0,
    skill3_id       INT,
    skill3_level    INT NOT NULL DEFAULT 0,
    skill4_id       INT,
    skill4_level    INT NOT NULL DEFAULT 0,
    appearance_id   INT,
    appearance_level INT NOT NULL DEFAULT 0,
    evolution_stage INT NOT NULL DEFAULT 0,
    blessing_points BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(32),
    created_at      DATETIME NOT NULL,
    updated_at      DATETIME NOT NULL,
    UNIQUE KEY uk_user_angel (user_id, angel_index),
    INDEX idx_user_id (user_id)
);
```

## Configuration

Port: 8090
Database: game_angel

## Protocol Messages

MsgIDs: 2130-2132
- 2130: Angel request (CSAngelReq)
- 2131: Angel info (SCAngelInfo)
- 2132: Angel operation result (SCAngelOpRet)

## Game Mechanics

- **Level System**: Max level 100, affects base stats
- **Grade System**: Max grade 10, quality tier with stat multiplier
- **Star System**: Max 12 stars, additional upgrade tier
- **Evolution**: Max 5 stages, breakthrough for major power boost
- **Skills**: 4 skill slots (3 active + 1 passive), max level 10 each
- **Blessing**: Accumulate points for continuous bonuses

## TODO

- [ ] Feign clients (config-service, role-service, bag-service, wallet-service)
- [ ] WebSocket handler integration (AngelHandler)
- [ ] Config file loading (angel.json)
- [ ] Complete business logic formulas
- [ ] Skill system implementation
- [ ] Unit and integration tests

