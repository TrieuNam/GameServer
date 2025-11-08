# Phase P1 — Domain: Kinh tế & Vật phẩm (Economy & Items)

Mục tiêu
- Port các chức năng quản lý tiền, vật phẩm, túi đồ, trang bị, rớt đồ, shop, quà và chế tạo từ C++ sang Java microservices.
- Bảo đảm idempotence cho giao dịch tài chính và consistency cho túi đồ (optimistic lock / versioning).

Service mapping đề xuất
- `wallet-service` (HTTP 8210) — account balance, transactions, idempotent operations
- `item-service` (HTTP 8220) — item metadata, read-only, cache
- `bag-service` (HTTP 8230) — runtime bag: grant/consume, optimistic locking
- `equip-service` (HTTP 8240) — equip, upgrade logic
- `drop-service` (HTTP 8250) — drop tables and RNG pools
- `shop-service` (HTTP 8260) — shop catalogs + purchases
- `gift-service` (HTTP 8270) — code redemption & safe gift delivery
- `crafting-service` (HTTP 8280) — crafting recipes
- `box-service` (HTTP 8290) — open box/loot logic

Data & schema
- Use per-service MySQL schema as you planned. Add Flyway migrations per service that correspond to existing SQL.
- Use Redis for caches and short-lived locks.

Eventing & integration
- Topics (examples from your mapping):
  - `gameh5.bag.grant` — producer: role-service / consumer: bag-service
  - `gameh5.bag.changed` — producer: bag-service / consumer: websocket-service
- Use Kafka to publish domain events. Use protobuf payloads for events.

Concrete migration steps (suggested order)
1. Extract table definitions for `wallet_db`, `bag_db`, `item` tables and create Flyway migrations.
2. Create `common-proto` and ensure message types used by these services are present.
3. Implement `item-service` first (read-only, low risk). Steps:
   - Spring Boot app + REST controller for metadata queries.
   - Load metadata from config files (maybe reuse XML/JSON configs present in C++ `config` folder).
   - Add caching (Redis) and metrics.
4. Implement `bag-service` next (more complex):
   - API: grantItems(userId, roleId, items[], source), consumeItem(userId, roleId, itemId, amount)
   - Ensure atomic operations with DB row versioning or optimistic locking. Use transactions with SELECT ... FOR UPDATE or optimistic version field.
   - On operations, publish events to Kafka using proto messages.
5. Wallet-service: implement idempotent transactions using transactionId dedup table or idempotency key.
6. Create end-to-end tests that simulate grant -> bag change -> websocket event flow.

Verification
- Unit tests for each service.
- Integration test: user grants item via `role-service` (stub) -> `bag-service` updates DB and publishes Kafka event -> `websocket-server` receives event and forwards to test client.

Risks
- Race conditions when migrating from in-process module to distributed microservice; choose a single owner of state (bag ownership) and use distributed locks if necessary.
- Differences in random behavior (drop RNG) if not using same seed/algorithm.

Artifacts to produce
- Per-service skeletons under `GameServer/services/` with README, Flyway migrations, sample controllers and Kafka producers/consumers.

Next
- I can create the `item-service` skeleton and Flyway migration files from the C++ SQL if you want. Choose: create `item-service` or `bag-service` skeleton first.