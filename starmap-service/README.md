# Star Map Service

Star Map/Constellation system microservice for celestial power and upgrades.

## Features

- **Star System**: Individual stars that can be activated and leveled up
- **Constellation System**: Groups of stars forming patterns
- **Energy Accumulation**: Collect energy to power stars
- **Completion Tracking**: Track constellation completion status
- **Power Calculation**: Total celestial power from stars and constellations

## API Endpoints

### Star Operations
- `GET /api/starmap/{userId}/stars` - Get all stars
- `GET /api/starmap/{userId}/stars/{starId}` - Get specific star
- `POST /api/starmap/{userId}/stars/{starId}/activate` - Activate/light star
- `POST /api/starmap/{userId}/stars/{starId}/levelup` - Level up star
- `POST /api/starmap/{userId}/stars/{starId}/energy` - Add star energy

### Constellation Operations
- `GET /api/starmap/{userId}/constellations` - Get all constellations
- `GET /api/starmap/{userId}/constellations/{constellationId}` - Get specific constellation
- `POST /api/starmap/{userId}/constellations/{constellationId}/unlock` - Unlock constellation
- `POST /api/starmap/{userId}/constellations/{constellationId}/levelup` - Level up constellation
- `POST /api/starmap/{userId}/constellations/{constellationId}/check` - Check completion status

### Power Calculations
- `GET /api/starmap/{userId}/power` - Get total star map power
- `GET /api/starmap/{userId}/constellations/{constellationId}/power` - Get constellation power

## Database

- **star**: Individual stars (level, active status, energy)
- **constellation**: Constellation groups (completion, unlocked status)

## Configuration

Port: 8365
gRPC Port: 9365 (override with GRPC_SERVER_PORT)
Database: game_starmap

## Protocol Messages

MsgIDs: 2150-2159

## Game Mechanics

- **Stars**: Max level 50, can be activated and powered with energy
- **Constellations**: Max level 20, contain 12 stars (configurable)
- **Completion**: Constellation completed when all stars are activated
- **Power Bonuses**: Completion provides significant power boost

## TODO

- [ ] Feign clients (config-service, role-service, bag-service)
- [ ] WebSocket handler integration
- [ ] Config file loading (starmap config)
- [ ] Star-to-constellation mapping logic
- [ ] Complete business logic formulas
- [ ] Unit and integration tests


