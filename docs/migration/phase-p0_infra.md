## Phase P0 — Nền tảng & Hạ tầng (Infra)

Mục tiêu chính
- Chuẩn hoá hạ tầng để các microservice Java có thể chạy song song với cụm C++ hiện có và tiến dần thay thế từng thành phần:
  - Service discovery + Config (Eureka + Config Server)
  - API Gateway (Spring Cloud Gateway)
  - Messaging backbone (Kafka, KRaft preferred)
  - Cache (Redis) và per-service MySQL databases
  - Observability (Prometheus / Grafana / Zipkin)
  - WebSocket / binary gateway để tương thích H5 client
  - Quy trình quản lý .proto: canonicalize ở `common-lib/src/main/proto` và publish artifact

Ngữ cảnh & giả định
- Code nguồn gốc (C++): D:\\project\\serverGame\\开箱h5 — nhiều binaries: `battleserver`, `globalserver`, `crossserver`, `dataaccess`, `gameworld`, v.v. (entry points: `main.cpp`, cấu hình: `Debug/serverconfig.xml`).
- Thông điệp & schema: protobuf — quyết định của bạn: `common-lib/src/main/proto` là nguồn chính.
- Bạn đã có một `docker/docker-compose.yml` trong repo; tôi sẽ không ghi đè file đó, mà bổ sung hoặc tạo file infra riêng cho Phase P0.

Ngắn gọn "contract" cho Phase P0 (inputs/outputs)
- Inputs: C++ config (serverconfig.xml), list proto files, docker/docker-compose.yml hiện tại.
- Outputs:
  - `docs/docker/docker-infra.yml` (hoặc `docker-compose.merged.yml`) — compose để chạy infra cho dev.
  - `infra/` hoặc `services/` skeletons: `gateway-service`, `config-service`, `eureka-server`, `websocket-gateway`.
  - Hướng dẫn: chạy, verify health, smoke tests.

Mapping C++ → Java (gợi ý phân tách dịch vụ)
- battleserver → combat-service (tính toán trận đấu; stateful; có thể là clusterless worker hoặc job queuing + Kafka)
- globalserver → global-service (world/meta, cross-shard coordination)
- crossserver → cross-service (cross-region/instance coordination)
- dataaccess → data-service (truy vấn/transactional, tương tác DB; nơi đặt Flyway migrations)
- gameworld → gameworld-service (game loop, scheduling, domain logic)
- Lưu ý: lúc đầu, ta sẽ không di chuyển toàn bộ logic; tạo các stub HTTP endpoints hoặc consumers/consumers Kafka để tiêu thụ events và xác minh end-to-end.

Infra components (chi tiết)
- Kafka (KRaft recommended): topics management, ACLs (sau). Provide KRaft single-node compose for dev.
- Redis: session cache, leaderboard, locks.
- MySQL: per-service databases. Khuyến nghị: tạo schema name tương ứng (userdb, role_db, bag_db, reportdb).
- Eureka + Config Server: service discovery và centralized config; Config có thể lấy từ Git repo (local file-backed for dev).
- Gateway: Spring Cloud Gateway với JWT filter + rate limiting (bucket4j/resilience4j).
- WebSocket gateway: Netty-based binary gateway if C++/H5 uses protobuf binary; alternately Spring WebSocket for STOMP/text if H5 supports.
- Observability: Zipkin for tracing; Prometheus + Grafana for metrics.

Detailed tasks (actionable checklist)
1) Canonical protos & artifact (required)
   - Verify `common-lib/src/main/proto` contains all canonical .proto.
   - Build & install locally so Java services can depend on it:

```powershell
Set-Location -LiteralPath 'D:\\\\project\\\\serverGame\\\\GameServer\\\\common-lib'
mvn -DskipTests install
```

   - Result: artifact installed to ~/.m2 and can be referenced as a dependency.

2) Docker infra for local dev (non-destructive)
   - Option A (recommended low-risk): create `docker/docker-infra.yml` containing only infra services: Kafka (KRaft), Redis, Prometheus, Grafana, Zipkin, Eureka, Config.
   - Option B (merge): create `docker/docker-compose.merged.yml` (already created) that reuses your existing DB/Kafka entries and adds infra.
   - Ensure Prometheus config `./prometheus/prometheus.yml` exists and points to the service metrics endpoints.
   - Start infra:

