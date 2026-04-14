<<<<<<< HEAD
# GameServer — Microservices Architecture

**Version**: 1.1.0 | **Java**: 21 | **Spring Boot**: 3.5.3 | **Spring Cloud**: 2025.0.0  
**Services**: 57 | **Phases**: P0 → P5 + Special | **Last Updated**: 2026-03-16

---

## 📚 Documentation

| Tài liệu | Mô tả |
|----------|-------|
| **[SERVICES_SUMMARY.md](SERVICES_SUMMARY.md)** | Tổng quan đầy đủ tất cả 57 services |
| **[common-lib/README.md](common-lib/README.md)** | Shared library (DTOs, gRPC stubs) |
| **[docs/CLIENT_INTEGRATION_GUIDE.md](docs/CLIENT_INTEGRATION_GUIDE.md)** | Quick start cho client developers |
| **[docs/CLIENT_SERVER_CONNECTION.md](docs/CLIENT_SERVER_CONNECTION.md)** | WebSocket + REST API architecture |
| **[docs/migration/README.md](docs/migration/README.md)** | Migration từ C++ sang Java — tổng quan |

---

## 🏗️ Architecture Overview

```
Client (Mobile/Web)
       │
       ▼
[Gateway :8080]  ←  JWT Auth + Route + CORS
       │
       ├── REST ──► All microservices via Eureka LB
       │
       └── WebSocket proxy ──► [webSocket-server :8094]
                                      │  Protobuf binary
                                      ├──► (Feign) role/bag/equip/shop/... 
                                      └──► (gRPC)  role(9410)/bag(9230)/...

[Eureka :8761] ← tất cả services register
[Config :8888] ← centralized configuration
[Kafka]        ← async events (BagChanged, Arena, Combat, v.v.)
[Redis]        ← session cache, leaderboard, scheduler, localization
[MySQL]        ← 42 databases (mỗi service một DB riêng)
```

---

## 📊 Service Summary

| Phase | Services | Mô tả |
|-------|----------|-------|
| P0 — Core Infra | eureka, config, gateway, webSocket-server | Infrastructure bắt buộc |
| P1 — Core Gameplay | user, session, role, wallet, bag, equip, shop, drop, gift, box, crafting, serverInfo, report, iap-verify | Economy & Core |
| P2 — Combat & Social | arena, trial, task, battleserver, globalserver, gameworld, starmap, territory, escort, world, chat, guild | Combat & World |
| P3 — Enhancement | friend, mail, leaderboard, pet, mount, rune, item, angel, artifact, analytics, notification, moderation, file, scheduler, localization | Support |
| P4 — Optional | main-fb, anti-cheat | Advanced features |
| P5 — New Systems | lingzhu, knights, pagoda, scroll, gem, activity, shizhuang | Latest gameplay |
| Special | admin, gm | Administration |

---

## 📌 Ports & Databases

> `*` = Port cấu hình trong `application-local.yml` / `application-prod.yml`

