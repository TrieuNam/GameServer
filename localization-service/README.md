# Localization Service

**Version**: 1.0.0  
**Phase**: P3 (Enhancement & Support)  
**Port**: 8560 · **gRPC**: 9560  
**Database**: N/A (Redis db:6)

---

## 📋 Overview

Localization Service cung cấp **đa ngôn ngữ và bản địa hóa (i18n/l10n)** cho toàn bộ game — strings, UI text, item names, error messages theo từng ngôn ngữ. Data được cache trong Redis, không cần MySQL.

### Core Features
- ✅ Cung cấp bản dịch theo ngôn ngữ
- ✅ Quản lý string bundles
- ✅ Hỗ trợ nhiều ngôn ngữ
- ✅ gRPC server (port 9560)
- ✅ Redis cache cho performance
- ✅ Hot-reload translations

---

## 🎯 Flow Localization

```
[Service cần text cho player]
bất kỳ service nào ──► localization-service (Feign/gRPC)
                                │
                        Translate(key, language)
                        ├── Check Redis cache: "i18n:{lang}:{key}"
                        ├── Nếu hit → trả về string
                        └── Nếu miss → load từ bundle file → cache → trả về
```

---

## 🗄️ Storage (Redis db:6)

```
# Translation cache
i18n:{language}:{key} → translated_string
i18n:vi:ITEM_SWORD_001_NAME → "Kiếm Sắt"
i18n:en:ITEM_SWORD_001_NAME → "Iron Sword"
i18n:zh:ITEM_SWORD_001_NAME → "铁剑"

# Bundle cache (all strings for a language)
i18n:bundle:{language} → { key: value, ... }
TTL: 1 giờ
```

---

## 🌍 Supported Languages

| Code | Ngôn ngữ |
|------|---------|
| `vi` | Tiếng Việt |
| `en` | English |
| `zh` | 中文 (Simplified) |
| `ko` | 한국어 |
| `ja` | 日本語 |
| `th` | ภาษาไทย |

---

## 🔌 API Endpoints

```
GET   /api/i18n/translate         - Dịch một key (query params: key, lang)
GET   /api/i18n/all/{language}    - Lấy tất cả strings của ngôn ngữ
```

---

## 📦 API Examples

### Dịch Key
```bash
curl -X POST http://localhost:8560/api/i18n/translate \
  -H "Content-Type: application/json" \
  -d '{
    "key": "ITEM_SWORD_001_NAME",
    "language": "vi",
    "params": {}
  }'
# Response: { "key": "ITEM_SWORD_001_NAME", "value": "Kiếm Sắt" }
```

### Lấy Toàn Bộ Bundle
```bash
curl http://localhost:8560/api/i18n/all/vi
# Response: { "ITEM_SWORD_001_NAME": "Kiếm Sắt", "SKILL_FIRE_NAME": "Cầu Lửa", ... }
```

---

## 🔧 Business Logic

### String Key Naming Convention
```
ITEM_{itemId}_NAME          → Tên item
ITEM_{itemId}_DESC          → Mô tả item
SKILL_{skillId}_NAME        → Tên skill
ERROR_{errorCode}           → Error message
UI_{component}_{label}      → UI text
NPC_{npcId}_DIALOG_001      → NPC dialogue
```

### Hot Reload
- Khi file bundle được cập nhật → POST /api/i18n/reload
- Xóa Redis cache → service tự load lại

---

## 🚀 Running

```bash
cd GameServer/localization-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### gRPC Server (port 9560)
- webSocket-server: Translate game messages trước khi gửi về client

### Được gọi bởi
- webSocket-server, notification-service, mail-service

---

## 📊 Statistics

```
Storage:         Redis db:6 (không có MySQL)
Languages:       6+ ngôn ngữ
Controllers:     1 class (LocalizationController)
Services:        1 class (LocalizationService)
gRPC:            LocalizationGrpcImpl
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          ~400 lines
```

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22

