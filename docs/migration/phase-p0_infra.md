# Phase P0 — Nền tảng & Hạ tầng (Infra)

Mục tiêu
- Thiết lập đầy đủ hạ tầng cần thiết để các microservice Java hoạt động: service discovery, config service, gateway, websocket, kafka, redis, observability (prometheus/grafana/zipkin).
- Tạo skeleton services cho infra (Spring Cloud/Eureka/Config/Gateway).

Danh sách service chính (tương ứng với bảng của bạn)
- `gateway-service` — Spring Cloud Gateway (JWT filter, rate-limit)
- `eureka-server` — Service discovery
- `config-service` — Spring Cloud Config or Config Server to serve `application.yml` per service
- `webSocket-server` — Netty or Spring WebSocket (STOMP) for H5 clients
- `session-service` — login/refresh/heartbeat service
- Observability: `spring-boot-admin`, `zipkin`, `prometheus`, `grafana`
- `kafka` (KRaft) + `kafdrop`
- `Redis` instance

Giải thích kỹ thuật & đề xuất
- Service discovery/config: sử dụng Spring Cloud (Eureka + Config Server). Push `application.yml` to `config-service` repository.
- Messaging: giữ Kafka (proto unchanged). For RPC use gRPC (proto-first) or HTTP+protobuf payloads.
- Client networking: H5 uses WebSocket -> `webSocket-server` receives WebSocket/text/binary and translates to internal proto messages, publishing to Kafka or calling backend services.

Tasks (concrete)
1. Create infra repo skeletons under `GameServer/infra/` or in the monorepo as modules.
2. Provide docker-compose (or Kubernetes manifests) to start: zookeeper (if not using KRaft), kafka (KRaft config), kafdrop, redis, mysql instances (service-per-db), eureka, config-service, gateway, prometheus, grafana, zipkin.
3. Create sample Spring Boot app `gateway-service` with:
   - Filters (JWT verification example), rate-limiter (resilience4j or bucket4j), routing to services by serviceId.
4. Create `webSocket-server` skeleton using Spring WebSocket or Netty. If existing C++ client uses binary protobuf over raw TCP, create a Netty binary gateway to bridge to Kafka or convert to websocket messages.

Verification
- Start docker-compose and verify all infra services are UP.
- `gateway-service` routes to a stubbed `user-service` and `session-service` endpoint.
- `webSocket-server` accepts a test WebSocket connection and can send/receive messages encoded with Java-generated proto classes.

Risks
- Networking/protocol mismatch between existing H5 clients and Spring WebSocket: may need a binary gateway.
- Kafka configuration and topic naming consistency must match the proto and event patterns discovered earlier.

Artifacts to deliver
- `docs/docker/infra-docker-compose.yml`
- `infra/gateway-service` Spring Boot skeleton
- `infra/config-service` skeleton with sample properties
- `infra/README.md` with start/stop commands and health checks

Next
- I can generate a `docker-compose` skeleton and the Spring Boot skeleton for `gateway-service` and `config-service` now if you want.