```powershell
Set-Location -LiteralPath 'D:\\\\project\\\\serverGame\\\\GameServer\\\\docker'
docker compose -f docker-infra.yml up -d
# or if you use the merged file:
docker compose -f docker-compose.merged.yml up -d
```

3) Create per-service DB schemas & Flyway
   - Under `infra/sql/` or each service module, add Flyway migrations (V1__init.sql) that create required schemas/tables. Example schemas: `userdb`, `game_role`, `db_bag`, `report_game_h2`.
   - For initial PoC keep schemas minimal (users, roles, inventory tables) and seed data for login.

4) Scaffold minimal Java services
   - `config-service`: Spring Cloud Config server (serves application-{service}.yml). Use local filesystem backend for dev.
   - `eureka-server`: minimal Eureka server.
   - `gateway-service`: Spring Boot app with dependency on `common-lib` artifact; example route to `user-service` and JWT filter.
   - `websocket-gateway`: Netty-based lightweight binary gateway that accepts H5 client connections and publishes/consumes protobuf messages to/from Kafka.

5) Wire messaging & topics
   - Define topic naming convention and create topics (dev only): `events.user.*`, `events.battle.*`, `commands.*`.
   - Add a simple consumer/stub service that subscribes to the main topics and logs messages (verify protobuf deserialization using generated Java classes).

6) Health & observability
   - Each Java service: actuator endpoints (/actuator/health, /actuator/prometheus if micrometer). Expose metrics port.
   - Zipkin: configure spring.zipkin.base-url for tracing.
   - Prometheus scrape targets: gateway, services, kafka-exporter (optional).

Verification & smoke-tests
- Step 1: Start infra compose and ensure services are UP:

```powershell
docker compose -f docker/docker-infra.yml ps
# or merged
docker compose -f docker/docker-compose.merged.yml ps
```

- Step 2: Start `config-service`, `eureka-server`, then `gateway-service` (stub) and `websocket-gateway`.
- Step 3: Confirm gateway routes a request to a stubbed user-service (curl or Postman):

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/user/health
```

- Step 4: Connect a test WebSocket client (e.g., browser or wscat) to websocket-gateway and send a protobuf-encoded login message; verify it arrives in the Kafka topic and a consumer logs it.

Edge cases & risks
- C++ networking format may be raw TCP with custom framing; if so, a pure Spring WebSocket won't interoperate — implement a binary Netty gateway that understands framing.
- Topic, partitioning, ordering semantics in Kafka are important for deterministic game logic (battles). For PoC, single-partition topic per aggregate key is acceptable.
- DB schema differences and character sets between C++ code and MySQL default. Verify engine, collation, and timezones.

Files to create / edit (deliverables)
- `docker/docker-infra.yml` (infra-only compose) OR `docker/docker-compose.merged.yml` (merged; already added)
- `infra/prometheus/prometheus.yml` (scrape config)
- `infra/sql/V1__init.sql` or per-service `src/main/resources/db/migration/V1__init.sql`
- `services/gateway-service/` (Spring Boot skeleton + Dockerfile + README)
- `services/websocket-gateway/` (Netty binary gateway skeleton)

Next concrete steps I can execute now (pick any combination):
1. Run `mvn -DskipTests install` in `common-lib` to publish the proto artifact to local Maven repo. (recommended first step)
2. Commit the existing `docker/docker-compose.merged.yml` into `Trunk` (it is already created in the repo workspace; tell me to commit and I'll add/commit).
3. Instead of merged file, create a safer `docker/docker-infra.yml` containing only infra services and commit it.
4. Scaffold `services/gateway-service` Spring Boot skeleton (pom.xml, Application, sample controller, Dockerfile) and wire dependency to `common-lib` (requires step 1).

Cho tôi biết bạn muốn tôi làm bước nào (ví dụ: "1 và 3 và 4"), hoặc chỉ "1" để tôi bắt đầu cài artifact `common-lib`.

---
_Ghi chú_: Tôi đã đọc nhanh các `main.cpp` và `Debug/serverconfig.xml` trong C++ tree để đảm bảo mapping dịch vụ hợp lý — nếu bạn muốn, tôi sẽ trích xuất các cổng, tên DB và tên topic chính xác từ file cấu hình C++ và cập nhật docker/SQL/flyway script tự động.
