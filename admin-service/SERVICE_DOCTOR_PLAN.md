# Service Doctor Plan for `admin-service`

**Date:** 2026-04-05  
**Scope:** `D:\project\serverGame\GameServer\admin-service`  
**Goal:** xây tool theo dõi log startup/runtime của các service, phát hiện lỗi theo thời gian thực, hiển thị lên Web UI, yêu cầu approval của admin, sau đó mới gọi GitHub Copilot CLI để đề xuất/sửa lỗi và build lại service để xác minh.

---

## 1. Mục tiêu chính

Tool này **không phải auto chơi game**. Tool chỉ hỗ trợ quy trình kỹ thuật:

1. theo dõi log service real-time;
2. phát hiện trạng thái `WAITING / STARTED / ERROR`;
3. hiển thị cảnh báo trên Web UI;
4. cho admin **Approve / Reject** hướng sửa;
5. nếu được duyệt thì gọi **GitHub Copilot CLI**;
6. build lại service để kiểm tra fix có hợp lệ hay không;
7. lưu lịch sử, prompt, kết quả build và diff để audit.

---

## 2. Định nghĩa MVP

### MVP cần làm được
- Theo dõi log của 1 hoặc nhiều service.
- Phát hiện các lỗi startup phổ biến.
- Phát hiện cả lỗi **runtime/gameplay** khi service vẫn đang `RUNNING`.
- Có Web UI hiển thị trạng thái theo thời gian thực.
- Có nút **Approve Fix** trước khi gọi Copilot CLI.
- Có bước **Build Verify** sau khi sửa.
- Ghi report ra `txt/json`.

> **Lưu ý:** bản cũ thiên về startup/build. Từ bản cập nhật này, plan yêu cầu bắt thêm lỗi lúc đang vào game/chơi thật, không chỉ lúc boot service.

### MVP chưa cần
- auto chơi game;
- auto click client;
- auto test gameplay end-to-end;
- auto commit code;
- auto sửa nhiều service cùng lúc không kiểm soát.

---

## 3. Luồng hoạt động mong muốn

```text
[Test game / start service]
        |
        v
[Log Watcher đọc log real-time]
        |
        v
[Error Classifier]
  |- WAITING
  |- STARTED
  |- ERROR
        |
        v
[Hiển thị trên Web UI + gửi notification]
        |
        v
[Admin Approve / Reject]
        |
        +--> Reject: chỉ lưu report, không sửa
        |
        v
[Copilot CLI Adapter]
        |
        v
[Apply patch có guardrails]
        |
        v
[Build Verifier]
        |
        +--> PASS => VERIFIED
        |
        +--> FAIL => FAILED + hiển thị lỗi build
```

---

## 4. Kiến trúc đề xuất

### 4.1 Backend trong `admin-service`
Tạo package mới:

```text
src/main/java/com/southMillion/admin/doctor/
```

Các thành phần chính:

- `DoctorController`
  - REST API cho UI.
- `DoctorSessionService`
  - quản lý trạng thái phiên theo từng service.
- `LogWatcherService`
  - tail log file hoặc bắt output từ process.
- `ErrorClassifierService`
  - phân loại lỗi bằng regex/rules.
- `CopilotCliService`
  - gọi `gh copilot` / Copilot CLI.
- `BuildVerificationService`
  - chạy Maven build và lưu kết quả.
- `DoctorEventBroadcaster`
  - đẩy cập nhật ra UI qua SSE.

### 4.2 Script hỗ trợ
Đặt tại:

```text
monitoring/
```

Đề xuất cấu trúc:

```text
monitoring/
  service-registry.json
  Start-ServiceDoctor.ps1
  Parse-StartupError.ps1
  Invoke-CopilotRepair.ps1
  Verify-Build.ps1
  reports/
```

---

## 5. Trạng thái nghiệp vụ

Mỗi service có thể đi qua các trạng thái:

- `IDLE` — chưa theo dõi
- `WAITING` — đang đợi service boot, đợi dependency, hoặc đang retry kết nối
- `STARTED` — service chạy ổn
- `ERROR` — phát hiện lỗi startup/runtime/build cần xử lý
- `NEEDS_APPROVAL` — đã phân tích xong, chờ admin
- `FIXING` — đang gửi yêu cầu cho Copilot CLI
- `BUILDING` — đang verify bằng Maven
- `VERIFIED` — build pass / service lên lại thành công
- `FAILED` — build fail hoặc fix chưa đạt
- `REJECTED` — admin không duyệt fix

---

## 6. Các lỗi cần bắt ở bản đầu

### Startup / Infra
- `Port already in use`
- `Failed to bind properties`
- `BeanCreationException`
- `ApplicationContextException`
- `UnsatisfiedDependencyException`
- `FlywayException`
- `JDBCConnectionException`
- `Communications link failure`
- `RedisConnectionFailureException`
- `KafkaException`
- `Connection refused`
- `NoSuchMethodError`
- `ClassNotFoundException`

