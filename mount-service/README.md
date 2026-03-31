# Mount Service

Mount/Cavalry system microservice for the game server.

## Features

- **Mount Management**: Unlock, level up, grade up mounts
- **Mount Equipment**: Ride/unequip mounts
- **Cosmetics**: Appearance and skin system
- **Exploration**: Mount exploration for rewards
- **Harness System**: Equipment with random attributes
- **Harness Operations**: Wear, remove, decompose, refresh entries

## API Endpoints

### Mount Operations (roleId)
- `GET /api/mount/{roleId}` - Get all mounts
- `POST /api/mount/{roleId}/levelup` - Level up mount
- `POST /api/mount/{roleId}/gradeup/{mountId}` - Grade up mount
- `POST /api/mount/{roleId}/appearance` - Set appearance
- `POST /api/mount/{roleId}/explore` - Mount exploration for rewards
- `POST /api/mount/{roleId}/pifu/set` - Set skin
- `POST /api/mount/{roleId}/pifu/upgrade` - Upgrade skin

### Mount Harness (roleId)
- `POST /api/mount/{roleId}/harness/wear` - Wear harness
- `POST /api/mount/{roleId}/harness/decompose` - Decompose harness
- `POST /api/mount/{roleId}/harness/unlock` - Unlock harness

### Mount Shop (roleId)
- `POST /api/mount/{roleId}/shop/open` - Open shop
- `POST /api/mount/{roleId}/shop/buy` - Buy from shop
- `POST /api/mount/{roleId}/shop/refresh` - Refresh shop
- `POST /api/mount/{roleId}/shop/refreshbuy` - Refresh and buy

### Harness Controller (userId)
- `GET /api/mount/harness/{userId}` - Get all harness
- `GET /api/mount/harness/{userId}/{harnessIndex}` - Get specific harness
- `GET /api/mount/harness/{userId}/{harnessIndex}/bonus` - Get harness bonus
- `GET /api/mount/harness/{userId}/hasspace` - Check harness space
- `POST /api/mount/harness/{userId}/add` - Add harness
- `POST /api/mount/harness/{userId}/wear` - Equip harness
- `DELETE /api/mount/harness/{userId}/wear` - Remove harness
- `DELETE /api/mount/harness/{userId}/{harnessIndex}/decompose` - Decompose harness
- `POST /api/mount/harness/{userId}/{harnessIndex}/refresh` - Refresh attributes
- `POST /api/mount/harness/{userId}/buy` - Buy harness
- `PUT /api/mount/harness/{userId}/{harnessIndex}/lock` - Set lock flag

## Database

- **mount**: Mount/cavalry data (level, grade, equipment slots)
- **mount_harness**: Harness items in bag (attributes, lock flags)

## Configuration

Port: 8089
Database: game_mount

## Protocol Messages

MsgIDs: 2140-2149
- 2140: Mount request
- 2141: Mount info
- 2142: Mount operation result
- 2143: Harness list info
- 2144: Single harness info
- 2145: Mount harness info

## Operations

- LEVEL_UP, GRADE_UP
- EXPLORE
- SET_APP (appearance), PIFU_UP (skin upgrade), SET_PIFU
- WEAR (harness), DECOMPOSE
- UNLOCK, ENTRY_REFRESH
- BUY, REFRESH_BUY, OPEN_BUY
- SET_LOCK_FLAG

## TODO

- [ ] Feign clients (config-service, role-service, bag-service, wallet-service)
- [ ] WebSocket handler integration (MountHandler)
- [ ] Config file loading (harness.json)
- [ ] Complete business logic formulas
- [ ] Unit and integration tests

