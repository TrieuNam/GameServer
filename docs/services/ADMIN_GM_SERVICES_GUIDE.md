# GameServer - Admin & GM Services Setup Guide

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    ADMIN SERVICE (9091)                  │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Service Management & Monitoring                  │  │
│  │  - Start/Stop/Restart 37 microservices          │  │
│  │  - Health checks & status tracking              │  │
│  │  - Docker container management                   │  │
│  │  - Spring Boot Admin UI                         │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                     GM SERVICE (9092)                    │
│  ┌───────────────────────────────────────────────────┐  │
│  │  Game Master Tool                                 │  │
│  │  - Give/Remove items to players                  │  │
│  │  - Add/Deduct currency (gold, diamond)          │  │
│  │  - Update VIP levels                            │  │
│  │  - Ban/Unban users                              │  │
│  │  - Broadcast system messages                    │  │
│  │  - Audit logging for all GM actions            │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    DATABASE LAYER                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   admindb    │  │    gmdb      │  │  11 other    │  │
│  │ (port 3306)  │  │ (port 3328)  │  │   databases  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Start Infrastructure (Docker)

```powershell
cd GameServer\docker
docker-compose -f docker-compose.local-full.yml up -d
```

**Containers started:**
- ✅ 13 MySQL databases (including admindb, gmdb)
- ✅ Redis (port 6379)
- ✅ Kafka (port 9092, 29092)
- ✅ phpMyAdmin (http://localhost:8082)
- ✅ Redis Commander (http://localhost:8081)
- ✅ Kafka UI (http://localhost:8090)

### 2. Start Admin Service

```powershell
cd GameServer\admin-service
.\start-admin-db.ps1
```

**Access:**
- Spring Boot Admin UI: http://localhost:9091/admin
- Control Panel API: http://localhost:9091/api/services
- Credentials: `admin / admin123`

### 3. Start GM Service

```powershell
cd GameServer\gm-service
.\start-gm-docker.ps1
```

**Access:**
- API Base URL: http://localhost:9092/api/gm
- Credentials: `gm / gm123`

## Admin Service Features

### 1. Service Control via UI
- Access Spring Boot Admin: http://localhost:9091/admin
- View all registered services
- Monitor health, metrics, logs

### 2. Service Control via REST API

```bash
# Get all services
curl -u admin:admin123 http://localhost:9091/api/services

# Start a service
curl -u admin:admin123 -X POST http://localhost:9091/api/services/user-service/start

# Stop a service
curl -u admin:admin123 -X POST http://localhost:9091/api/services/user-service/stop

# Restart a service
curl -u admin:admin123 -X POST http://localhost:9091/api/services/user-service/restart

# Start all services
curl -u admin:admin123 -X POST http://localhost:9091/api/services/start-all

# Get service logs
curl -u admin:admin123 http://localhost:9091/api/services/user-service/logs
```

### 3. Service Management Database

Admin service tracks 37 microservices in `game_admin.service_config` table:
- Service name, port, phase
- Startup order, auto-start settings
- Process ID, status
- Docker container associations
- Health check URLs

## GM Service Features

### 1. Item Management

```bash
# Give items to player
curl -u gm:gm123 -X POST http://localhost:9092/api/gm/item/give \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": "player123",
    "itemId": 1001,
    "quantity": 10,
    "reason": "Compensation"
  }'

# Remove items
curl -u gm:gm123 -X POST http://localhost:9092/api/gm/item/remove \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": "player123",
    "itemId": 1001,
    "quantity": 5,
    "reason": "Correction"
  }'
```

### 2. Currency Management

```bash
# Add currency
curl -u gm:gm123 -X POST http://localhost:9092/api/gm/currency/add \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": "player123",
    "currencyType": "GOLD",
    "amount": 100000,
    "reason": "Event reward"
  }'

# Deduct currency
curl -u gm:gm123 -X POST http://localhost:9092/api/gm/currency/deduct \
  -H "Content-Type: application/json" \
  -d '{
    "playerId": "player123",
    "currencyType": "DIAMOND",
    "amount": 50,
    "reason": "Adjustment"
  }'
```

### 3. VIP Management

```bash
# Update VIP level
curl -u gm:gm123 -X POST http://localhost:9092/api/gm/vip/update \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "vipLevel": 10,
    "reason": "Special promotion"
  }'
```

### 4. User Ban/Unban

```bash
# Ban user (7 days)
curl -u gm:gm123 -X POST \
  "http://localhost:9092/api/gm/user/1/ban?reason=Cheating&durationDays=7"

# Permanent ban
curl -u gm:gm123 -X POST \
  "http://localhost:9092/api/gm/user/2/ban?reason=Severe violation"

# Unban user
curl -u gm:gm123 -X POST \
  "http://localhost:9092/api/gm/user/1/unban?reason=Appeal approved"
```

### 5. System Broadcast

```bash
# Broadcast message
curl -u gm:gm123 -X POST http://localhost:9092/api/gm/broadcast \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Server maintenance in 10 minutes",
    "type": "MAINTENANCE",
    "durationSeconds": 60
  }'
```

### 6. Audit Logs

```bash
# View recent logs
curl -u gm:gm123 http://localhost:9092/api/gm/logs/recent

# View logs by GM ID
curl -u gm:gm123 http://localhost:9092/api/gm/logs/gm/1?page=0&size=20

# View logs by player ID
curl -u gm:gm123 http://localhost:9092/api/gm/logs/player/player123?page=0&size=20
```

## Database Ports

| Database | Container | Port | DB Name | User | Password |
|----------|-----------|------|---------|------|----------|
| Admin | local-admindb | 3306 | game_admin | root | 1234 |
| GM | local-gmdb | 3328 | game_gm | root | 1234 |
| User | local-userdb | 3307 | user_db | tpnam | 121831 |
| Report | local-reportdb | 3309 | report_game_h2 | tpnam | 121831 |
| Box | local-boxdb | 3310 | box_db | tpnam | 121831 |
| Bag | local-bagdb | 3311 | db_bag | tpnam | 121831 |
| Equip | local-equipdb | 3312 | equip_db | tpnam | 121831 |
| Shop | local-shopdb | 3314 | shop_db | tpnam | 121831 |
| Crafting | local-craftingdb | 3316 | crafting_db | tpnam | 121831 |
| ServerInfo | local-serverinfodb | 3318 | serverinfo_db | tpnam | 121831 |
| Role | local-roledb | 3319 | db_role | tpnam | 121831 |
| Arena | local-arenadb | 3327 | game_arena | tpnam | 121831 |
| Wallet | local-walletdb | 3342 | wallet_db | tpnam | 121831 |

## Management URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| Admin Service | http://localhost:9091/admin | admin / admin123 |
| GM Service API | http://localhost:9092/api/gm | gm / gm123 |
| phpMyAdmin | http://localhost:8082 | - |
| Redis Commander | http://localhost:8081 | - |
| Kafka UI | http://localhost:8090 | - |

## Troubleshooting

### Admin Service won't start
```powershell
# Check if MySQL is running
docker ps | Select-String admindb

# Restart database
docker restart local-admindb

# Check logs
docker logs local-admindb
```

### GM Service can't connect to database
```powershell
# Verify database is running
docker exec local-gmdb mysqladmin ping -uroot -p1234

# Check port is not in use
netstat -ano | Select-String 3328

# Restart GM database
docker restart local-gmdb
```

### Service shows as DOWN in Admin UI
1. Check if Eureka Server is running
2. Verify service registered with Eureka
3. Check health endpoint is accessible
4. Review service logs

## Development

### Build Admin Service
```powershell
cd GameServer\admin-service
mvn clean package -DskipTests
```

### Build GM Service
```powershell
cd GameServer\gm-service
mvn clean package -DskipTests
```

### Rebuild All
```powershell
cd GameServer
mvn clean install -DskipTests
```

## Architecture Details

### Admin Service Dependencies
- ✅ MySQL (admindb)
- ❌ Eureka (removed - standalone)
- ❌ Redis (removed - not critical)
- ❌ Feign Clients (removed - only service management)

### GM Service Dependencies
- ✅ MySQL (gmdb)
- ✅ Eureka (service discovery)
- ✅ Redis (caching)
- ✅ Feign Clients (bag, wallet, role, user services)

### Service Communication
```
GM Service → Feign Clients → Eureka → Target Services
           ↓
       MySQL (audit logs)
```

## Security

### Admin Service
- Basic Authentication (admin/admin123)
- CSRF disabled (API only)
- All endpoints require authentication except `/actuator/**`

### GM Service
- Basic Authentication (gm/gm123)
- CSRF disabled (API only)
- All GM actions logged with:
  - GM ID and username
  - Target player/user ID
  - Action details
  - IP address
  - Timestamp
  - Success/failure status

## Next Steps

1. ✅ Start Docker infrastructure
2. ✅ Start Admin Service
3. ✅ Start GM Service
4. ✅ Test GM operations
5. ⏳ Start other 37 microservices via Admin UI
6. ⏳ Implement WebSocket for real-time broadcasts
7. ⏳ Add GM web dashboard UI