| Service | Port | gRPC | Database |
|---------|------|------|----------|
| **eureka-server** | 8761 | — | — |
| **config-service** | 8888 | — | — |
| **gateway-service** | 8080 | — | — |
| **webSocket-server** | 8094 | — | — (Redis) |
| **user-service** | 8110 | — | user_db |
| **session-service** | 8096 | — | — (Redis) |
| **serverInfo-service** | 8095 | — | serverinfo_db |
| **role-service** | 8410 | 9410 | db_role |
| **wallet-service** | 8210 | — | wallet_db |
| **bag-service** | 8230 | 9230 | bag_db |
| **equip-service** | 8240 | 9240 | equip_db |
| **drop-service** | 8250 | — | — (Stateless) |
| **shop-service** | 8260 | 9260 | shop_db |
| **gift-service** | 8270 | — | — (Stateless) |
| **box-service** | 8290 | — | box_db |
| **crafting-service** | 8280 | 9280 | crafting_db |
| **report-service** | 8098 | — | report_db |
| **iap-verify-service** | 8580 | — | iap_verify_db |
| **arena-service** | 8084 | — | game_arena |
| **trial-service** | 8300 | 9300 | game_trial |
| **task-service** | 8097 | — | game_task |
| **battleserver-service** | 8082 | 9082 | db_battle_service |
| **globalserver-service** | 8100 | — | globalserver_service_db |
| **gameworld-service** | 8105 | 9105 | gameworld_db |
| **starmap-service** | 8092 | 9092 | game_starmap |
| **territory-service** | 8360 | 9086 | game_territory |
| **escort-service** | 8340 | — | game_escort |
| **world-service** | 8370 | — | game_world |
| **chat-service** | 8460 | — | chat_db |
| **guild-service** | 8440 | — | guild_db |
| **friend-service** | 8450 | — | friend_db |
| **mail-service** | 8470 | — | mail_db |
| **leaderboard-service** | 8480 | 9088 | leaderboard_db |
| **pet-service** | 8112 | — | game_pet |
| **mount-service** | 8089 | — | game_mount |
| **rune-service** | 8093 | 9093 | game_rune |
| **item-service** | 8220 | — | — (Stateless) |
| **angel-service** | 8090 | 9090 | game_angel |
| **artifact-service** | 8091 | 9087 | game_artifact |
| **analytics-service** | 8510 | — | game_analytics |
| **notification-service** | 8520 | — | game_notification |
| **moderation-service** | 8570 | — | game_moderation |
| **file-service** | 8540 | — | — (Stateless) |
| **scheduler-service** | 8550 | — | — (Redis db:5) |
| **localization-service** | 8560 | 9560 | — (Redis db:6) |
| **main-fb-service** | 8128 | 9128 | game_mainfb |
| **anti-cheat-service** | 8590 | — | game_anticheat |
| **lingzhu-service** | 8380* | — | lingzhudb |
| **knights-service** | 8310* | — | knightsdb |
| **pagoda-service** | 8320* | — | pagodadb |
| **scroll-service** | 8330* | — | scrolldb |
| **gem-service** | 8381* | — | gemdb |
| **activity-service** | 8382* | — | activitydb |
| **shizhuang-service** | 8350* | — | game_shizhuang |
| **admin-service** | 9091 | — | game_admin |
| **gm-service** | 9093 | — | game_gm |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (Virtual Threads) |
| Framework | Spring Boot 3.5.3 |
| Cloud | Spring Cloud 2025.0.0 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Inter-service REST | OpenFeign |
| Inter-service RPC | gRPC (net.devh:grpc-spring-boot-starter 3.1.0) |
| Message Queue | Apache Kafka |
| Cache | Redis (Lettuce) |
| Database | MySQL 8.x |
| Migration | Flyway |
| Serialization | Protobuf 3.25.5 |
| Resilience | Resilience4j CircuitBreaker |
| Build | Maven 3.6+ |
| Container | Docker / Docker Compose |

---

## 🔧 Yêu Cầu Hệ Thống

- **Java**: 21+
- **Maven**: 3.6+
- **MySQL**: 8.x
- **Redis**: 6.0+
- **Kafka**: 3.x (Zookeeper hoặc KRaft)
- **Docker**: 20.10+ (tuỳ chọn)

---

## 🚀 Thứ Tự Khởi Động

### 1. Infrastructure (bắt buộc trước)
```bash
# 1a. Eureka Server (service discovery)
cd eureka-server && mvn spring-boot:run
# → http://localhost:8761

# 1b. Config Service (configuration management)
cd config-service && mvn spring-boot:run
# → http://localhost:8888/actuator/health

# 1c. Gateway Service (API gateway)
cd gateway-service && mvn spring-boot:run
# → http://localhost:8080/actuator/health
```

### 2. Core Services
```bash
cd user-service    && mvn spring-boot:run  # :8110
cd session-service && mvn spring-boot:run  # :8096
cd role-service    && mvn spring-boot:run  # :8410, gRPC :9410
cd serverInfo-service && mvn spring-boot:run # :8095
```

### 3. Economy Services
```bash
cd wallet-service  && mvn spring-boot:run  # :8210
cd item-service    && mvn spring-boot:run  # :8220
cd bag-service     && mvn spring-boot:run  # :8230, gRPC :9230
cd equip-service   && mvn spring-boot:run  # :8240, gRPC :9240
cd drop-service    && mvn spring-boot:run  # :8250
cd shop-service    && mvn spring-boot:run  # :8260, gRPC :9260
```

### 4. WebSocket Server (sau khi tất cả services sẵn sàng)
```bash
cd webSocket-server && mvn spring-boot:run # :8094
# WebSocket URL: ws://localhost:8080/websocket-server/ws/game?token={jwt}
```

### 5. Business Services (thứ tự tùy ý)
```bash
# Tất cả P2, P3, P4, P5 services
```

