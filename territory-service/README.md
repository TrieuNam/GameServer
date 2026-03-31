# Territory Service

Territory/Base management system microservice for land control and resource production.

## Features

- **Territory Ownership**: Each player has one territory/base
- **Territory Levels**: Upgrade territory to unlock more building slots and storage
- **Building System**: Construct and upgrade buildings in slots
- **Resource Production**: Auto-generate gold and resources over time
- **Construction Queue**: Buildings take time to construct/upgrade
- **Instant Finish**: Speed up construction with special items
- **Defense & Attack**: Buildings contribute to territory combat ratings
- **Prosperity**: Development score from buildings
- **Customization**: Rename territory and change appearance

## API Endpoints

### Territory Operations
- `GET /api/territory/{userId}` - Get territory info
- `POST /api/territory/{userId}/create` - Create new territory
- `POST /api/territory/{userId}/levelup` - Level up territory
- `PUT /api/territory/{userId}/rename` - Rename territory
- `PUT /api/territory/{userId}/appearance` - Change appearance/skin
- `POST /api/territory/{userId}/collect` - Collect accumulated resources
- `POST /api/territory/{userId}/update-production` - Update production calculations

### Building Operations
- `GET /api/territory/{userId}/buildings` - Get all buildings
- `GET /api/territory/{userId}/buildings/{slotId}` - Get specific building
- `POST /api/territory/{userId}/buildings/{slotId}/construct` - Construct building
- `POST /api/territory/{userId}/buildings/{slotId}/upgrade` - Upgrade building
- `POST /api/territory/{userId}/buildings/{slotId}/finish` - Finish construction (when time elapsed)
- `POST /api/territory/{userId}/buildings/{slotId}/instant-finish` - Instant finish construction
- `DELETE /api/territory/{userId}/buildings/{slotId}` - Demolish building

### Query Operations
- `GET /api/territory/{userId}/buildings/completed` - Get completed constructions
- `GET /api/territory/{userId}/stats/defense` - Get total defense rating
- `GET /api/territory/{userId}/stats/attack` - Get total attack rating
- `GET /api/territory/{userId}/stats/prosperity` - Get total prosperity

### Validation
- `GET /api/territory/{userId}/can-levelup` - Check if can level up territory
- `GET /api/territory/{userId}/buildings/{slotId}/can-construct` - Check if can construct
- `GET /api/territory/{userId}/buildings/{slotId}/can-upgrade` - Check if can upgrade

## Database

- **territory**: Main territory/base (one per user)
- **territory_building**: Buildings constructed in territory slots

## Configuration

Port: 8095
Database: game_territory

## Game Mechanics

- **Territory Level**: Max 50, unlocks more slots and storage
- **Building Slots**: Start with 10, gain 1 per territory level
- **Building Status**: 0-Empty, 1-Constructing, 2-Built, 3-Upgrading
- **Production**: Gold and resources accumulate hourly based on production rate
- **Storage Caps**: Max storage prevents overflow
- **Construction Time**: 2 hours for new buildings, 1 hour for upgrades (configurable)
- **Building Contributions**: Each building adds to defense/attack/prosperity/production

## TODO

- [ ] Feign clients (config-service, bag-service, wallet-service)
- [ ] WebSocket handler integration
- [ ] Config file loading (territory/building configurations)
- [ ] Material requirement validation
- [ ] Resource distribution on collection
- [ ] Complete business logic formulas
- [ ] Scheduled job for production updates
- [ ] Territory wars/PvP system
- [ ] Unit and integration tests

