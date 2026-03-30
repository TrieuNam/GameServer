# 📋 Luồng Hướng Dẫn Nhiệm Vụ (Task Guide Flow)

## 🎯 Khi Click vào Task Item trên màn hình chính

File: `MainTaskItem.ts` → hàm `onClickFetch()`

```
Player click vào Task
        │
        ▼
  info.pro >= cfg.param?
  (tiến độ >= mục tiêu?)
   ┌────┴────┐
  YES       NO
   │         │
   ▼         ▼
SendFetchTaskReq()   GuideCtrl.Start(cfg.guide_id)
(Nhận thưởng)        (Bắt đầu hướng dẫn)
```

---

## 📍 Khi Task Chưa Hoàn Thành → `GuideCtrl.Start(guide_id)`

### Bước 1: `GuideCtrl.Start(id)`
- Lấy config guide từ `function_guide_auto.json`
- Lấy danh sách steps cho guide đó
- Gọi `Continue()` → `Execute()`

### Bước 2: `Execute()` xử lý từng step

Có 2 loại step:
| StepType | Tên | Hành động |
|----------|-----|-----------|
| 1 | AutoOpenView | Tự động chuyển tiếp (không cần click) |
| 2 | ClickButton | Hiện ngón tay/mũi tên → Chờ player click |

### Bước 3: `GuideButton` (ClickButton step)
1. Dùng timer **1 frame/lần, 800 lần** để tìm UI element theo `step_param_1` (key)
2. Khi tìm thấy → Tính vị trí và hiện hình **ngón tay trỏ** vào nút đó
3. Khi player click → `simulateClick()` vào nút thật → Gọi bước tiếp

---

## 🗺️ Ví dụ: Task_id=0 "Mở 1 rương" (guide_id=1)

```
Click Task "打开一个宝箱"
  → GuideCtrl.Start(1)
    → Step: step_param_1 = "BtnBoxOpen"
      → Hiện ngón tay trỏ vào nút [Rương]
        → Player click
          → simulateClick(BtnBoxOpen)
          → Rương thực sự mở
            → Server nhận Kafka event (condition=3)
            → taskProgress tăng: 0→1
            → Server gửi PB_SCTaskProgressInfo về client
              → Client cập nhật pro=1 >= param=1
                → Hiện nút "Nhận thưởng"
```

---

## ⚡ Cơ chế Tự Động (Auto-trigger)

### 1. Khi nhận task MỚI có `is_auto=1`:
```typescript
// Trong FlushData()
if (last_task_id != new_task_id && pro < param && is_auto == 1) {
    GuideCtrl.Start(guide_id)  // Guide tự khởi động!
}
```
→ Tasks có `is_auto=1`: task_id 1, 9, 33, 108

### 2. Task đầu tiên (task_id=0) luôn auto-click khi load:
```typescript
if (task_id == task_list[0].task_id) {
    this.onClickFetch();  // Tự gọi → tự khởi động guide
}
```

---

## 🔑 Mapping Guide → Button

| Guide ID | Tên | Step 1 Button | Step 2 Button |
|----------|-----|---------------|---------------|
| 1 | Mở rương | `BtnBoxOpen` | - |
| 2 | Phiêu lưu | `BtnMaoXian` | - |
| 3 | Nâng cấp rương | `BtnBoxUp` | `BtnBoxBuy` |
| 5 | Đấu trường | `BtnArena` | `BtnEqual` |
| 18 | Hộ tống | `BtnDungeon` → `EscortMainBtn` → `EscortBoatBtn` → `EscortBtnEscort` | - |
| 28 | Tự động mở rương | `BtnBoxAuto` | `BtnBoxAutoStart` |

---

## 📊 Cấu trúc CfgTask quan trọng

```typescript
{
  task_id: 0,
  condition: 3,    // Loại điều kiện (3 = mở rương)
  param: 1,        // Mục tiêu (1 lần)
  task_plan: 1,    // Mục tiêu hiển thị progress bar
  guide_id: 1,     // Guide nào sẽ hiện khi click
  is_auto: 0       // 0 = phải click; 1 = tự khởi động guide
}
```

---

## 🏗️ Kiến trúc Server: Analytics làm Intermediary

### Nguyên tắc thiết kế
`analytics-service` là **điểm trung gian duy nhất** nhận tất cả game events từ các service,
xử lý tracking, sau đó publish `TaskProgressEvent` để `task-service` cập nhật progress.
`task-service` **không** lắng nghe trực tiếp raw events từ các service khác.

### Luồng hoàn chỉnh

```
[Source Service]          [analytics-service]              [task-service]
guild-service      ──────► consumeGuildCreated()    ──────► TaskProgressEventConsumer
  guild.created            consumeGuildJoined()              → reportProgress("create_guild")
  guild.joined             consumeGuildLeft()                → reportProgress("join_guild")
  guild.left               trackEvent() [lưu DB]
                           publishTaskProgressEvent()  ──────► task.progress.update topic
                                   │
combat/gameworld   ──────► consumeCombatResult()     ──────► TaskProgressEventConsumer
  combat.result            trackEvent() [lưu DB]              → reportProgress("kill_monster")
                           publishTaskProgressEvent()

arena-service      ──────► consumeArenaMatchEnd()    ──────► TaskProgressEventConsumer
  arena.match.end          trackEvent() [lưu DB]              → reportProgress("arena_win")
                           publishTaskProgressEvent()

trial-service      ──────► consumeTrialCompleted()   ──────► TaskProgressEventConsumer
  trial.completed          trackEvent() [lưu DB]              → reportProgress("trial_complete")
                           publishTaskProgressEvent()
```

