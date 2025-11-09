# Game Server - Infrastructure Services

## Tổng quan hệ thống

Dự án Game Server được xây dựng theo kiến trúc microservices với Spring Boot và Spring Cloud.

## 📚 Documentation

### For Developers
- **[Client Integration Guide](docs/CLIENT_INTEGRATION_GUIDE.md)** - Quick start guide cho client developers
- **[Client-Server Connection](docs/CLIENT_SERVER_CONNECTION.md)** - Chi tiết về WebSocket và REST API architecture
- **[Phase P0 Summary](PHASE_P0_SUMMARY.md)** - Infrastructure services completion report
- **[Phase P1 Summary](docs/migration/phase-p1_COMPLETED.md)** - Economy services status

### Migration Guides
- **[Migration Overview](docs/migration/README.md)** - Tổng quan migration từ C++ sang Java
- **[Phase P0 - Infrastructure](docs/migration/phase-p0_infra.md)** - Eureka, Gateway, Config
- **[Phase P1 - Economy](docs/migration/phase-p1_economy.md)** - Item, Bag, Wallet, Shop services
- **[Phase P2 - Combat](docs/migration/phase-p2_combat.md)** - Battle, Arena services
- **[Phase P3 - Progress & Social](docs/migration/phase-p3_progress_social.md)** - Quest, Mail, Friend
- **[Phase P4 - Other](docs/migration/phase-p4_other.md)** - Remaining services

---

## 🏗️ Architecture Overview

### Infrastructure Services (Phase P0) ✅

1. **Eureka Server** (Port 8761) - Service Discovery
2. **Config Service** (Port 8091) - Configuration Management  
3. **Gateway Service** (Port 8080) - API Gateway + WebSocket Proxy

### Economy Services (Phase P1) ✅

1. **Item Service** (Port 8220) - Item metadata management
2. **Wallet Service** (Port 8210) - Wallet and transaction management
3. **Bag Service** (Port 8230) - Inventory/bag management

### Communication Services

1. **WebSocket Server** (Port 8090) - Real-time binary protocol (Protobuf)
2. **Session Service** (Port 8081) - Authentication and session management

### Business Services (Đang phát triển)

- User Service
- Shop Service (Port 8260)
- Equip Service (Port 8240)
- Drop Service (Port 8250)
- Gift Service (Port 8270)
- Box Service (Port 8290)
- World Service
- Arena Service

---

## Yêu cầu hệ thống

- **Java**: 21 hoặc cao hơn
- **Maven**: 3.6+
- **Redis**: 6.0+ (optional cho caching)
- **Docker**: 20.10+ (optional cho containerization)

## Build tất cả services

### Build infrastructure services

```bash
# Build Eureka Server
cd eureka-server
mvn clean install -DskipTests

# Build Config Service  
cd ../config-service
mvn clean install -DskipTests

# Build Gateway Service
### Build economy services

```bash
# Build Item Service
cd item-service
mvn clean install -DskipTests

# Build Wallet Service
cd ../wallet-service
mvn clean install -DskipTests

# Build Bag Service
cd ../bag-service
mvn clean install -DskipTests
```

cd ../gateway-service
mvn clean install -DskipTests

# Build Common Library (cần thiết cho các service khác)
cd ../common-lib
mvn clean install -DskipTests
```

### Sử dụng build scripts

```cmd
REM Build infrastructure services
build-infrastructure.cmd

REM Build economy services  
build-economy.cmd
```

### Build business services

```bash
# Ví dụ: build session-service
cd session-service
mvn clean install -DskipTests
```

## Khởi động services

### Thứ tự khởi động khuyến nghị

1. **Khởi động Eureka Server trước** (port 8761)
```bash
java -jar eureka-server/target/eureka-server-1.0.0.jar
```

Truy cập dashboard: http://localhost:8761

2. **Khởi động Config Service** (port 8091)
```bash
java -jar config-service/target/config-service-1.0.0.jar
```

Verify: http://localhost:8091/actuator/health

3. **Khởi động Gateway Service** (port 8080)
```bash
java -jar gateway-service/target/gateway-service-1.0.0.jar
```

Verify: http://localhost:8080/actuator/health

4. **Khởi động các business services**

Các service sẽ tự động register với Eureka và có thể được route qua Gateway.

### Sử dụng Docker Compose (Khuyến nghị)

```bash
# Khởi động infrastructure
cd docker
docker-compose -f docker-compose.merged.yml up -d

# Xem logs
docker-compose -f docker-compose.merged.yml logs -f

# Dừng services
docker-compose -f docker-compose.merged.yml down
### Economy Services (Phase P1)

| Service | Port | Access via Gateway | Purpose |
|---------|------|--------------------| --------|
| Wallet Service | 8210 | http://localhost:8080/wallet-service/** | Wallet & transactions |
| Item Service | 8220 | http://localhost:8080/item-service/** | Item metadata |
| Bag Service | 8230 | http://localhost:8080/bag-service/** | Inventory management |

```

## Cấu trúc thư mục

```
GameServer/
├── common-lib/              # Shared libraries, DTOs, utilities
├── config-service/          # Configuration management service
├── eureka-server/           # Service discovery server
├── gateway-service/         # API Gateway
├── session-service/         # Session & authentication
├── user-service/            # User management
├── item-service/            # Item management
├── bag-service/             # Inventory management
├── wallet-service/          # Currency/wallet management
├── shop-service/            # Shop functionality
├── world-service/           # Game world service
├── arena-service/           # Arena/PvP service
├── webSocket-server/        # WebSocket server
├── docker/                  # Docker compose files
└── docs/                    # Documentation
    └── migration/           # Migration guides
```