### Docker Compose (khuyến nghị cho dev)
```bash
cd docker
docker-compose -f docker-compose.merged.yml up -d
docker-compose -f docker-compose.merged.yml logs -f
```

---

## 🏗️ Build

### Build common-lib trước (bắt buộc)
```bash
cd common-lib
mvn clean install -DskipTests
```

### Build một service
```bash
cd {service-name}
mvn clean install -DskipTests
```

### Build tất cả
```bash
# Dùng script có sẵn
.\build_all.ps1
```

---

## ⚙️ Configuration

### Config Service
Config files tại `config-service/src/main/resources/config/`:
```
config/
├── gameworld/
│   ├── monster/   - monster.json
│   ├── skill/     - skill.json
│   └── drop/      - drop.json
└── serverconfig/  - server settings
```

Truy cập:
```
GET http://localhost:8888/api/config/file/{path}
GET http://localhost:8888/api/config/file/gameworld/monster/monster.json
```

### Gateway Auth Whitelist (không cần JWT)
```
/actuator/**
/session-service/api/session/login
/session-service/api/session/timesync
/config-service/api/config/**
```

### WebSocket Connection
```
ws://localhost:8080/websocket-server/ws/game?token={jwt-token}
```
Protocol: Binary Protobuf — xem [docs/CLIENT_INTEGRATION_GUIDE.md](docs/CLIENT_INTEGRATION_GUIDE.md)

---

## 🔌 Service Dependencies

```
webSocket-server ──► Feign ──► role, bag, shop, equip, wallet, arena, trial,
                               task, mail, friend, guild, chat, leaderboard,
                               pet, mount, rune, angel, artifact, item,
                               escort, starmap, territory, world, crafting,
                               gem, lingzhu, knights, pagoda, scroll,
                               shizhuang, activity, analytics, notification,
                               localization, gm, main-fb

                 ──► gRPC ──► role(9410), bag(9230), equip(9240), shop(9260),
                              crafting(9280), trial(9300), starmap(9092),
                              territory(9086), angel(9090), artifact(9087),
                              rune(9093), leaderboard(9088), gameworld(9105),
                              main-fb(9128), localization(9560),
                              battleserver(9082)

session-service  ──► user-service
shop-service     ──► bag, wallet, item, role, config
bag-service      ──► wallet-service
task-service     ──► bag, wallet, leaderboard
scheduler-service──► gift, guild, leaderboard, shop, task
```

---

## 🐞 Troubleshooting

| Vấn đề | Giải pháp |
|--------|----------|
| Service không register Eureka | Kiểm tra `eureka.client.serviceUrl.defaultZone` và Eureka đang chạy |
| Gateway không route | Verify service đã register Eureka, check CORS config |
| Config không load | Kiểm tra mode classpath/filesystem, verify file path |
| Redis lỗi | `redis-cli ping`, check port 6379, `spring.data.redis.host` |
| gRPC lỗi | Check `grpc.server.port` config và `@GrpcService` annotation |
| Build lỗi | Build `common-lib` trước, check Java 21 và Maven 3.6+ |

---

## 🧪 Testing

```java
// Disable Eureka trong unit tests
@SpringBootTest
@TestPropertySource(properties = {"eureka.client.enabled=false"})
class ServiceTest { ... }
```

```bash
# Test qua Gateway
curl http://localhost:8080/config-service/api/config/file/test.json

# Test trực tiếp service
curl http://localhost:8888/api/config/file/test.json

# Test WebSocket
wscat -c "ws://localhost:8080/websocket-server/ws/game?token={jwt}"
```

---

## 📡 Monitoring

- **Eureka Dashboard**: http://localhost:8761
- **Actuator Health**: http://localhost:{port}/actuator/health
- **Actuator Metrics**: http://localhost:{port}/actuator/metrics

---

## 🔒 Security Checklist (Production)

- [ ] Đổi internal token trong Config Service
- [ ] Cấu hình CORS origins đúng
- [ ] Enable HTTPS/TLS
- [ ] Bật Redis authentication
- [ ] Cập nhật dependencies (kiểm tra CVE)
- [ ] Cấu hình firewall rules
- [ ] Bật distributed tracing (Zipkin/Jaeger)
- [ ] Setup monitoring alerts

---

**Status**: ✅ 57/57 services implemented  
**Last Updated**: 2026-03-16
=======
# GameServer
server game h5 (java) migrate from game Vô hạn Bối Lạp H5 (c++)
>>>>>>> main
