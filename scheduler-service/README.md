# Scheduler Service

**Version**: 1.0.0  
**Phase**: P3 (Enhancement & Support)  
**Port**: 8550  
**Database**: N/A (Redis db:5)

---

## 📋 Overview

Scheduler Service là **dịch vụ cron jobs tập trung** — thực hiện các tác vụ reset định kỳ (daily, weekly) bằng cách gọi reset endpoints của các game services. Dùng Redis để lưu trạng thái và tránh duplicate execution.

### Core Features
- ✅ Daily reset jobs (DailyResetJob) — 00:00 mỗi ngày
- ✅ Weekly reset jobs (WeeklyResetJob) — Thứ 2 00:00
- ✅ Manual trigger cho testing
- ✅ Job status tracking
- ✅ Distributed lock (Redis) — chống duplicate khi scale

---

## 🎯 Daily Reset Flow

```
[00:00 Server Time — Auto trigger]
scheduler-service (Spring @Scheduled)
        │
        ▼
DailyResetJob.execute()
├──► gift-service: POST /api/gift/reset-daily
├──► shop-service: POST /api/shop/reset-daily
├──► task-service: POST /api/task/reset-daily
├──► guild-service: POST /api/guild/reset-donation
└──► leaderboard-service: POST /api/leaderboard/refresh
        │
        ▼
Log kết quả vào Redis: "daily_reset_status:2026-03-16" → "success"
```

---

## 🗄️ Storage (Redis db:5)

```
# Job execution log
scheduler:daily:{date} → { status, executedAt, results }
scheduler:weekly:{week} → { status, executedAt, results }

# Distributed lock (chống duplicate)
scheduler:lock:daily → acquired (TTL 30 phút)
scheduler:lock:weekly → acquired (TTL 1 giờ)
```

---

## 🔌 API Endpoints

```
POST  /api/scheduler/trigger/daily-reset    - Manual trigger daily reset
POST  /api/scheduler/trigger/weekly-reset   - Manual trigger weekly reset
GET   /api/scheduler/status                 - Xem status các jobs
```

---

## 📦 API Examples

### Manual Trigger Daily Reset
```bash
curl -X POST http://localhost:8550/api/scheduler/trigger/daily-reset \
  -H "Authorization: Bearer {admin-token}"
```

### Xem Status Jobs
```bash
curl http://localhost:8550/api/scheduler/status
# Response:
# {
#   "lastDailyReset": "2026-03-16T00:00:05",
#   "lastWeeklyReset": "2026-03-09T00:00:08",
#   "nextDailyReset": "2026-03-17T00:00:00",
#   "status": "OK"
# }
```

---

## ⚙️ Configuration

```yaml
server:
  port: 8550

spring:
  redis:
    database: 5      # Dedicated Redis DB cho scheduler

scheduler:
  daily-reset:
    cron: "0 0 0 * * *"     # 00:00:00 mỗi ngày
    timezone: "Asia/Ho_Chi_Minh"
  weekly-reset:
    cron: "0 0 0 * * MON"   # Thứ 2 00:00:00
```

---

## 🔧 Business Logic

### Distributed Execution
- Dùng Redis distributed lock để đảm bảo chỉ 1 instance chạy job
- Nếu lock bị timeout (service crash) → tự release sau TTL
- Retry 3 lần nếu 1 service con fail

### Job Target Services
| Job | Services |
|-----|---------|
| Daily Reset | gift, shop, task, guild, leaderboard |
| Weekly Reset | guild, leaderboard, shop (weekly limits) |

---

## 🚀 Running

```bash
cd GameServer/scheduler-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Gọi (Feign)
- **gift-service**: Daily gift reset
- **guild-service**: Daily donation reset
- **leaderboard-service**: Refresh rankings
- **shop-service**: Daily/weekly purchase limit reset
- **task-service**: Daily/weekly task reset

---

## 📊 Statistics

```
Storage:         Redis db:5 (không có MySQL)
Jobs:            2 (DailyResetJob, WeeklyResetJob)
Controllers:     1 class (SchedulerController)
Services:        1 class (SchedulerService)
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22