## Ports và Endpoints

### Infrastructure Services

| Service | Port | Dashboard/UI | Health Check |
|---------|------|--------------|--------------|
| Eureka Server | 8761 | http://localhost:8761 | http://localhost:8761/actuator/health |
| Config Service | 8091 | - | http://localhost:8091/actuator/health |
| Gateway | 8080 | - | http://localhost:8080/actuator/health |

### Communication Services

| Service | Port | Protocol | Description |
|---------|------|----------|-------------|
| WebSocket Server | 8090 | WS/Binary | Real-time game communication |
| Session Service | 8081 | HTTP/REST | Authentication & session |

**WebSocket Connection**:
```
ws://localhost:8080/websocket-server/ws/game?token={jwt-token}
```

### Economy Services (Phase P1)

| Service | Port | Access via Gateway | Purpose |
|---------|------|--------------------| --------|
| Wallet Service | 8210 | http://localhost:8080/wallet-service/** | Wallet & transactions |
| Item Service | 8220 | http://localhost:8080/item-service/** | Item metadata |
| Bag Service | 8230 | http://localhost:8080/bag-service/** | Inventory management |

## API Documentation

Các service có tích hợp Springdoc OpenAPI:

- Config Service: http://localhost:8091/swagger-ui.html
- Gateway Service: http://localhost:8080/swagger-ui.html (nếu enabled)

## Configuration

### Config Service

Config files được lưu trong `config-service/src/main/resources/config/`:

```
config/
├── gameworld/
│   ├── globalconfig/
│   ├── monster/
│   ├── skill/
│   └── drop/
└── serverconfig/
```

Truy cập config file:
```
GET http://localhost:8091/api/config/file/{path}
```

Ví dụ:
```
GET http://localhost:8091/api/config/file/gameworld/monster/monster.json
```

### Gateway Service

CORS configuration trong `gateway-service/src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:7456"
```

Auth whitelist (không cần authentication):
- /actuator/**
- /session-service/api/session/timesync
- /session-service/api/session/login
- /config-service/api/config/file/**

## Troubleshooting

### Service không register với Eureka

1. Kiểm tra Eureka Server đang chạy
2. Kiểm tra `eureka.client.serviceUrl.defaultZone` trong application.yml
3. Xem logs để tìm connection errors

### Gateway không route được

1. Verify service đã register với Eureka
2. Check Gateway logs
3. Test trực tiếp service endpoint trước khi test qua Gateway
4. Kiểm tra CORS configuration

### Config Service không load được file

1. Kiểm tra mode: classpath vs filesystem
2. Verify file path
3. Check permissions (nếu dùng filesystem mode)
4. Xem logs để tìm errors

### Redis connection issues

1. Start Redis server: `redis-server`
2. Verify connection: `redis-cli ping`
3. Check port 6379
4. Update `spring.data.redis.host` nếu cần

## Development

### Thêm service mới

1. Tạo Spring Boot project với dependencies:
   - Spring Web (hoặc WebFlux)
   - Eureka Discovery Client
   - Actuator
   - Common Lib (internal)

2. Thêm configuration trong application.yml:
```yaml
spring:
  application:
    name: your-service-name

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

3. Build và start service
4. Verify trong Eureka Dashboard

### Hot reload configuration

Config Service hỗ trợ reload configuration mà không cần restart:
- Files được cache với TTL 60 seconds
- ETag support cho caching hiệu quả
- Clear cache thông qua internal API (với token)

## Testing

### Integration Test với Eureka

```java
@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class ServiceTest {
    // Test without Eureka
}
```

### Test Gateway Routes

```bash
# Via Gateway
curl http://localhost:8080/config-service/api/config/file/test.json

# Direct access
curl http://localhost:8091/api/config/file/test.json
```

## Monitoring

### Actuator Endpoints

Enabled endpoints:
- `/actuator/health` - Health status
- `/actuator/info` - Service info
- `/actuator/metrics` - Metrics
- `/actuator/gateway` - Gateway routes (Gateway only)

### Eureka Dashboard

Truy cập http://localhost:8761 để xem:
- Registered services
- Instance status
- Health information
- Metadata

## Security Notes

### Production Checklist

- [ ] Thay đổi internal token trong Config Service
- [ ] Configure proper CORS origins
- [ ] Enable HTTPS/TLS
- [ ] Setup proper Redis authentication
- [ ] Update dependency versions (check for CVEs)
- [ ] Configure firewall rules
- [ ] Setup monitoring alerts
- [ ] Enable distributed tracing

### Current Security Warnings

Một số dependencies có security vulnerabilities:
- logback-core CVE-2025-11226
- spring-beans CVE-2025-41242
- xstream CVE-2024-47072
- protobuf-java CVE-2024-7254

**Action**: Monitor và upgrade khi có patches

## Additional Resources

- [Phase P0 Completion Report](docs/migration/phase-p0_COMPLETED.md)
- [Migration Guide](docs/migration/README.md)
- [Docker Setup](docs/docker/)
- [Proto Files Documentation](docs/proto-index.csv)

## Support

Để báo cáo issues hoặc đóng góp, vui lòng:
1. Check existing documentation
2. Review logs
3. Create detailed issue report với:
   - Service version
   - Error logs
   - Steps to reproduce
   - Expected vs actual behavior

---

**Last Updated**: 2025-11-09  
**Version**: 1.0.0  
**Status**: Infrastructure Phase Complete ✅

