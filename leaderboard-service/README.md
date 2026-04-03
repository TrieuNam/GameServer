# Leaderboard Service

**Version**: 1.0.0  
**Phase**: P3 (Social)  
**Port**: 8480  
**Database**: `game_leaderboard`

---

## 📋 Tổng quan

Leaderboard Service quản lý toàn bộ bảng xếp hạng game bao gồm power, level, arena, wealth, guild, pet, mount, và PVP rankings. Sử dụng Redis cho caching hiệu suất cao với tự động refresh mỗi 5 phút.

### Core Features
- ✅ 8 loại xếp hạng
- ✅ Top 100 người chơi mỗi bảng xếp hạng
- ✅ Cập nhật hạng thời gian thực
- ✅ Theo dõi thay đổi hạng (lên/xuống)
- ✅ Redis caching (TTL 5 phút)
- ✅ Tự động refresh mỗi 5 phút
- ✅ Tra cứu hạng cá nhân của người chơi

---

## 🏆 Loại Xếp Hạng

| Type | ID | Mô tả | Chỉ số điểm |
|------|----|----|-------------|
| **Power** | 1 | Sức mạnh chiến đấu | Tổng power |
| **Level** | 2 | Cấp độ nhân vật | Level + EXP |
| **Arena** | 3 | Xếp hạng đấu trường | Điểm arena |
| **Wealth** | 4 | Người chơi giàu nhất | Gold + Gems |
| **Guild** | 5 | Xếp hạng guild | Guild power |
| **Pet** | 6 | Xếp hạng pet | Pet power |
| **Mount** | 7 | Xếp hạng mount | Mount power |
| **PVP Kills** | 8 | Số lần kill | Tổng số kills |

---

## 🗄️ Database Schema

### ranking_entry
```sql
CREATE TABLE ranking_entry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ranking_type INT NOT NULL, -- 1-8
    role_id VARCHAR(50) NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    role_level INT NOT NULL,
    score BIGINT DEFAULT 0,
    current_rank INT,
    previous_rank INT,
    guild_name VARCHAR(50),
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_type_role (ranking_type, role_id)
);
```

---

## 🔌 API Endpoints

```
POST   /api/leaderboard/update              - Update player score
GET    /api/leaderboard/{rankingType}       - Get leaderboard (top 100); optional ?roleId= to include my rank
POST   /api/leaderboard/refresh             - Manual refresh all
GET    /api/leaderboard/health              - Health check
```

---

## 📦 API Examples

### Cập Nhật Điểm
```bash
curl -X POST http://localhost:8480/api/leaderboard/update \
  -H "Content-Type: application/json" \
  -d '{
    "rankingType": 1,
    "roleId": "player123",
    "roleName": "DragonSlayer",
    "roleLevel": 85,
    "score": 125000,
    "guildName": "Legends"
  }'
```

### Lấy Bảng Xếp Hạng Power
```bash
curl http://localhost:8480/api/leaderboard/1
```

### Lấy Bảng Xếp Hạng Kèm Hạng Của Tôi
```bash
curl "http://localhost:8480/api/leaderboard/1?roleId=player123"
```

### Refresh Thủ Công
```bash
curl -X POST http://localhost:8480/api/leaderboard/refresh
```

---

## 🔧 Business Logic

### Tính Toán Xếp Hạng
- Top 100 người chơi mỗi loại xếp hạng
- Sắp xếp theo score DESC, updatedAt ASC
- Theo dõi thay đổi hạng (trước vs hiện tại)
- Cập nhật thời gian thực khi điểm thay đổi

### Chiến Lược Caching
- Redis cache: TTL 5 phút
- Tự động refresh: Mỗi 5 phút qua @Scheduled
- Xóa cache khi cập nhật điểm
- Hot data trong memory cho queries nhanh

### Chỉ Báo Thay Đổi Hạng
- **Số dương**: Hạng cải thiện (vd: +5 nghĩa là lên 5 hạng)
- **Số âm**: Hạng giảm (vd: -3 nghĩa là xuống 3 hạng)
- **Số không**: Không thay đổi

---

## 🚀 Running

```bash
cd GameServer/leaderboard-service
mvn clean install
mvn spring-boot:run
```

---

## 📊 Statistics

```
Entities:        1 class
Repositories:    1 interface
DTOs:            1 file (4 DTO classes)
Services:        1 class
Controllers:     1 class
━━━━━━━━━━━━━━━━━━━━━━━━━━━
TOTAL:          5 files ~1,000 lines
```

---

## 🔗 Integration Points

### Nguồn Dữ Liệu
- **role-service**: Cập nhật power, level
- **arena-service**: Điểm arena
- **wallet-service**: Tính toán wealth
- **guild-service**: Xếp hạng guild
- **pet-service**: Pet power
- **mount-service**: Mount power

### WebSocket Handler
- MSGID_1501_RANK_REQ

---

## ⚡ Hiệu Suất

- **Redis caching**: Đọc dưới millisecond
- **Chỉ Top 100**: Tập dữ liệu giới hạn
- **Tự động refresh**: Cập nhật nền
- **Indexed queries**: Sắp xếp nhanh theo score

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22

