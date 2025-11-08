# Phase 0 — Preparation

Mục tiêu
- Thu thập mọi tài sản chung cần thiết trước khi port: `.proto` (message contracts), SQL schema (tạo bảng), file config (serverconfig.xml và các config loader), danh sách topic Kafka.
- Tạo module/proj `common/proto` để generate Java classes.
- Thiết lập hệ thống quản lý schema DB (Flyway/Liquibase) bằng SQL có sẵn.

Artifacts đầu ra
- `GameServer/common/proto/` — copy tất cả `.proto` liên quan
- `GameServer/docs/sql/` — per-service SQL files (Flyway format V1__create_*.sql)
- `GameServer/docs/config/` — converted `serverconfig.xml` -> Spring `application.yml` example
- `GameServer/docs/proto-index.csv` — mapping file: proto-file, top-level messages, description

Các bước thực hiện (chi tiết)
1. Inventory proto
   - Tìm tất cả `.proto` trong `开箱h5/server/server/src/servercommon/proto` và `.../protobuf`.
   - Tạo `docs/proto-index.csv` chứa: proto_path, package, top_message_names, brief_purpose.

2. Create `common/proto` module in Java repo
   - Tạo folder `GameServer/common/proto/`.
   - Copy proto files vào `common/proto/src/main/proto/`.
   - Tạo Maven/Gradle skeleton (pom.xml hoặc build.gradle) với plugin `protobuf-gradle-plugin` hoặc `maven-protobuf-plugin`.
   - Run protoc to generate Java classes. Verify `protoc --version` and `protoc-gen-grpc-java` if using gRPC.

3. Extract DB schema
   - Tập hợp các file SQL trong `sql_change` và `tabledefcreateor`.
   - Tổ chức theo dịch vụ: `user_db`, `bag_db`, `role_db`, ... và tạo migration files cho Flyway: `V1__create_user_tables.sql`, `V1__create_bag_tables.sql`, ...
   - Add a README describing table ownership per service.

4. Capture server config and runtime params
   - Copy `serverconfig.xml` -> create `docs/config/serverconfig.md` mapping important keys -> suggested Spring properties (e.g., network ports, job queue sizes).

5. Generate test vectors
   - From proto, create a small set of serialized test messages (hex/base64) that we can use to validate Java deserialization.

Kiểm chứng (verification)
- Java generated proto classes compile in a new `common-proto` module.
- Flyway migrations run successfully against a local MySQL instance and create expected tables.
- A small Java test can deserialize sample messages produced by the C++ server (or by `protoc --encode`).

Rủi ro & ghi chú
- Một vài proto có C++-specific options or custom types — kiểm tra và điều chỉnh plugin/protobuf version.
- DB schema mapping: kiểu dữ liệu (unsigned/int64) and encoding may need mapping to JPA types.

Commands (Windows PowerShell samples)
```powershell
# create proto module (example using gradle)
cd D:\project\serverGame\GameServer
mkdir common\proto -Force
# copy proto files (example)
# (I can perform this copy for you if you want)
```

Tiếp theo
- Nếu bạn muốn, mình sẽ: (A) generate `docs/proto-index.csv` automatically, (B) create the Java proto module skeleton and run protoc to generate classes. Chọn A hoặc B.