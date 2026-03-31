# Database Config Audit Report
> Generated: 2026-02-28 | So sánh `application.yml` + `application-local.yml` vs `docker-compose-databases.yml`

---

## Tóm tắt

| Mức độ | Số lượng | Mô tả |
|--------|----------|-------|
| 🔴 CRITICAL | 8 | Port sai, DB name sai — service sẽ không start được |
| 🟡 MEDIUM | 15+ | Credentials không nhất quán (hardcode hoặc sai default) |
| 🟢 OK | ~20 | Cấu hình đúng |

---

## 🔴 CRITICAL — Cần fix ngay

### 1. wallet-service
| | Hiện tại | Đúng |
|--|----------|------|
| Port | `3329` | `33064` |
| DB name | `db_game_wallet` | `wallet_db` |
| File | `application.yml` hardcode `tpnam/121831` | Dùng env var |

### 2. serverInfo-service
| | Hiện tại | Đúng |
|--|----------|------|
| Port | `3310` | `33063` |
| DB name | `game_serverInfor` | `serverinfo_db` |

### 3. task-service
| | Hiện tại | Đúng |
|--|----------|------|
| Port | `3312` | `33074` |
| DB name | `task_db` | `game_task` |

### 4. bag-service
| | Hiện tại | Đúng |
|--|----------|------|
| Port | `3311` | `33067` |
| DB name | `db_bag` | `bag_db` |

### 5. pet-service
| | Hiện tại | Đúng |
|--|----------|------|
| Port | `3306` | `33077` |
| DB name | `db_pet_service` | `game_pet` |

### 6. report-service
| | Hiện tại | Đúng |
|--|----------|------|
| Port | `3309` | `33065` |
| DB name | `report_game_h2` | `report_db` |

### 7. battleserver-service
| | Hiện tại | Đúng |
|--|----------|------|
| Port | `3328` | `3328` (OK) |
| DB name | `db_bag_service` ← COPY-PASTE LỖI | `db_battle_service` |

### 8. pagoda-service (application-local.yml)
| | Hiện tại | Vấn đề |
|--|----------|--------|
| DB port | `3317` | Trùng với `globalserver-service` (cũng dùng `3317`) |

---

## 🟡 MEDIUM — Credentials không nhất quán

Các service sau đang dùng default `root/1234` thay vì `tpnam/121831` trong `application.yml`:

| Service | Default hiện tại | Nên là |
|---------|-----------------|--------|
| trial-service | `root/1234` | `tpnam/121831` |
| guild-service | `root/1234` | `tpnam/121831` |
| friend-service | `root/1234` | `tpnam/121831` |
| leaderboard-service | `root/1234` | `tpnam/121831` |
| notification-service | `root/1234` | `tpnam/121831` |
| moderation-service | `root/1234` | `tpnam/121831` |
| analytics-service | `root/1234` | `tpnam/121831` |
| anti-cheat-service | `root/1234` | `tpnam/121831` |
| main-fb-service | `root/1234` | `tpnam/121831` |
| escort-service | `root/1234` | `tpnam/121831` |
| guild-service | `root/1234` | `tpnam/121831` |
| gm-service | `DB_USER` (sai tên) | `DB_USERNAME` |

**admin-service:** `application-local.yml` dùng `tpnam/121831` thay vì `root/root`

**crafting-service:** `application-local.yml` dùng `tpnam/121831` thay vì `root/root`

---

## 🟢 OK — Cấu hình đúng