### Topic Kafka

| Topic (nguồn vào analytics) | Nguồn publish | Task Key tương ứng |
|---|---|---|
| `guild.created` | guild-service | `create_guild` |
| `guild.joined` | guild-service | `join_guild` |
| `guild.left` | guild-service | *(tracking only)* |
| `combat.result` | gameworld/battle-service | `kill_monster` |
| `arena.match.end` | arena-service | `arena_win` |
| `trial.completed` | trial-service | `trial_complete` |

| Topic (analytics → task-service) | Mô tả |
|---|---|
| `task.progress.update` | analytics-service publish sau khi xử lý event |

---

## 📝 Kế hoạch thay đổi (Implementation Plan)

### 1. common-lib — Thêm mới

**`TaskProgressEvent.java`** *(tạo mới)*
`org.SouthMillion.dto.event.task.TaskProgressEvent`

```java
// Fields:
String  eventId       // UUID idempotency
Long    roleId        // Player cần cập nhật progress
String  taskKey       // "kill_monster" | "join_guild" | "create_guild" | "arena_win" | "trial_complete" | ...
Integer progressDelta // Số lượng tăng thêm (thường = 1)
String  source        // Service gốc phát sinh event: "guild-service" | "combat" | "arena" | "trial"
Instant occurredAt
```

---

### 2. analytics-service — Sửa đổi

#### 2a. `pom.xml`
- Đã có `spring-kafka` (consumer) → thêm config producer

#### 2b. `application.yml`
- Thêm Kafka **producer** config:
```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
```

#### 2c. `AnalyticsEventConsumer.java` *(sửa)*
Inject `KafkaTemplate`, sau mỗi `trackEvent()` gọi thêm `publishTaskProgressEvent()`:

| Method | Publish TaskProgressEvent với taskKey |
|---|---|
| `consumeGuildCreated()` | `create_guild`, delta=1 |
| `consumeGuildJoined()` | `join_guild`, delta=1 |
| `consumeGuildLeft()` | *(không publish — chỉ tracking)* |
| `consumeBattleEvent()` / combat | `kill_monster`, delta=killCount |
| `consumeArenaEvent()` | `arena_win`, delta=1 (chỉ khi winner) |
| `consumeTaskCompleted()` | `trial_complete`, delta=1 |

---

### 3. task-service — Sửa đổi

#### 3a. `GuildEventConsumer.java` *(XÓA)*
Không còn cần — guild events giờ đến qua `task.progress.update`.

#### 3b. `CombatEventConsumer.java` *(sửa)*
- **Giữ nguyên** phần achievement + statistics
- **Xóa** block `taskDomainService.reportProgress("kill_monster")` vì analytics đã xử lý

#### 3c. `ArenaEventConsumer.java` *(sửa)*
- **Giữ nguyên** phần achievement + statistics + leaderboard
- **Xóa** block `taskDomainService.reportProgress("arena_win")` vì analytics đã xử lý

#### 3d. `TrialEventConsumer.java` *(sửa)*
- **Giữ nguyên** phần achievement + statistics
- **Xóa** block `taskDomainService.reportProgress("trial_complete")` vì analytics đã xử lý

#### 3e. `TaskProgressEventConsumer.java` *(tạo mới)*
```
Lắng nghe topic: task.progress.update
groupId: task-service

→ Nhận TaskProgressEvent
→ Gọi taskDomainService.reportProgress(playerId, taskKey, delta)
→ ack.acknowledge()
```

---

## ✅ Mapping Task Key → Condition Code (CfgTask)

| taskKey | condition | Mô tả |
|---|---|---|
| `daily_login` | 1 | Đăng nhập hàng ngày |
| `kill_monster` | 2 | Tiêu diệt quái vật |
| `complete_dungeon` | 3 | Hoàn thành phó bản |
| `level_up` | 4 | Nâng cấp nhân vật |
| `spend_gold` | 5 | Chi tiêu vàng |
| `join_guild` | 6 | Gia nhập bang hội |
| `create_guild` | 7 | Tạo bang hội |
| `arena_win` | 8 | Thắng đấu trường |
| `trial_complete` | 9 | Hoàn thành ải |

---

## 🚨 Lưu ý quan trọng

### Tại sao analytics làm intermediary?
1. **Single Responsibility**: task-service chỉ quản lý progress, không quan tâm nguồn event
2. **Decoupling**: Thêm event source mới → chỉ cần sửa analytics, không đụng task-service
3. **Analytics có đủ context**: analytics đã xử lý event → biết cần tăng task key nào → publish luôn
4. **Tránh duplicate**: Trước đây cả analytics VÀ task-service đều consume cùng topic → 2 consumers cùng 1 event

### Idempotency
- `TaskProgressEvent.eventId` = UUID để downstream detect duplicate nếu cần
- task-service hiện tại chưa deduplicate → acceptable vì Kafka at-least-once là đủ cho game

### Nếu analytics-service down
- `task.progress.update` topic sẽ không có message mới
- Task progress sẽ không tăng trong thời gian analytics down
- → Analytics là **critical path** của task progression → cần monitor
