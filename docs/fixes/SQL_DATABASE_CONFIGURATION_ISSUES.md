# 🔴 BÁO CÁO VẤN ĐỀ CẤU HÌNH DATABASE VÀ SQL

**Ngày:** 01/02/2026  
**Trạng thái:** CẦN SỬA NGAY

---

## 📋 TÓM TẮT VẤN ĐỀ

Phát hiện 4 vấn đề nghiêm trọng trong cấu hình database:

1. ❌ **Port MySQL không nhất quán** - Services dùng nhiều port khác nhau
2. ❌ **Password không khớp** - Docker vs Application configs khác nhau
3. ❌ **Username không khớp** - `liner_user` vs `root`
4. ❌ **Database names không tồn tại** - Nhiều DB riêng lẻ chưa được tạo

---

## 🔍 CHI TIẾT VẤN ĐỀ

### **1. PORT CONFLICT - Các service dùng port khác nhau**

#### Docker Compose chỉ expose:
```yaml
mysql:
  ports:
    - "3306:3306"  # ✅ Chỉ có 1 port được expose
```

#### Nhưng các services lại kết nối đến nhiều port:

| Service | Port | Database | File |
|---------|------|----------|------|
| guild-service | **3306** | guild_db | `guild-service/application.yml` |
| friend-service | **3306** | friend_db | `friend-service/application.yml` |
| leaderboard-service | **3306** | leaderboard_db | `leaderboard-service/application.yml` |
| user-service | **33062** ❌ | user_db | `user-service/application.yml` |
| report-service | **33063** ❌ | report_db | `report-service/application.yml` |
| wallet-service | **33064** ❌ | wallet_db | `wallet-service/application.yml` |
| shop-service | **33068** ❌ | shop_db | `shop-service/application.yml` |

**❌ Port 33062, 33063, 33064, 33068 KHÔNG TỒN TẠI** - Chỉ có port 3306!

---

### **2. PASSWORD MISMATCH**

#### Docker Compose:
```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: root123      # ✅ Root password
    MYSQL_USER: liner_user
    MYSQL_PASSWORD: liner_pass         # ✅ User password
```

#### Application configs:
```yaml
# user-service/application.yml
datasource:
  username: root
  password: root                      # ❌ SAI! Phải là "root123"
```

```yaml
# guild-service/application.yml
datasource:
  username: root
  password: root                      # ❌ SAI! Phải là "root123"
```

**➡️ Tất cả services dùng password `root` nhưng Docker đặt là `root123`!**

---

### **3. USERNAME MISMATCH**

#### Docker tạo 2 users:
```yaml
MYSQL_ROOT_PASSWORD: root123
MYSQL_USER: liner_user              # ✅ User được tạo
MYSQL_PASSWORD: liner_pass
```

#### Nhưng application configs đều dùng:
```yaml
username: root                      # ❌ Không dùng liner_user
```

**Best Practice:** Nên dùng `liner_user` thay vì `root` cho security!

---

### **4. DATABASE NAMES - Multiple Databases Chưa Được Tạo**

#### Docker chỉ tạo:
```yaml
MYSQL_DATABASE: liner_game          # ✅ Chỉ có 1 database
```

#### Nhưng services cần:
- ❌ `user_db` (user-service)
- ❌ `guild_db` (guild-service)
- ❌ `friend_db` (friend-service)
- ❌ `mail_db` (mail-service)
- ❌ `chat_db` (chat-service)
- ❌ `leaderboard_db` (leaderboard-service)
- ❌ `wallet_db` (wallet-service)
- ❌ `shop_db` (shop-service)
- ❌ `report_db` (report-service)
- ❌ `game_starmap`, `game_trial`, `game_task`, `game_pet`, etc.

**➡️ Tất cả các database này CHƯA ĐƯỢC TẠO!**

---

## ✅ GIẢI PHÁP ĐỀ XUẤT

### **Option 1: Đơn Giản - Dùng 1 Database (Khuyến Nghị cho Dev)**

#### Ưu điểm:
- ✅ Đơn giản, dễ quản lý
- ✅ Không cần nhiều connection
- ✅ Phù hợp cho development

#### Các bước:

**1. Sửa Docker Compose** - GIỮ NGUYÊN hoặc đổi password:
```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: root        # ✅ Đổi thành "root" cho đơn giản
    MYSQL_DATABASE: liner_game
    MYSQL_USER: liner_user
    MYSQL_PASSWORD: liner_pass
```

**2. Sửa TẤT CẢ Application Configs:**
```yaml
datasource:
  url: jdbc:mysql://127.0.0.1:3306/liner_game     # ✅ Cùng 1 database
  username: root                                    # Hoặc liner_user
  password: root                                    # Hoặc liner_pass
```

**3. Tạo tables cho tất cả services trong cùng DB `liner_game`**

---

### **Option 2: Multiple Databases - Kiến Trúc Microservices (Khuyến Nghị cho Production)**

