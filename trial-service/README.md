# Trial Service

Trial/Challenge dungeon system microservice for testing player strength.

## Features

- **Stage Progression**: Track progress through multi-stage challenges
- **Daily Attempts**: 3 attempts per day (configurable), auto-reset at midnight
- **Score System**: Track current and best scores
- **Star Rating**: 0-3 stars based on performance
- **Speed Records**: Track completion time for time-based challenges
- **Reward System**: Claim rewards for completed stages (bit flag tracking)
- **Progress Reset**: Reset individual or all trials

## API Endpoints

### Basic Operations
- `GET /api/trial/{roleId}` - Get all trial records
- `GET /api/trial/{roleId}/{trialId}` - Get specific trial record

### Trial Operations
- `POST /api/trial/{roleId}/start/{trialId}` - Start trial (consumes 1 attempt)
- `POST /api/trial/{roleId}/complete/{trialId}` - Complete trial with score/stars
- `POST /api/trial/{roleId}/fail/{trialId}` - Fail current trial attempt
- `POST /api/trial/{roleId}/advance/{trialId}` - Advance to next stage
- `POST /api/trial/{roleId}/reset/{trialId}` - Reset all progress for trial

### Reward Operations
- `POST /api/trial/{roleId}/claim/{trialId}/{stageId}` - Claim stage reward
- `GET /api/trial/{roleId}/{trialId}/claimed` - Get list of claimed stage rewards

### Query Operations
- `GET /api/trial/{roleId}/{trialId}/best` - Get best stage, score and time

## Database

- **trial_record**: Trial progress and statistics

## Configuration

Port: 8094
Database: game_trial

## Game Mechanics

- **Daily Attempts**: 3 per trial per day (resets at midnight)
- **Stars**: 0-3 stars based on performance criteria
- **Best Records**: Track best stage, score, and completion time
- **Rewards**: Per-stage rewards using bit flags (supports up to 64 stages)
- **Auto Reset**: Daily attempts auto-reset on next start if needed

## TODO

- [ ] Feign clients (config-service, bag-service, wallet-service)
- [ ] WebSocket handler integration
- [ ] Config file loading (trial configurations)
- [ ] Unlock requirement validation
- [ ] Entry cost checking
- [ ] Reward distribution implementation
- [ ] Complete business logic formulas
- [ ] Scheduled job for daily reset
- [ ] Unit and integration tests