### Build / Compile
- `COMPILATION ERROR`
- `cannot find symbol`
- `incompatible types`
- `package ... does not exist`
- `Failed to execute goal`

### Runtime / Gameplay khi service vẫn RUNNING
- `NullPointerException`, `IllegalStateException`, `IndexOutOfBoundsException`
- `HTTP 500`, `Internal Server Error`, request fail bất thường
- `WebSocket closed/error`, `Broken pipe`, `Read timed out`
- `Kafka timeout`, `Redis timeout`, `gRPC deadline exceeded`
- `deadlock`, `lock wait timeout exceeded`, `OutOfMemoryError`
- spike log `ERROR`/`Exception` dù service chưa chết process

---

## 7. API đề xuất cho `admin-service`

```http
GET  /api/doctor/sessions
GET  /api/doctor/sessions/{serviceName}
GET  /api/doctor/sessions/{serviceName}/events      # SSE stream
POST /api/doctor/sessions/{serviceName}/watch
POST /api/doctor/sessions/{serviceName}/approve
POST /api/doctor/sessions/{serviceName}/reject
POST /api/doctor/sessions/{serviceName}/retry-build
GET  /api/doctor/sessions/{serviceName}/report
```

### Response mẫu
```json
{
  "serviceName": "role-service",
  "status": "ERROR",
  "lastErrorType": "BeanCreationException",
  "lastErrorSummary": "Failed to create datasource bean",
  "approvalRequired": true,
  "lastUpdated": "2026-04-05T10:15:00"
}
```

---

## 8. Web UI đề xuất

Tạo trang:

```text
src/main/resources/static/doctor.html
```

### Thành phần UI
- bảng danh sách service;
- cột `Status`;
- cột `Last Error`;
- cột `Build Result`;
- nút `View Log`;
- nút `View Prompt`;
- nút `Approve Fix`;
- nút `Reject`;
- nút `Retry Build`.

### Hành vi UI
- tự refresh qua **SSE** mỗi 1–2 giây;
- khi phát hiện lỗi thì badge đổi màu đỏ;
- khi chờ duyệt thì hiện nút Approve;
- khi build xong thì hiển thị PASS/FAIL rõ ràng.

---

## 9. Guardrails an toàn

Đây là phần bắt buộc để tránh sửa code quá tay:

1. **chỉ sửa trong whitelist path**;
2. **mỗi service tối đa 2-3 lần retry**;
3. luôn lưu:
   - prompt gửi cho Copilot,
   - response nhận về,
   - build log,
   - diff trước/sau;
4. **không auto commit**;
5. bắt buộc có **Approve** trước khi fix;
6. nếu build fail thì dừng ngay, không lặp vô hạn;
7. ưu tiên `suggest patch` trước, apply sau;
8. cho phép rollback bằng `git diff` / stash.

---

## 10. Action plan cụ thể

## Phase 1 — Theo dõi log và phân loại lỗi
**Mục tiêu:** nhìn thấy lỗi real-time trên UI.

### Action
- [ ] Tạo package `doctor` trong `admin-service`.
- [ ] Tạo model `DoctorSession` và `DoctorEvent`.
- [ ] Tạo `LogWatcherService` đọc log từ file/process.
- [ ] Tạo `ErrorClassifierService` với bộ regex cơ bản.
- [ ] Tạo API `GET /api/doctor/sessions`.
- [ ] Tạo `doctor.html` hiển thị danh sách service và trạng thái.
- [ ] Ghi report ra `monitoring/reports/*.json`.

### Done when
- [ ] Start một service lỗi và UI hiển thị `ERROR`.
- [ ] Có summary lỗi ngắn gọn trên màn hình.

---

## Phase 1B — Runtime / gameplay monitoring
**Mục tiêu:** vẫn đang vào game/chơi mà có lỗi thì UI cũng phải nổi cảnh báo.

### Action
- [ ] Mở rộng `ErrorClassifierService` để bắt lỗi runtime khi service vẫn `RUNNING`.
- [ ] Bắt các dòng log `ERROR`, `Exception`, `HTTP 500`, timeout, websocket disconnect.
- [ ] Ưu tiên hiển thị lỗi mới nhất phát sinh trong lúc test game.
- [ ] Nếu cần, bổ sung endpoint synthetic check cho các flow quan trọng như login, vào map, đánh quái, nhận thưởng.

### Done when
- [ ] Service vẫn `RUNNING` nhưng log xuất hiện `NullPointerException`/`500` thì UI chuyển `ERROR` hoặc `NEEDS_APPROVAL`.
- [ ] Khi test game thực tế, admin nhìn thấy lỗi runtime trong vòng vài giây.

---

## Phase 2 — Approval workflow
**Mục tiêu:** admin có thể duyệt hoặc từ chối việc fix.

### Action
- [ ] Thêm trạng thái `NEEDS_APPROVAL`.
- [ ] Tạo nút `Approve Fix` và `Reject` trên `doctor.html`.
- [ ] Tạo API `POST /approve` và `POST /reject`.
- [ ] Lưu audit log cho quyết định của admin.