#### Ưu điểm:
- ✅ Database isolation
- ✅ Scale độc lập
- ✅ Security tốt hơn

#### Các bước:

**1. Tạo Init Script** - `sql/init.sql`:
```sql
-- Root password: root (hoặc root123)
CREATE DATABASE IF NOT EXISTS liner_game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS user_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS guild_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS friend_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mail_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS chat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS leaderboard_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS wallet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS report_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_starmap CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_trial CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_task CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_pet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_territory CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_rune CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_shizhuang CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_mount CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_moderation CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS game_gm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant privileges (nếu dùng liner_user)
GRANT ALL PRIVILEGES ON *.* TO 'liner_user'@'%';
FLUSH PRIVILEGES;

SHOW DATABASES;
```

**2. Update Docker Compose**:
```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: root           # ✅ Đổi thành "root"
    MYSQL_DATABASE: liner_game
  volumes:
    - mysql-data:/var/lib/mysql
    - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql   # ✅ Tạo tất cả DBs
```

**3. Sửa TẤT CẢ Application Configs - Đồng nhất password và port:**
```yaml
# Tất cả services:
datasource:
  url: jdbc:mysql://127.0.0.1:3306/<database_name>    # ✅ Port 3306
  username: root                                        # ✅ Đồng nhất
  password: root                                        # ✅ Đồng nhất
```

---

## 🎯 KHUYẾN NGHỊ CỤ THỂ

### **📌 Chuẩn hóa như sau:**

| Cấu hình | Giá trị | Ghi chú |
|----------|---------|---------|
| MySQL Port | `3306` | Tất cả services |
| Root Password | `root` | Đơn giản cho dev |
| Username | `root` | Dùng root cho dev, liner_user cho prod |
| Init Script | `sql/init.sql` | Tạo tất cả databases |

### **📝 Các Services Cần Sửa:**

#### Priority 1 - Core Services (SỬA NGAY):
- ✅ user-service: Port 33062 → 3306, password root → root
- ✅ session-service: Không cần DB (dùng Redis)
- ✅ role-service: Kiểm tra config

#### Priority 2 - Economy Services:
- ✅ wallet-service: Port 33064 → 3306, password
- ✅ shop-service: Port 33068 → 3306, password

#### Priority 3 - Social Services:
- ✅ guild-service: Password đúng
- ✅ friend-service: Password đúng
- ✅ mail-service: Password đúng
- ✅ chat-service: Password đúng
- ✅ leaderboard-service: Password đúng

#### Priority 4 - Game Services:
- ✅ report-service: Port 33063 → 3306, password

---

## 🛠️ CÁC BƯỚC THỰC HIỆN

### **Bước 1: Tạo SQL Init Script** ✅
```bash
# Tạo file sql/init.sql với tất cả CREATE DATABASE statements
```

### **Bước 2: Update Docker Compose** ✅
```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: root
```

### **Bước 3: Update Application Configs** ✅
```bash
# Sửa tất cả application.yml:
# - Port: 3306
# - Username: root
# - Password: root
```

### **Bước 4: Restart Docker**
```bash
docker-compose down -v          # Xóa volumes cũ
docker-compose up -d mysql      # Khởi động lại MySQL
docker-compose logs mysql       # Kiểm tra logs
```

### **Bước 5: Verify Databases**
```bash
docker exec -it liner-mysql mysql -u root -proot -e "SHOW DATABASES;"
```

---

## 🚀 TRIỂN KHAI TỰ ĐỘNG

Bạn có muốn tôi:
1. ✅ Tạo `sql/init.sql` với tất cả databases
2. ✅ Sửa `docker-compose.yml` 
3. ✅ Sửa tất cả `application.yml` files
4. ✅ Tạo script kiểm tra `verify-db-config.ps1`

**Chỉ cần xác nhận và tôi sẽ thực hiện tất cả!**

---

## 📊 DANH SÁCH FILE CẦN SỬA

### Docker & SQL:
- [ ] `docker-compose.yml` - Password root123 → root
- [ ] `sql/init.sql` - Tạo mới với tất cả DBs

### Application Configs - Sửa Port & Password:
- [ ] `user-service/src/main/resources/application.yml` - Port 33062 → 3306
- [ ] `wallet-service/src/main/resources/application.yml` - Port 33064 → 3306
- [ ] `report-service/src/main/resources/application.yml` - Port 33063 → 3306
- [ ] `shop-service/src/main/resources/application.yml` - Port 33068 → 3306
- [ ] `guild-service/src/main/resources/application.yml` - Password root → root
- [ ] `friend-service/src/main/resources/application.yml` - Password
- [ ] `mail-service/src/main/resources/application.yml` - Password
- [ ] `chat-service/src/main/resources/application.yml` - Password
- [ ] `leaderboard-service/src/main/resources/application.yml` - Password

**Tổng cộng:** ~20-25 files cần sửa

---

**🎯 BẠN MUỐN SỬA THEO OPTION 1 HAY OPTION 2?**
