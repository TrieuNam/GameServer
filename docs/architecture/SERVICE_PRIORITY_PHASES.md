# Service Priority Phases - Game Server Architecture

> Phân loại các services theo độ ưu tiên triển khai (P0 → P4)  
> Cập nhật: 24/01/2026

---

## 🔴 PHASE 0 (P0) - Core Infrastructure Only

**Mục tiêu**: Xây dựng nền tảng hạ tầng cơ bản cho toàn hệ thống (không có database)

| Service | Mục đích | HTTP Port | Database | DB Port | Status |
|---------|----------|-----------|----------|---------|--------|
| **eureka-server** | Service discovery/registry | 8761 | - | - | ✅ |
| **gateway-service** | API Gateway (routing, JWT, rate-limit) | 8080 | - | - | ✅ |
| **config-service** | JSON/XML config, cache Redis | 8888 | - | - | ✅ |
| **webSocket-server** | WebSocket/STOMP realtime | 8094 | - | - | ✅ |

### Infrastructure Components
| Component | Purpose | Port | Status |
|-----------|---------|------|--------|
| **MySQL** | Primary database | 3306 | ✅ |
| **Redis** | Cache/blacklist/locks | 6379 | ✅ |
| **Kafka** (KRaft) | Event streaming backbone | 9092/29092 | ✅ |
| **Prometheus** | Metrics collection | 9090 | ✅ |
| **Grafana** | Dashboard & visualization | 3000 | ✅ |
| **Spring Boot Admin** | Service monitoring | 9091 | ✅ |
| **Kafdrop** | Kafka UI management | 9000 | ✅ |

---

## 🟡 PHASE 1 (P1) - Database Services + Core Gameplay

**Mục tiêu**: Triển khai các services với database và hệ thống kinh tế / vật phẩm

| Service | Mục đích | HTTP Port | Database | DB Port | Status |
|---------|----------|-----------|----------|---------|--------|
| **session-service** | Đăng nhập/refresh/heartbeat | 8096 | - | - | ✅ |
| **user-service** | Tài khoản (profile, authentication) | 8110 | user_db | 3307 | ✅ |
| **report-service** | Báo cáo, xuất CSV | 8098 | report_db | 3309 | ✅ |
| **wallet-service** | Số dư & giao dịch idempotent | 8210 | wallet_db | 3342 | ✅ |
| **item-service** | Metadata vật phẩm, cache Redis | 8220 | - | - | 🔄 |
| **bag-service** | Túi đồ runtime, optimistic lock | 8230 | bag_db | 3311 | ✅ |
| **equip-service** | Trang bị, slot, chỉ số, nâng cấp | 8240 | equip_db | 3312 | ✅ |
| **drop-service** | Bảng rớt/RNG, pool/tỷ lệ | 8250 | drop_db | 3313 | ✅ |
| **shop-service** | Shop (cloth_shop.json, ...) | 8260 | shop_db | 3314 | ✅ |
| **gift-service** | Quà/mã code, phát quà an toàn | 8270 | gift_db | 3315 | ✅ |
| **crafting-service** | Chế tạo/ghép/rèn theo recipe | 8280 | crafting_db | 3316 | 🔄 |
| **box-service** | Mở hộp quà | 8290 | box_db | 3310 | ✅ |

---

## 🟠 PHASE 2 (P2) - Combat & World Domain

**Mục tiêu**: Triển khai hệ thống chiến đấu và thế giới

| Service | Mục đích | HTTP Port | Database | DB Port | Status |
|---------|----------|-----------|----------|---------|--------|
| **arena-service** | Arena/PvP (season, kết quả) | 8370 | arena_db | 3323 | ⌛ |
| **battleserver-service** | Real-time battle logic | 8082 | battle_db | 3317 | ⌛ |
| **world-service** | Tổng hợp/global world state | 8390 | world_db | 3325 | ⌛ |
| **gameworld-service** | Game world management | 8105 | gameworld_db | 3344 | ⌛ |
| **trial-service** | Ải/instance, wave, AI script | 8094 | trial_db | 3320 | ⌛ |
| **territory-service** | Territory control | 8095 | territory_db | 3341 | ⌛ |
| **escort-service** | Escort quests | 8096 | escort_db | 3343 | ⌛ |
| **globalserver-service** | Cross-server coordination | 8600 | globalserver_db | 3317 | ✅ |

---

## 🟢 PHASE 3 (P3) - Enhancement & Progression Domain

**Mục tiêu**: Triển khai hệ thống tiến trình và tăng cường

| Service | Mục đích | HTTP Port | Database | DB Port | Status |
|---------|----------|-----------|----------|---------|--------|
| **role-service** | Nhân vật/level/exp/thuộc tính | 8410 | role_db | 3308 | ✅ |
| **task-service** | Nhiệm vụ + Thành tích | 8420 | task_db | 3326 | ⌛ |
| **pet-service** | Pet management | 8112 | pet_db | 3347 | ⌛ |
| **mount-service** | Mount management | 8089 | mount_db | 3348 | ⌛ |
| **angel-service** | Angel companion system | 8090 | angel_db | 3349 | ⌛ |
| **starmap-service** | Star map exploration | 8092 | starmap_db | 3350 | ⌛ |
| **artifact-service** | Artifact management | 8091 | artifact_db | 3351 | ⌛ |
| **rune-service** | Rune enhancement | 8093 | rune_db | 3352 | ⌛ |
| **shizhuang-service** | Cosmetic items (fashion) | 8099 | shizhuang_db | 3353 | ⌛ |

