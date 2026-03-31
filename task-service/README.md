# Task Service

**Version**: 1.1.0  
**Phase**: P2 (Combat, World & Social)  
**Port**: 8097  
**Database**: game_task

---

## Overview

Task Service quan ly he thong nhiem vu va diem danh. Service nhan event tu Kafka de cap nhat tien do, cho phep claim thuong, va dong bo task definitions tu `config-service` theo chu ky.

### Core Features
- Task progress tracking
- Seven-day sign-in tracking
- Kafka consumers (Arena, Combat, Trial)
- Claim 1 task hoac claim all completed tasks
- Dynamic task definitions tu `config-service` (co cache + refresh)
- Local fallback task definitions neu `config-service` loi/khong co file

---

## Task Definition Source (Moi)

`task-service` uu tien lay danh sach task tu `config-service`:

- Endpoint: `GET /api/config/file?path=<TASK_CONFIG_PATH>`
- Header ho tro cache: `If-None-Match` / `ETag`
- Neu `304 Not Modified`: giu nguyen cache hien tai
- Neu loi mang/404/parse fail: tiep tuc dung cache cu; neu khoi dong lan dau thi dung fallback local

### Cau hinh lien quan

```yaml
task:
  config:
    path: ${TASK_CONFIG_PATH:gameworld/logicconfig/task/task_cfg.json}
    refresh-initial-delay-ms: ${TASK_CONFIG_REFRESH_INITIAL_DELAY_MS:60000}
    refresh-interval-ms: ${TASK_CONFIG_REFRESH_INTERVAL_MS:60000}
```

Mau schema JSON duoc ho tro (array):

```json
[
  {
    "taskKey": "daily_login",
    "taskName": "Dang nhap hang ngay",
    "description": "Dang nhap vao game",
    "targetValue": 1,
    "goldReward": 100,
    "expReward": 50,
    "itemRewards": ""
  }
]
```

Service cung parse duoc mot so alias key nhu `task_key`, `task_name`, `target`, `gold`, `exp`, `item_rewards`.

---

## Flow Nhiem Vu

```text
[Player chien dau / hoan thanh hoat dong]
battleserver-service -> Kafka (combat-events / arena-events / trial-events)
                           |
                      task-service consumers
                           |
                    reportProgress(taskKey, delta)
                           |
                 cap nhat task_progress theo playerId + taskKey

[Player claim reward]
POST /api/task/claim
  |
  +-> wallet-service (gold/exp)
  +-> bag-service (items)
  +-> mark task = CLAIMED
```

---

## API Endpoints

```text
GET   /api/task/{playerId}/all
POST  /api/task/report
POST  /api/task/claim
POST  /api/task/claim/all/{roleId}
GET   /api/task/progress/{roleId}/{taskKey}
GET   /api/task/config/status
POST  /api/task/config/reload
```

## API Examples

### Get all tasks
```bash
curl http://localhost:8097/api/task/player123/all
```

### Claim one task
```bash
curl -X POST http://localhost:8097/api/task/claim \
  -H "Content-Type: application/json" \
  -d '{"playerId":"player123","taskKey":"daily_login"}'
```

### Claim all completed
```bash
curl -X POST http://localhost:8097/api/task/claim/all/player123
```

### Task config status
```bash
curl http://localhost:8097/api/task/config/status
```

### Trigger manual config reload
```bash
curl -X POST http://localhost:8097/api/task/config/reload
```

---

## Database Schema (thuc te hien tai)

### task_progress
```sql
CREATE TABLE task_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    task_key VARCHAR(64) NOT NULL,
    progress_value INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    last_update TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_player_task (player_id, task_key)
);
```

### seven_day_sign
```sql
CREATE TABLE seven_day_sign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL UNIQUE,
    start_epoch BIGINT NOT NULL,
    signed_mask INT NOT NULL DEFAULT 0,
    claimed_mask INT NOT NULL DEFAULT 0,
    last_sign_date DATE
);
```

---

## Integration Points

### Phụ thuộc
| Service | Endpoint | Mục đích |
|---------|----------|---------|
| **config-service** | `GET /api/config/file?path=gameworld/logicconfig/task_cfg.json` | Load task definitions (ETag cached, TTL 60s) |
| **wallet-service** | (Feign) | Cộng currency khi claim task reward |
| **bag-service** | `POST /api/bag/internal/add` | Phát item khi claim task reward |
| **role-service** | `GET /api/role/{roleId}` | Lấy player level, cộng EXP |

### Kafka Producer (output)
- `task-progress-events`: Báo cáo tiến độ task cho clients khác

### Kafka Consumer (input)
- `combat-events`: Nhận sự kiện chiến đấu để update task progress
- `arena-events`: Nhận sự kiện đấu trường
- `trial-events`: Nhận sự kiện trial

### Được gọi bởi
- **webSocket-server**: Claim task, lấy danh sách task
- **role-service**: (fallback) Lấy task info

---

## Running

```bash
cd GameServer/task-service
mvn clean test
mvn spring-boot:run
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)  
**Last Updated**: 2026-03-22