| Service | Port | DB Name | Env Var |
|---------|------|---------|---------|
| user-service | 33061 | user_db | ✅ |
| role-service | 33062 | db_role | ✅ |
| equip-service | 33068 | equip_db | ✅ |
| box-service | 33070 | box_db | ✅ |
| shop-service | 33069 | shop_db | ✅ |
| crafting-service | 33071 | crafting_db | ✅ |
| arena-service | 33072 | game_arena | ✅ |
| starmap-service | 33075 | game_starmap | ✅ |
| territory-service | 33076 | game_territory | ✅ |
| mount-service | 33078 | game_mount | ✅ |
| rune-service | 33079 | game_rune | ✅ |
| chat-service | 33080 | chat_db | ✅ |
| angel-service | 33082 | game_angel | ✅ |
| artifact-service | 33083 | game_artifact | ✅ |
| mail-service | 33086 | mail_db | ✅ |
| lingzhu-service | 3315 | lingzhudb | ✅ |
| knights-service | 3316 | knightsdb | ✅ |
| scroll-service | 3319 | scrolldb | ✅ |
| gem-service | 3320 | gemdb | ✅ |
| activity-service | 3321 | activitydb | ✅ |
| globalserver-service | 3317 | globalserver_service_db | ✅ |

---

## Stateless Services (không có DB — đúng)

- item-service
- drop-service
- gift-service
- world-service (không thấy datasource config)
- gameworld-service (không thấy datasource config)

---

## Checklist chuẩn cho mỗi service

```yaml
# application.yml — TEMPLATE CHUẨN
spring:
  profiles:
    active: local
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:PORT/DB_NAME?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true}
    username: ${DB_USERNAME:tpnam}
    password: ${DB_PASSWORD:121831}
    hikari:
      initialization-fail-timeout: -1
      connection-timeout: 30000
      validation-timeout: 5000
      leak-detection-threshold: 60000
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    properties:
      hibernate:
        jdbc.time_zone: UTC
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

```yaml
# application-local.yml — TEMPLATE CHUẨN
spring:
  datasource:
    url: jdbc:mysql://localhost:PORT/DB_NAME?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
    username: root
    password: root
```

---

## Port Reference (docker-compose-databases.yml)

| Service | Host Port | DB Name |
|---------|-----------|---------|
| user-service | 33061 | user_db |
| role-service | 33062 | db_role |
| serverInfo-service | 33063 | serverinfo_db |
| wallet-service | 33064 | wallet_db |
| report-service | 33065 | report_db |
| iap-verify-service | 33066 | iap_verify_db |
| bag-service | 33067 | bag_db |
| equip-service | 33068 | equip_db |
| shop-service | 33069 | shop_db |
| box-service | 33070 | box_db |
| crafting-service | 33071 | crafting_db |
| arena-service | 33072 | game_arena |
| trial-service | 33073 | game_trial |
| task-service | 33074 | game_task |
| starmap-service | 33075 | game_starmap |
| territory-service | 33076 | game_territory |
| pet-service | 33077 | game_pet |
| mount-service | 33078 | game_mount |
| rune-service | 33079 | game_rune |
| chat-service | 33080 | chat_db |
| shizhuang-service | 33081 | game_shizhuang |
| angel-service | 33082 | game_angel |
| artifact-service | 33083 | game_artifact |
| guild-service | 33084 | guild_db |
| friend-service | 33085 | friend_db |
| mail-service | 33086 | mail_db |
| leaderboard-service | 33087 | leaderboard_db |
| admin-service | 9088 | game_admin |
| gm-service | 33089 | game_gm |
| notification-service | 33090 | game_notification |
| moderation-service | 33091 | game_moderation |
| analytics-service | 33092 | game_analytics |
| anti-cheat-service | 33093 | game_anticheat |
| main-fb-service | 33094 | game_mainfb |
| escort-service | 33095 | game_escort |
| globalserver-service | 3317 | globalserver_service_db |
| battleserver-service | 3328 | db_battle_service |
| lingzhu-service | 3315 | lingzhudb |
| knights-service | 3316 | knightsdb |
| pagoda-service | ⚠️ CONFLICT | pagodadb (cần port mới) |
| scroll-service | 3319 | scrolldb |
| gem-service | 3320 | gemdb |
| activity-service | 3321 | activitydb |