---

## 🔵 PHASE 4 (P4) - Optional Features

**Mục tiêu**: Các tính năng tùy chọn

| Service | Mục đích | HTTP Port | Database | DB Port | Status |
|---------|----------|-----------|----------|---------|--------|
| **serverInfo-service** | Server status info | 8095 | serverinfo_db | 3354 | ⌛ |
| **main-fb-service** | Facebook integration | 8097 | fb_db | 3355 | ⌛ |

---

## 🟣 SPECIAL SERVICES - Admin & Management

| Service | Mục đích | HTTP Port | Database | DB Port | Status |
|---------|----------|-----------|----------|---------|--------|
| **admin-service** | Admin panel, quản lý hệ thống | 8500 | admin_db | 3345 | 🔄 |
| **gm-service** | GM commands, game master tools | 8505 | gm_db | 3346 | 🔄 |
| **globalserver-service** | Global server coordination | 8600 | globalserver_db | 3317 | ✅ |

---

## 📊 Status Legend

| Symbol | Status | Description |
|--------|--------|-------------|
| ✅ | Completed | Service đã hoàn thành và đang chạy |
| 🔄 | In Progress | Service đang trong quá trình phát triển |
| ⏳ | Planned | Service đã lên kế hoạch, chưa bắt đầu |
| ❌ | Blocked | Service bị chặn, cần giải quyết dependencies |

---

## 🎯 Migration Strategy

### 1. Phase 0 - Foundation First ✅
- Xây dựng hạ tầng microservices
- Service discovery, API gateway, session management
- Monitoring & observability
- **Status**: 100% Complete

### 2. Phase 1 - Economy Next 🔄
- Hệ thống kinh tế và vật phẩm
- Critical cho gameplay cơ bản
- **Status**: 85% Complete

### 3. Phase 2 - Combat Later ⏳
- Hệ thống chiến đấu phức tạp
- Phụ thuộc vào P0 và P1
- **Status**: Not Started

### 4. Phase 3 - Social & Progression ⏳
- Hệ thống xã hội và tiến trình
- Tăng cường engagement
- **Status**: 15% Complete (role-service only)

### 5. Phase 4 - Advanced Features ⏳
- Tính năng nâng cao và hỗ trợ
- Optimization & polish
- **Status**: Not Started

---

## 🔧 Infrastructure Setup

### Local Development
```bash
# Start infrastructure only
cd GameServer/docker
docker-compose -f docker-compose.local.yml up -d

# Start all services (full stack)
docker-compose -f docker-compose.local-full.yml up -d
```

### Management Tools
| Tool | URL | Purpose |
|------|-----|---------|
| Eureka Dashboard | http://localhost:8761 | Service registry |
| Grafana | http://localhost:3000 | Metrics & monitoring |
| Prometheus | http://localhost:9090 | Metrics collection |
| Kafdrop | http://localhost:9000 | Kafka UI |
| Redis Commander | http://localhost:8081 | Redis management |
| **phpMyAdmin** | http://localhost:8082 | MySQL management |
| Spring Boot Admin | http://localhost:9091 | Service health |

---

## 📝 Architecture Principles

1. **One Service, One Database**: Mỗi service có database riêng
2. **Event-Driven**: Sử dụng Kafka cho communication giữa services
3. **API Gateway Pattern**: Tất cả requests đi qua gateway
4. **Service Discovery**: Dynamic service registration với Eureka
5. **Distributed Caching**: Redis cho shared cache và locks
6. **Observability**: Metrics, logs, traces cho tất cả services
7. **Idempotency**: Đảm bảo an toàn cho financial transactions
8. **Health Checks**: Mỗi service phải expose health endpoints

---

## 🚀 Quick Commands

### Build Services
```bash
# Build specific service
cd GameServer/<service-name>
mvn clean package -DskipTests

# Build all services
cd GameServer
mvn clean package -DskipTests
```

### Run Services
```bash
# Run specific service
java -jar target/<service-name>-<version>.jar

# Run with profile
java -jar -Dspring.profiles.active=dev target/<service-name>-<version>.jar
```

### Docker Commands
```bash
# Build Docker image
docker build -t <service-name> .

# Run container
docker run -p <port>:<port> <service-name>

# View logs
docker logs -f <container-name>
```

---

## 📚 Related Documentation

- [P0 Completion Summary](./P0_COMPLETION_SUMMARY.md)
- [Phase 4 Completion Report](./PHASE_4_COMPLETION_REPORT.md)
- [Phase 5 Completion Report](./PHASE_5_COMPLETION_REPORT.md)
- [Service Implementation Status](./SERVICE_IMPLEMENTATION_STATUS.md)
- [Implementation Tracking](./IMPLEMENTATION_TRACKING.md)
- [Production Deployment Guide](./PRODUCTION_DEPLOYMENT_GUIDE.md)

---

**Last Updated**: January 24, 2026  
**Maintained By**: Development Team