### Done when
- [ ] Khi có lỗi, hệ thống không tự fix.
- [ ] Chỉ sau khi bấm Approve mới chuyển sang `FIXING`.

---

## Phase 3 — Tích hợp GitHub Copilot CLI
**Mục tiêu:** tạo prompt sửa lỗi có ngữ cảnh và gọi CLI.

### Action
- [ ] Tạo `CopilotCliService` hoặc script `Invoke-CopilotRepair.ps1`.
- [ ] Chuẩn hóa prompt gồm:
  - service name,
  - stacktrace rút gọn,
  - file nghi ngờ,
  - yêu cầu fix tối thiểu,
  - lệnh build verify.
- [ ] Ghi lại prompt/response vào `monitoring/reports/`.
- [ ] Chỉ cho sửa trong scope repo cho phép.

### Prompt template mẫu
```text
Service: {serviceName}
Symptom: startup/build failed
Error summary: {errorSummary}
Stacktrace excerpt:
{stackExcerpt}

Task:
- find root cause
- make minimal code/config fix
- do not change unrelated files
- keep current behavior intact
- ensure Maven build passes for this service
```

### Done when
- [ ] Sau khi bấm Approve, hệ thống sinh prompt và gọi được Copilot CLI.

---

## Phase 4 — Build verify
**Mục tiêu:** chứng minh fix hợp lệ bằng build thật.

### Action
- [ ] Tạo `BuildVerificationService`.
- [ ] Chạy lệnh build phù hợp, ví dụ:
  ```bash
  mvn clean package -DskipTests
  ```
- [ ] Lưu stdout/stderr ra file report.
- [ ] Parse kết quả PASS/FAIL.
- [ ] Nếu PASS thì cập nhật `VERIFIED`.
- [ ] Nếu FAIL thì cập nhật `FAILED` và show lỗi build.

### Done when
- [ ] UI hiển thị trạng thái build cuối cùng với timestamp.

---

## 11. Thứ tự triển khai khuyến nghị

### Sprint 1 (1-2 ngày)
- log watcher
- classifier
- `doctor.html`
- sessions API

### Sprint 2 (1 ngày)
- approval workflow
- report JSON/TXT
- audit trail

### Sprint 3 (1-2 ngày)
- Copilot CLI integration
- build verification
- retry / fail-safe rules

---

## 12. File nên tạo trong repo

```text
admin-service/
  SERVICE_DOCTOR_PLAN.md
  monitoring/
    service-registry.json
    Start-ServiceDoctor.ps1
    Parse-StartupError.ps1
    Invoke-CopilotRepair.ps1
    Verify-Build.ps1
    reports/
  src/main/java/com/southMillion/admin/doctor/
    DoctorController.java
    DoctorSessionService.java
    LogWatcherService.java
    ErrorClassifierService.java
    CopilotCliService.java
    BuildVerificationService.java
  src/main/resources/static/
    doctor.html
```

---

## 13. Acceptance criteria

Chỉ xem là đạt khi thỏa các điều kiện sau:

- [ ] Có thể theo dõi ít nhất 1 service real-time.
- [ ] Bắt được ít nhất 5 nhóm lỗi startup/build phổ biến.
- [ ] Bắt được lỗi runtime quan trọng ngay cả khi service vẫn `RUNNING`.
- [ ] UI hiển thị `WAITING / ERROR / VERIFIED / FAILED`.
- [ ] Có bước **Approve** trước khi gọi Copilot CLI.
- [ ] Có build verify sau khi sửa.
- [ ] Có report log/prompt/build lưu vào file.

---

## 14. Bước nên làm ngay hôm nay

### Ưu tiên cao
1. tạo `doctor` package;
2. tạo `doctor.html` bản đơn giản;
3. tạo `DoctorController` với API danh sách sessions;
4. tạo `LogWatcherService` chỉ để watch 1 service trước;
5. test với một service đang có lỗi startup.
6. test thêm một flow runtime thật (ví dụ login / đánh quái / nhận thưởng) để chắc lỗi lúc chơi game cũng nổi trên dashboard.

### Ưu tiên tiếp theo
6. thêm `Approve / Reject`;
7. thêm adapter gọi Copilot CLI;
8. thêm Maven build verification.

---

## 15. Ghi chú triển khai

- Bản đầu nên ưu tiên **read-only + notify** trước.
- Sau đó mới bật **Approve + Fix + Build**.
- Không nên cho auto-heal full system ngay từ đầu.
- Cần giữ nguyên nguyên tắc: **evidence before completion** — chỉ đánh dấu `VERIFIED` khi build thực sự pass.

---

## 16. Kết luận

Đây là một hướng triển khai khả thi cho `admin-service`:

- theo dõi log service real-time;
- thông báo lỗi qua Web UI;
- yêu cầu approval;
- gọi Copilot CLI để hỗ trợ sửa;
- build lại để xác minh kết quả.

Nếu triển khai đúng theo các phase trên, bạn sẽ có một **Service Doctor MVP** đủ dùng để tăng tốc việc phát hiện và xử lý lỗi khi test game/server.
