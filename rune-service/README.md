# Rune Service

Rune enhancement system microservice for equipment power boost.

## Features

- **Rune Creation**: Generate runes with random attributes based on quality
- **Quality System**: 5 quality tiers (White, Green, Blue, Purple, Orange)
- **Multiple Upgrade Paths**: Level, Quality, Star, Refinement
- **Attribute System**: 1 main attribute + up to 3 sub attributes (based on quality)
- **Equipment System**: Equip runes to gear slots
- **Attribute Refresh**: Re-roll sub attributes

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

## Protocol Messages

MsgIDs: 1670-1672
C++ Source: gameworld/other/rune/
Config: rune.json

## Game Mechanics

- **Level**: Max 100, increases main attribute
- **Quality**: 1-5 (White/Green/Blue/Purple/Orange), determines sub attribute count
- **Star**: Max 10, overall power multiplier
- **Refinement**: Max 20, boosts all attributes (å¼ºåŒ–ç­‰çº§)
- **Sub Attributes**: 0-3 based on quality (White=0, Green=1, Blue=2, Purple/Orange=3)
- **Equipment Slots**: Multiple slots for equipping runes on gear

## TODO

- [ ] Feign clients (config-service, bag-service, wallet-service)
- [ ] WebSocket handler integration
- [ ] Config file loading (rune.json)
- [ ] Material requirement validation
- [ ] Complete business logic formulas
- [ ] Auto level-up when exp threshold reached
- [ ] Unit and integration tests


