# Escort Service

Escort mission system microservice for transporting goods and earning rewards.

## Features

- **Quality Tiers**: 5 quality levels (White, Green, Blue, Purple, Orange)
- **Mission Generation**: Weighted random quality distribution
- **Daily Limits**: 10 missions per day (configurable)
- **Time Limits**: 2-hour mission duration
- **Progress Tracking**: Track distance covered
- **Attack Events**: Random attacks during escort (10% chance)
- **Bonus Multipliers**: Perfect completion bonus (no attacks)
- **Refresh System**: 3 free refreshes per day
- **Statistics Tracking**: Comprehensive stats and achievements

## API Endpoints

### Feign-Compatible Endpoints (WebSocket Server)
- `GET /api/escort/player/{roleId}` - Get all missions (Feign)
- `GET /api/escort/targets/{roleId}` - Get active missions (Feign)
- `GET /api/escort/history/{roleId}` - Get completed missions history (Feign)
- `POST /api/escort/start` - Start escort mission (Feign, body: roleId, escortType)
- `POST /api/escort/complete` - Complete escort mission (Feign, body: roleId, escortId)
- `POST /api/escort/rob` - Rob another player's escort (Feign, body: userId, victimId)
- `POST /api/escort/speedup` - Speed up escort mission (Feign, body: userId, missionId)

### Mission Operations
- `GET /api/escort/{userId}/missions` - Get all missions
- `GET /api/escort/{userId}/missions/{missionId}` - Get specific mission
- `POST /api/escort/{userId}/missions/generate` - Generate new mission
- `POST /api/escort/{userId}/missions/{missionId}/start` - Start mission
- `POST /api/escort/{userId}/missions/{missionId}/progress` - Update progress
- `POST /api/escort/{userId}/missions/{missionId}/complete` - Complete mission
- `POST /api/escort/{userId}/missions/{missionId}/fail` - Fail mission
- `DELETE /api/escort/{userId}/missions/{missionId}` - Cancel mission
- `POST /api/escort/{userId}/missions/{missionId}/claim` - Claim reward

### Mission Management
- `POST /api/escort/{userId}/missions/refresh` - Refresh available missions
- `POST /api/escort/{userId}/missions/check-expired` - Check for expired missions
- `GET /api/escort/{userId}/missions/active` - Get active missions
- `GET /api/escort/{userId}/missions/completed` - Get completed missions
- `GET /api/escort/{userId}/missions/unclaimed` - Get unclaimed rewards

### Statistics Operations
- `GET /api/escort/{userId}/stats` - Get statistics
- `POST /api/escort/{userId}/stats/init` - Initialize statistics
- `POST /api/escort/{userId}/stats/reset-daily` - Reset daily stats

### Validation
- `GET /api/escort/{userId}/can-start` - Check if can start mission
- `GET /api/escort/{userId}/can-refresh` - Check if can refresh
- `GET /api/escort/{userId}/has-active` - Check if has active mission

## Database

- **escort_mission**: Active and historical missions
- **escort_stats**: Player statistics and daily tracking

## Configuration

Port: 8096
Database: game_escort

## Game Mechanics

### Quality Tiers
- **White (1)**: 50% chance, 1000 distance, 100 gold, 50 exp
- **Green (2)**: 30% chance, 2000 distance, 400 gold, 200 exp
- **Blue (3)**: 15% chance, 3000 distance, 900 gold, 450 exp
- **Purple (4)**: 4% chance, 4000 distance, 1600 gold, 800 exp
- **Orange (5)**: 1% chance, 5000 distance, 2500 gold, 1250 exp

### Mission Status
- **0 - Available**: Mission generated, not started
- **1 - In Progress**: Mission started, timer running
- **2 - Completed**: Mission completed, reward available
- **3 - Failed**: Mission failed
- **4 - Expired**: Mission timed out

### Bonus System
- **Quality Multiplier**: 1.0 + (quality × 0.2)
- **Perfect Completion**: 1.5× multiplier (no attacks survived)
- **Final Reward**: base_reward × quality_multiplier × perfect_bonus

### Daily Limits
- **Missions**: 10 per day
- **Free Refreshes**: 3 per day
- **Auto Reset**: Midnight daily

## TODO

- [ ] Feign clients (config-service, wallet-service, role-service)
- [ ] WebSocket handler integration
- [ ] Config file loading (mission configurations)
- [ ] Unlock requirement validation
- [ ] Entry cost checking
- [ ] Reward distribution implementation
- [ ] Paid refresh item support
- [ ] Attack event handling (combat integration)
- [ ] Route/waypoint system
- [ ] Guild convoy system
- [ ] Complete business logic formulas
- [ ] Scheduled job for expired mission cleanup
- [ ] Unit and integration tests

