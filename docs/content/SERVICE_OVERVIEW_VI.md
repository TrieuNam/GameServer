# Game Server - Tổng Quan Các Service (53 Services)

**Phiên bản:** 1.0.0  
**Ngày:** 02/02/2026  
**Trạng thái Build:** ✅ Tất cả 53 modules đã compile thành công  
**Tổng thời gian Build:** 19:01 phút  
**Spring Boot:** 3.5.3  
**Java:** 21 (Virtual Threads đã kích hoạt)

---

## Tóm Tắt Kiến Trúc

- **Tổng số Modules:** 53 (1 common-lib + 1 parent pom + 51 services)
- **Services có Database:** 36 services (mỗi service có MySQL riêng)
- **Stateless Services:** 15 services (chỉ dùng Redis/Kafka/Config)
- **Infrastructure Services:** 4 (Eureka, Gateway, Config, WebSocket)
- **gRPC Services:** 15 services
- **Kafka Producers/Consumers:** 15 services
- **Redis Cache:** 10+ services

---

## Phân Loại Services

### 📦 1. Thư Viện Dùng Chung (1 module)

#### **common-lib** 
**Thứ tự Build:** [1/53] | **Thời gian Build:** 01:30 phút

**Mục đích:** Thư viện chia sẻ chứa các tiện ích, DTOs và định nghĩa Protocol Buffers

**Các thành phần chính:**
- **Protocol Buffers:** 58 file .proto được compile thành Java + gRPC stubs
- **Common DTOs:** Các đối tượng truyền dữ liệu dùng chung cho tất cả services
- **Utility Classes:** Hàm helper, validators, converters
- **gRPC Definitions:** Các interface service cho giao tiếp giữa các services
- **Annotations:** Các annotation tùy chỉnh cho validation và logging

**Dependencies:**
- Spring Boot Starter Web
- Spring Cloud Eureka Client
- Thư viện gRPC Protobuf & Stub
- Lombok để sinh code tự động
- Jackson để xử lý JSON

**Được sử dụng bởi:** Tất cả 51 services đều phụ thuộc vào thư viện này

---

## 🏗️ 2. Infrastructure Services (Giai đoạn P0)

### **eureka-server**
**Thứ tự Build:** [2/53] | **Port:** 8761 | **Thời gian Build:** 11.238s

**Mục đích:** Service Discovery & Registry (Netflix Eureka)

**Tính năng chính:**
- Đăng ký và khám phá services
- Giám sát health check
- Hỗ trợ load balancing
- Dashboard UI tại http://localhost:8761

**Database:** Không có  
**Cấu hình:** Self-registration enabled  
**Độ ưu tiên khởi động:** 1 (Phải khởi động đầu tiên)

---

### **gateway-service**
**Thứ tự Build:** [3/53] | **Port:** 8083 | **Thời gian Build:** 01:53 phút

**Mục đích:** API Gateway & Routing (Spring Cloud Gateway)

**Tính năng chính:**
- Điểm vào thống nhất cho tất cả requests từ client
- Chuyển tiếp routes đến các microservices
- Xác thực JWT token
- Rate limiting và throttling
- Cấu hình CORS
- Logging request/response

**Database:** Không có  
**Dependencies:** Eureka Client, Spring Cloud Gateway  
**Độ ưu tiên khởi động:** 2 (Sau Eureka)

**Cấu hình Routes:**
- `/api/user/**` → user-service
- `/api/wallet/**` → wallet-service
- `/api/shop/**` → shop-service
- `/api/guild/**` → guild-service
- Tất cả routes được resolve động qua Eureka

---

### **config-service**
**Thứ tự Build:** [4/53] | **Port:** 8888 | **Thời gian Build:** 24.595s

**Mục đích:** Cung cấp file cấu hình JSON/XML cho các services

**Tính năng chính:**
- Phục vụ file cấu hình JSON cho các services
- Phục vụ file cấu hình XML cho các services
- Lưu trữ cấu hình tĩnh (template vật phẩm, bảng rơi, v.v.)
- API đọc file cấu hình theo tên
- Cache file cấu hình để tăng hiệu suất

**Database:** Không có  
**Storage:** File system (chứa các file JSON/XML cấu hình)  
**Độ ưu tiên khởi động:** 2 (Sau Eureka, song song với Gateway)

---

### **webSocket-server**
**Thứ tự Build:** [5/53] | **Port:** 8094 | **Thời gian Build:** 22.076s

**Mục đích:** Server giao tiếp realtime hai chiều

**Tính năng chính:**
- Messaging nhị phân Protocol Buffers
- Quản lý phiên người chơi (lưu trên Redis)
- Định tuyến tin nhắn đến handlers
- Connection pooling
- Heartbeat & tự động kết nối lại
- Nén tin nhắn

**Database:** Không có  
**Storage:** Redis (sessions người chơi, registry)  
**Message Queue:** Kafka (async events)  
**Độ ưu tiên khởi động:** 3 (Sau Gateway)

**Hỗ trợ Handlers:**
- Handlers đăng nhập/đăng xuất
- Định tuyến tin nhắn chat
- Thông báo realtime
- Cập nhật vị trí người chơi
- Sự kiện chiến đấu

---

## 👤 3. Core Services (Giai đoạn P0 - Quản lý người dùng)

### **session-service**
**Thứ tự Build:** [6/53] | **Port:** 8096 | **Thời gian Build:** 15.836s

**Mục đích:** Quản lý JWT Session & Token

**Tính năng chính:**
- Tạo và xác thực JWT token
- Cơ chế refresh token
- Theo dõi token hết hạn
- Lưu trữ session trên Redis
- Blacklist cho các token bị thu hồi

**Database:** Không có  
**Storage:** Redis (DB 0)  
**Token TTL:** 2 giờ (access), 7 ngày (refresh)

---

### **user-service**
**Thứ tự Build:** [7/53] | **Port:** 8400 | **gRPC:** Chưa cấu hình | **Thời gian Build:** 19.504s

**Mục đích:** Quản lý tài khoản người dùng & xác thực

**Tính năng chính:**
- Đăng ký & đăng nhập người dùng
- Mã hóa mật khẩu (BCrypt)
- Quản lý trạng thái tài khoản
- CRUD thông tin hồ sơ người dùng
- Xác minh email/điện thoại

**Database:** MySQL Port **33062** (`user_db`)  
**Tables:** users, user_profiles, login_history  
**Flyway Migrations:** ✅ Đã bật  
**Credentials:** tpnam/121831

**API Endpoints:**
- `POST /api/user/register` - Đăng ký người dùng mới
- `POST /api/user/login` - Đăng nhập
- `GET /api/user/{id}` - Lấy thông tin người dùng
- `PUT /api/user/{id}` - Cập nhật thông tin

---

### **role-service**
**Thứ tự Build:** [26/53] | **Port:** 8410 | **gRPC:** 9090 | **Thời gian Build:** 22.437s

**Mục đích:** Quản lý nhân vật người chơi

**Tính năng chính:**
- Tạo và xóa nhân vật
- Thuộc tính nhân vật (level, exp, stats)
- Quản lý trang bị
- Theo dõi tiến trình kỹ năng
- Tùy chỉnh ngoại hình nhân vật

**Database:** MySQL Port **3319** (`db_role`)  
**Tables:** roles, role_attributes, role_equipment  
**gRPC Services:** RoleService (get/update dữ liệu nhân vật)  
**Flyway Migrations:** ✅ Đã bật

**API Endpoints:**
- `POST /api/role/create` - Tạo nhân vật mới
- `GET /api/role/{roleId}` - Lấy chi tiết nhân vật
- `PUT /api/role/{roleId}/level` - Nâng cấp nhân vật

---

### **serverInfo-service**
**Thứ tự Build:** [43/53] | **Port:** 8420 | **Thời gian Build:** 19.735s

**Mục đích:** Thông tin & trạng thái game server

**Tính năng chính:**
- Quản lý danh sách server
- Giám sát công suất server
- Kiểm soát chế độ bảo trì
- Thông tin cân bằng tải server

**Database:** MySQL Port **3318** (`serverinfo_db`)  
**Tables:** server_info, server_status  
**Flyway Migrations:** ✅ Đã bật

---

## 💰 4. Economy Services (Giai đoạn P1)

### **wallet-service**
**Thứ tự Build:** [9/53] | **Port:** 8200 | **Thời gian Build:** 20.101s

**Mục đích:** Quản lý tiền tệ & ví ảo người chơi

**Tính năng chính:**
- Hỗ trợ đa loại tiền tệ (vàng, kim cương, kim cương khóa)
- Theo dõi lịch sử giao dịch
- Xác thực số dư
- Chuyển đổi tiền tệ
- Áp dụng giới hạn hàng ngày

**Database:** MySQL Port **33064** (`wallet_db`)  
**Tables:** wallets, wallet_transactions  
**Flyway Migrations:** ✅ Đã bật

**API Endpoints:**
- `GET /api/wallet/{userId}` - Lấy số dư ví
- `POST /api/wallet/add` - Thêm tiền tệ
- `POST /api/wallet/deduct` - Trừ tiền tệ
- `GET /api/wallet/history` - Lịch sử giao dịch

---

### **shop-service**
**Thứ tự Build:** [14/53] | **Port:** 8260 | **gRPC:** 9089 | **Thời gian Build:** 18.122s

**Mục đích:** Cửa hàng trong game & mua vật phẩm

**Tính năng chính:**
- Quản lý danh mục cửa hàng
- Xử lý mua vật phẩm
- Ưu đãi giới hạn thời gian
- Truy cập cửa hàng VIP
- Lịch sử mua hàng

**Database:** MySQL Port **33068** (`shop_db`)  
**Tables:** shop_items, purchases, shop_history  
**gRPC Services:** ShopService (xác thực mua hàng)  
**Flyway Migrations:** ✅ Đã bật

---

### **bag-service**
**Thứ tự Build:** [11/53] | **Port:** 8230 | **gRPC:** 9087 | **Thời gian Build:** 23.820s

**Mục đích:** Quản lý túi đồ (inventory) người chơi

**Tính năng chính:**
- Quản lý ô túi đồ
- Xếp chồng vật phẩm
- Sắp xếp và lọc vật phẩm
- Nâng cấp dung lượng túi
- Theo dõi vật phẩm hết hạn

**Database:** MySQL Port **33065** (`bag_db`)  
**Tables:** bag_items, bag_slots  
**gRPC Services:** BagService (thao tác vật phẩm)  
**Kafka:** Producer/Consumer đã bật  
**Flyway Migrations:** ✅ Đã bật

---

### **equip-service**
**Thứ tú Build:** [12/53] | **Port:** 8240 | **gRPC:** 9088 | **Thời gian Build:** 18.204s

**Mục đích:** Quản lý & nâng cấp trang bị

**Tính năng chính:**
- Trang bị/gỡ bỏ trang bị
- Nâng cấp trang bị (tăng cấp độ)
- Hệ thống đính ngọc
- Độ bền trang bị
- Tính toán bonus bộ trang bị

**Database:** MySQL Port **33066** (`equip_db`)  
**Tables:** equipment, equipment_enhancements  
**gRPC Services:** EquipService (thao tác trang bị)  
**Flyway Migrations:** ✅ Đã bật

---

### **item-service**
**Thứ tự Build:** [10/53] | **Port:** 8330 | **Thời gian Build:** 14.194s

**Mục đích:** Service cấu hình & template vật phẩm

**Tính năng chính:**
- Định nghĩa template vật phẩm (read-only)
- Tra cứu thuộc tính vật phẩm
- Quy tắc sử dụng vật phẩm
- Thông tin độ hiếm và chất lượng
- Cấu hình tỷ lệ rơi

**Database:** Không có (chỉ cache, load từ JSON/XML)  
**Storage:** Redis cache + File system  
**Nguồn dữ liệu:** File cấu hình tĩnh

---

### **drop-service**
**Thứ tự Build:** [13/53] | **Port:** 8250 | **Thời gian Build:** 14.887s

**Mục đích:** Tính toán bảng rơi vật phẩm (loot generation)

**Tính năng chính:**
- Tính toán xác suất rơi
- Sinh loot ngẫu nhiên
- Cấu hình bảng rơi
- Modifier tăng tỷ lệ rơi
- Boost rơi theo sự kiện

**Database:** Không có (Stateless)  
**Storage:** Load bảng rơi từ file cấu hình XML  
**Thuật toán:** Lựa chọn ngẫu nhiên có trọng số

---

### **box-service**
**Thứ tự Build:** [17/53] | **Port:** 8290 | **Thời gian Build:** 19.739s

**Mục đích:** Quản lý rương báu & hộp quà

**Tính năng chính:**
- Cơ chế mở rương
- Công khai xác suất hộp quà
- Theo dõi inventory hộp
- Hộp sự kiện đặc biệt
- Hệ thống phần thưởng đảm bảo

**Database:** MySQL Port **33071** (`box_db`)  
**Tables:** boxes, box_openings, box_rewards  
**Flyway Migrations:** ✅ Đã bật

---

### **gift-service**
**Thứ tự Build:** [15/53] | **Port:** 8270 | **Thời gian Build:** 11.357s

**Mục đích:** Hệ thống mã quà tặng & đổi thưởng

**Tính năng chính:**
- Sinh mã quà tặng
- Xác thực đổi mã
- Tạo mã hàng loạt
- Quản lý hết hạn mã
- Lịch sử đổi thưởng

**Database:** Không có (Stateless)  
**Storage:** Redis cache để xác thực mã  
**Nguồn cấu hình:** File JSON

---

### **report-service**
**Thứ tự Build:** [8/53] | **Port:** 8210 | **Thời gian Build:** 24.303s

**Mục đích:** Hệ thống báo cáo & phản hồi người chơi

**Tính năng chính:**
- Gửi báo cáo bug
- Báo cáo hành vi người chơi
- Theo dõi trạng thái báo cáo
- Giao diện xem xét cho admin
- Phân loại báo cáo tự động

**Database:** MySQL Port **33063** (`report_db`)  
**Tables:** reports, report_comments  
**Flyway Migrations:** ✅ Đã bật

---

### **iap-verify-service**
**Thứ tự Build:** [51/53] | **Port:** 8220 | **Thời gian Build:** 22.678s

**Mục đích:** Xác minh mua hàng trong ứng dụng (iOS/Android/Web)

**Tính năng chính:**
- Xác minh biên lai Apple App Store
- Xác minh Google Play billing
- Xác minh thanh toán Steam
- Validation biên lai
- Phát hiện gian lận mua hàng

**Database:** MySQL Port **3357** (`iap_verify_db`)  
**Tables:** iap_receipts, iap_transactions  
**Flyway Migrations:** ✅ Đã bật

**API bên ngoài:**
- Apple StoreKit API
- Google Play Billing API
- Steam Web API

---

### **crafting-service**
**Thứ tự Build:** [16/53] | **Port:** 8280 | **gRPC:** 9099 | **Thời gian Build:** 19.454s

**Mục đích:** Hệ thống chế tạo & tổng hợp vật phẩm

**Tính năng chính:**
- Quản lý công thức
- Xác thực nguyên liệu chế tạo
- Xác suất thành công chế tạo
- Chế tạo chí mạng (phần thưởng bonus)
- Hệ thống hàng đợi chế tạo

**Database:** MySQL Port **33070** (`crafting_db`)  
**Tables:** recipes, crafting_history  
**gRPC Services:** CraftingService  
**Flyway Migrations:** ✅ Đã bật

---

## ⚔️ 5. Combat & Battle Services (Giai đoạn P2)

### **arena-service**
**Thứ tự Build:** [19/53] | **Port:** 8084 | **gRPC:** 9370 | **Thời gian Build:** 23.526s

**Mục đích:** PvP Arena & trận đấu xếp hạng

**Tính năng chính:**
- Ghép trận arena
- Tính toán hạng (hệ thống ELO)
- Phân phối phần thưởng arena
- Theo dõi lịch sử trận đấu
- Quản lý mùa giải

**Database:** MySQL Port **33072** (`game_arena`)  
**Tables:** arena_ranks, arena_matches, arena_rewards  
**gRPC Services:** ArenaService  
**Flyway Migrations:** ✅ Đã bật

---

### **battleserver-service**
**Thứ tự Build:** [20/53] | **Port:** 8082 | **gRPC:** 9092 | **Thời gian Build:** 19.233s

**Mục đích:** Engine tính toán chiến đấu realtime

**Tính năng chính:**
- Quản lý trạng thái chiến đấu
- Engine tính toán kỹ năng
- Tính toán sát thương
- Xử lý buff/debuff
- AI chiến đấu cho PvE

**Database:** Không có (Stateless)  
**Storage:** Redis (phiên chiến đấu với TTL 30 phút)  
**Message Queue:** Kafka (sự kiện chiến đấu)  
**gRPC Services:** BattleService

**Luồng chiến đấu:**
1. Khởi tạo trạng thái chiến đấu trong Redis
2. Xử lý yêu cầu kỹ năng/tấn công
3. Tính toán sát thương & hiệu ứng
4. Publish sự kiện lên Kafka
5. Trả về kết quả chiến đấu

---

### **trial-service**
**Thứ tự Build:** [22/53] | **Port:** 8094 | **gRPC:** 9094 | **Thời gian Build:** 21.788s

**Mục đích:** Dungeons thử thách & stages

**Tính năng chính:**
- Tiến trình stages thử thách
- Giới hạn lượt thử hàng ngày
- Tính toán phần thưởng thử thách
- Xếp hạng bảng xếp hạng
- Lên lịch reset thử thách

**Database:** MySQL Port **33073** (`game_trial`)  
**Tables:** trial_progress, trial_records  
**gRPC Services:** TrialService  
**Flyway Migrations:** ✅ Đã bật

---

### **territory-service**
**Thứ tự Build:** [23/53] | **Port:** 8122 | **Thời gian Build:** 18.513s

**Mục đích:** Chiếm lĩnh lãnh thổ & chiến tranh bang hội

**Tính năng chính:**
- Theo dõi quyền sở hữu lãnh thổ
- Lên lịch chiến đấu bang hội
- Sản xuất tài nguyên lãnh thổ
- Phần thưởng chiếm lĩnh
- Hệ thống nâng cấp lãnh thổ

**Database:** MySQL Port **33076** (`game_territory`)  
**Tables:** territories, territory_battles  
**Flyway Migrations:** ✅ Đã bật

---

### **escort-service**
**Thứ tự Build:** [24/53] | **Port:** 8129 | **Thời gian Build:** 15.798s

**Mục đích:** Nhiệm vụ hộ tống & hệ thống vận tiêu

**Tính năng chính:**
- Sinh nhiệm vụ hộ tống
- Quản lý tuyến đường vận tiêu
- Cơ chế cướp tiêu
- Tính toán phần thưởng hộ tống
- Cơ chế bảo vệ

**Database:** MySQL Port **33095** (`game_escort`)  
**Tables:** escort_missions, escort_history  
**Flyway Migrations:** ✅ Đã bật

---

### **world-service**
**Thứ tự Build:** [18/53] | **Port:** 8084 | **Thời gian Build:** 19.689s

**Mục đích:** Sự kiện thế giới & spawn boss thế giới

**Tính năng chính:**
- Lên lịch world boss
- Quản lý sự kiện toàn cầu
- Theo dõi tham gia sự kiện
- Xếp hạng sát thương world boss
- Phân phối phần thưởng sự kiện

**Database:** MySQL Port **33096** (`game_world`)  
**Tables:** world_events, world_bosses, world_state  
**Dependencies:** Spring Data JPA, MySQL Connector (✅ Đã fix trong build này)  
**Flyway Migrations:** ✅ Đã bật

**Fix gần đây:** Đã thêm dependencies JPA còn thiếu để giải quyết lỗi compilation

---

### **gameworld-service**
**Thứ tự Build:** [21/53] | **Port:** 8105 | **gRPC:** 9095 | **Thời gian Build:** 18.209s

**Mục đích:** Theo dõi vị trí người chơi & quản lý zone

**Tính năng chính:**
- Theo dõi vị trí người chơi realtime
- Quản lý zone/map
- Tra cứu người chơi gần đó
- Xác thực vị trí
- Xác minh vị trí chống hack

**Database:** Không có (Stateless)  
**Storage:** Redis (vị trí người chơi với TTL 30 phút)  
**gRPC Services:** GameWorldService  
**Cấu trúc dữ liệu:** GeoHash để query không gian hiệu quả

---

### **globalserver-service**
**Thứ tự Build:** [25/53] | **Port:** 8100 | **Thời gian Build:** 19.262s

**Mục đích:** Điều phối server toàn cầu & tính năng liên server

**Tính năng chính:**
- Ghép trận liên server
- Bảng xếp hạng toàn cầu
- Thông báo toàn server
- Hệ thống bạn bè liên server
- Quản lý merge server

**Database:** Không có (Stateless)  
**Storage:** Redis + Kafka  
**Giao tiếp:** Kafka để messaging giữa các server

---

## 🎮 6. Progression Services (Giai đoạn P2)

### **task-service**
**Thứ tự Build:** [27/53] | **Port:** 8095 | **Thời gian Build:** 25.452s

**Mục đích:** Hệ thống nhiệm vụ & quest

**Tính năng chính:**
- Nhiệm vụ hàng ngày/tuần/thành tựu
- Theo dõi tiến trình nhiệm vụ
- Xác thực hoàn thành nhiệm vụ
- Phân phối phần thưởng
- Quản lý chuỗi nhiệm vụ

**Database:** MySQL Port **33074** (`game_task`)  
**Tables:** tasks, task_progress, task_rewards  
**Redis:** Cache tiến trình nhiệm vụ  
**Kafka:** Sự kiện hoàn thành nhiệm vụ  
**Flyway Migrations:** ✅ Đã bật

---

### **starmap-service**
**Thứ tự Build:** [33/53] | **Port:** 8120 | **Thời gian Build:** 18.511s

**Mục đích:** Hệ thống khám phá bản đồ sao

**Tính năng chính:**
- Mở khóa nút sao
- Tiến trình chòm sao
- Phần thưởng bản đồ sao
- Tính toán sức mạnh sao
- Nâng cấp nút

**Database:** MySQL Port **33075** (`game_starmap`)  
**Tables:** starmap_progress, starmap_nodes  
**Flyway Migrations:** ✅ Đã bật

---

## 🐾 7. Companion Services (Giai đoạn P2)

### **pet-service**
**Thứ tự Build:** [28/53] | **Port:** 8300 | **Thời gian Build:** 24.204s

**Mục đích:** Quản lý hệ thống thú cưng

**Tính năng chính:**
- Thu thập & tiến hóa thú cưng
- Quản lý thuộc tính thú cưng
- Hệ thống kỹ năng thú cưng
- Cho ăn & nâng cấp thú cưng
- Trang bị thú cưng

**Database:** MySQL Port **33077** (`game_pet`)  
**Tables:** pets, pet_attributes, pet_skills  
**Kafka:** Sự kiện thú cưng (tiến hóa, nâng cấp)  
**Flyway Migrations:** ✅ Đã bật

---

### **mount-service**
**Thứ tự Build:** [30/53] | **Port:** 8310 | **Thời gian Build:** 20.752s

**Mục đích:** Hệ thống cưỡi (mount)

**Tính năng chính:**
- Bộ sưu tập mount
- Nâng cấp & tiến hóa mount
- Bonus tốc độ mount
- Tùy chỉnh ngoại hình mount
- Hệ thống kỹ năng mount

**Database:** MySQL Port **33078** (`game_mount`)  
**Tables:** mounts, mount_attributes  
**Kafka:** Sự kiện mount  
**Flyway Migrations:** ✅ Đã bật

---

### **angel-service**
**Thứ tự Build:** [31/53] | **Port:** 8360 | **Thời gian Build:** 22.131s

**Mục đích:** Hệ thống thiên thần đồng hành

**Tính năng chính:**
- Triệu hồi & nâng cấp thiên thần
- Hiệu ứng ban phước thiên thần
- Hệ thống kỹ năng thiên thần
- Đường tiến hóa thiên thần
- Trang bị thiên thần

**Database:** MySQL Port **33082** (`game_angel`)  
**Tables:** angels, angel_attributes, angel_skills  
**Kafka:** Sự kiện thiên thần  
**Flyway Migrations:** ✅ Đã bật

---

## 💎 8. Enhancement Services (Giai đoạn P3)

### **rune-service**
**Thứ tự Build:** [34/53] | **Port:** 8320 | **Thời gian Build:** 21.094s

**Mục đích:** Hệ thống ngọc rune & khắc chữ

**Tính năng chính:**
- Thu thập & nâng cấp rune
- Quản lý ổ cắm rune
- Tính toán bonus bộ rune
- Cường hóa rune
- Hợp nhất rune

**Database:** MySQL Port **33079** (`game_rune`)  
**Tables:** runes, rune_sockets, rune_sets  
**Kafka:** Sự kiện rune  
**Flyway Migrations:** ✅ Đã bật

---

### **artifact-service**
**Thứ tự Build:** [32/53] | **Port:** 8370 | **Thời gian Build:** 21.285s

**Mục đích:** Thu thập & cường hóa artifact

**Tính năng chính:**
- Thu thập artifact
- Hệ thống leveling artifact
- Kích hoạt kỹ năng artifact
- Hiệu ứng cộng hưởng artifact
- Tiến hóa artifact

**Database:** MySQL Port **33083** (`game_artifact`)  
**Tables:** artifacts, artifact_levels  
**Kafka:** Sự kiện artifact  
**Flyway Migrations:** ✅ Đã bật

---

### **shizhuang-service**
**Thứ tự Build:** [29/53] | **Port:** 8350 | **Thời gian Build:** 17.046s

**Mục đích:** Hệ thống thời trang/trang phục (时装)

**Tính năng chính:**
- Bộ sưu tập trang phục
- Hệ thống nhuộm trang phục
- Cường hóa trang phục
- Tùy chỉnh ngoại hình
- Bonus bộ trang phục

**Database:** MySQL Port **33081** (`game_shizhuang`)  
**Tables:** costumes, costume_dyes, costume_collections  
**Flyway Migrations:** ✅ Đã bật

---

## 👥 9. Social Services (Giai đoạn P3)

### **chat-service**
**Thứ tự Build:** [37/53] | **Port:** 8470 | **Thời gian Build:** 4.898s

**Mục đích:** Hệ thống chat trong game

**Tính năng chính:**
- Chat đa kênh (thế giới, bang hội, riêng tư)
- Lưu trữ lịch sử chat
- Lọc từ ngữ tục tĩu
- Cấm & khóa chat
- Hỗ trợ emoji & sticker

**Database:** MySQL Port **33080** (`chat_db`)  
**Tables:** chat_messages, chat_channels, chat_bans  
**Redis:** Cache tin nhắn gần đây  
**Kafka:** Broadcasting tin nhắn  
**Flyway Migrations:** ✅ Đã bật

---

### **friend-service**
**Thứ tự Build:** [38/53] | **Port:** 8450 | **Thời gian Build:** 3.594s

**Mục đích:** Quản lý hệ thống bạn bè

**Tính năng chính:**
- Yêu cầu & chấp nhận kết bạn
- Quản lý danh sách bạn bè
- Theo dõi trạng thái online
- Gợi ý kết bạn
- Quản lý danh sách chặn

**Database:** MySQL Port **33085** (`friend_db`)  
**Tables:** friendships, friend_requests, blocked_users  
**Redis:** Cache trạng thái online  
**Flyway Migrations:** ✅ Đã bật

---

### **guild-service**
**Thứ tự Build:** [39/53] | **Port:** 8440 | **Thời gian Build:** 3.409s

**Mục đích:** Hệ thống quản lý bang hội

**Tính năng chính:**
- Tạo & giải tán bang hội
- Quản lý thành viên (vai trò, quyền hạn)
- Cấp độ & kinh nghiệm bang hội
- Kho bang hội
- Chiến đấu & chiến tranh bang hội

**Database:** MySQL Port **33084** (`guild_db`)  
**Tables:** guilds, guild_members, guild_applications  
**Kafka:** Sự kiện bang hội  
**Flyway Migrations:** ✅ Đã bật

---

### **mail-service**
**Thứ tự Build:** [41/53] | **Port:** 8460 | **Thời gian Build:** 16.101s

**Mục đích:** Hệ thống thư trong game

**Tính năng chính:**
- Phát thư hệ thống hàng loạt
- Thư giữa người chơi
- Đính kèm thư (vật phẩm, tiền tệ)
- Quản lý hết hạn thư
- Thao tác thư hàng loạt

**Database:** MySQL Port **33086** (`mail_db`)  
**Tables:** mails, mail_attachments, mail_read_status  
**Flyway Migrations:** ✅ Đã bật

---

### **leaderboard-service**
**Thứ tự Build:** [40/53] | **Port:** 8480 | **Thời gian Build:** 51.790s

**Mục đích:** Hệ thống xếp hạng & bảng xếp hạng toàn cầu

**Tính năng chính:**
- Nhiều loại bảng xếp hạng (level, sức mạnh, arena, v.v.)
- Cập nhật hạng realtime
- Theo dõi hạng lịch sử
- Bảng xếp hạng theo mùa
- Phần thưởng bảng xếp hạng

**Database:** MySQL Port **33087** (`leaderboard_db`)  
**Tables:** leaderboards, leaderboard_entries, leaderboard_history  
**Redis:** Sorted sets cho xếp hạng realtime  
**Flyway Migrations:** ✅ Đã bật

---

## 🛠️ 10. Admin & Support Services (Giai đoạn P4)

### **admin-service**
**Thứ tự Build:** [42/53] | **Port:** 9091 | **Thời gian Build:** 18.574s

**Mục đích:** Quản lý service & dashboard admin

**Tính năng chính:**
- Kiểm soát start/stop service
- Giám sát health service
- Quản lý cấu hình
- Registry service (theo dõi tất cả 51 services)
- Giao diện web admin

**Database:** MySQL Port **33088** (`game_admin`)  
**Tables:** service_config (đã đăng ký 51 services)  
**Web UI:** http://localhost:9091  
**Thông tin đăng nhập:** admin / admin123  
**Flyway Migrations:** ✅ V1__Init_51_services.sql (đã consolidate)

**Quản lý Service:**
- Xem trạng thái tất cả services
- Start/stop services riêng lẻ
- Xem logs service
- Giám sát sử dụng tài nguyên
- Thực hiện thao tác hàng loạt

---

### **gm-service**
**Thứ tự Build:** [53/53] | **Port:** 8500 | **Thời gian Build:** 16.352s

**Mục đích:** Công cụ & thao tác Game Master

**Tính năng chính:**
- Quản lý tài khoản người chơi (ban, unban)
- Phát vật phẩm/tiền tệ
- Thực thi lệnh GM
- Thao tác bảo trì server
- Công cụ rollback dữ liệu

**Database:** MySQL Port **33089** (`game_gm`)  
**Tables:** gm_operations, gm_logs  
**Credentials:** root / root (quyền đặc biệt)  
**Flyway Migrations:** ✅ Đã bật

**Lệnh GM:**
- `/give [player] [item] [amount]` - Tặng vật phẩm
- `/ban [player] [reason]` - Khóa tài khoản
- `/announcement [message]` - Thông báo server
- `/teleport [player] [x] [y]` - Dịch chuyển người chơi

---

### **notification-service**
**Thứ tự Build:** [46/53] | **Port:** 8520 | **gRPC:** 9520 | **Thời gian Build:** 21.979s

**Mục đích:** Hệ thống push notification & cảnh báo

**Tính năng chính:**
- Gửi push notification (iOS/Android)
- Quản lý thông báo trong game
- Template thông báo
- Thông báo theo lịch
- Lịch sử thông báo

**Database:** MySQL Port **33090** (`game_notification`)  
**Tables:** notifications, notification_logs  
**gRPC Services:** NotificationService  
**API bên ngoài:** Firebase Cloud Messaging (FCM), Apple Push Notification Service (APNs)  
**Flyway Migrations:** ✅ Đã bật

---

### **analytics-service**
**Thứ tự Build:** [45/53] | **Port:** 8510 | **gRPC:** 9510 | **Thời gian Build:** 23.581s

**Mục đích:** Thu thập analytics & metrics game

**Tính năng chính:**
- Theo dõi hành vi người chơi
- Analytics doanh thu
- Metrics retention
- Phân tích funnel
- Theo dõi sự kiện tùy chỉnh

**Database:** MySQL Port **33092** (`game_analytics`)  
**Tables:** events, user_metrics, revenue_data  
**gRPC Services:** AnalyticsService  
**Export dữ liệu:** CSV, JSON cho công cụ BI bên ngoài  
**Flyway Migrations:** ✅ Đã bật

---

### **moderation-service**
**Thứ tự Build:** [50/53] | **Port:** 8530 | **Thời gian Build:** 17.352s

**Mục đích:** Kiểm duyệt nội dung & giám sát hành vi người chơi

**Tính năng chính:**
- Kiểm duyệt tin nhắn chat
- Phát hiện & lọc từ ngữ tục tĩu
- Phân tích hành vi người chơi
- Hệ thống cảnh báo tự động
- Engine gợi ý khóa tài khoản

**Database:** MySQL Port **33091** (`game_moderation`)  
**Tables:** moderation_logs, banned_words, player_warnings  
**Machine Learning:** Mô hình ML phát hiện hành vi độc hại  
**Flyway Migrations:** ✅ Đã bật

---

### **anti-cheat-service**
**Thứ tự Build:** [52/53] | **Port:** 8093 | **Thời gian Build:** 17.148s

**Mục đích:** Phát hiện & ngăn chặn gian lận

**Tính năng chính:**
- Phát hiện hành vi bất thường
- Phát hiện speed hack
- Phát hiện thay đổi bộ nhớ
- Xác thực tính toàn vẹn client
- Nhận dạng mẫu gian lận

**Database:** MySQL Port **33093** (`game_anticheat`)  
**Tables:** cheat_detections, cheat_patterns, banned_ips  
**Giám sát Realtime:** Tích hợp WebSocket  
**Flyway Migrations:** ✅ Đã bật

---

## 📊 11. Utility Services (Giai đoạn P5)

### **scheduler-service**
**Thứ tự Build:** [47/53] | **Port:** 8550 | **Thời gian Build:** 14.203s

**Mục đích:** Lập lịch tác vụ phân tán & cron jobs

**Tính năng chính:**
- Lập lịch cron job
- Thực thi tác vụ phân tán
- Cơ chế retry tác vụ
- Giám sát trạng thái job
- Kích hoạt sự kiện theo lịch

**Database:** Không có  
**Storage:** Redis (hàng đợi job & locks)  
**Framework:** Spring @Scheduled + Redis distributed locks

**Tác vụ theo lịch:**
- Reset hàng ngày (00:00 giờ server)
- Phần thưởng arena hàng tuần (Chủ nhật 23:59)
- Snapshot bảng xếp hạng (hàng ngày)
- Dọn dẹp database (hàng tuần)

---

### **file-service**
**Thứ tự Build:** [48/53] | **Port:** 8540 | **gRPC:** 9540 | **Thời gian Build:** 13.425s

**Mục đích:** Quản lý upload & lưu trữ file

**Tính năng chính:**
- Upload/download file
- Lưu trữ ảnh avatar
- Lưu trữ screenshot
- Tích hợp CDN
- Kiểm soát truy cập file

**Database:** Không có  
**Storage:** File system + CDN  
**Định dạng hỗ trợ:** JPG, PNG, GIF (hình ảnh); ZIP, PDF (tài liệu)  
**gRPC Services:** FileService  
**Kích thước upload tối đa:** 10MB mỗi file

---

### **localization-service**
**Thứ tự Build:** [49/53] | **Port:** 8560 | **gRPC:** 9560 | **Thời gian Build:** 14.750s

**Mục đích:** Hỗ trợ đa ngôn ngữ & i18n

**Tính năng chính:**
- Quản lý dịch văn bản
- Load gói ngôn ngữ
- Cập nhật văn bản động
- Phát hiện ngôn ngữ
- Hỗ trợ ngôn ngữ dự phòng

**Database:** Không có  
**Storage:** Redis cache + file JSON  
**Ngôn ngữ hỗ trợ:** Tiếng Anh, Trung Quốc, Nhật Bản, Hàn Quốc, Việt Nam  
**gRPC Services:** LocalizationService

---

### **main-fb-service**
**Thứ tự Build:** [44/53] | **Port:** 8128 | **gRPC:** 9096 | **Thời gian Build:** 19.779s

**Mục đích:** Hệ thống dungeon chính/phó bản (FB = FuBen)

**Tính năng chính:**
- Tạo instance dungeon
- Cơ chế chiến đấu boss
- Theo dõi tiến trình dungeon
- Các chế độ khó instance
- Phân phối phần thưởng dungeon

**Database:** MySQL Port **33094** (`game_mainfb`)  
**Tables:** dungeon_instances, dungeon_progress, dungeon_rewards  
**gRPC Services:** MainFbService  
**Flyway Migrations:** ✅ Đã bật

---

### **dataaccess-service**
**Thứ tự Build:** [35/53] | **Port:** 8340 | **Thời gian Build:** 9.495s

**Mục đích:** Lớp truy cập dữ liệu & tổng hợp query

**Tính năng chính:**
- Query dữ liệu liên services
- Tổng hợp dữ liệu
- Query báo cáo
- Pooling kết nối database
- Cache query

**Database:** Không có (Truy cập database của services khác qua JPA)  
**Storage:** Redis (cache query)  
**Mục đích:** Truy cập dữ liệu tập trung cho báo cáo và analytics

---

## 📚 12. Parent POM Module

### **Game Server Parent**
**Thứ tự Build:** [36/53] | **Thời gian Build:** 0.070s

**Mục đích:** Parent POM để quản lý dependencies

**Tính năng chính:**
- Phiên bản dependency tập trung
- Cấu hình plugin chung
- Định nghĩa build profile
- Properties chia sẻ

**Loại:** Module Maven POM (không phải service)  
**Phiên bản:** 1.0.0  
**Spring Boot Version:** 3.5.3  
**Spring Cloud Version:** 2025.0.0  
**Java Version:** 21

---

## 🔧 Thông Tin Build & Deployment

### Thống Kê Build
- **Tổng thời gian Build:** 19:01 phút
- **Build lâu nhất:** gateway-service (01:53 phút)
- **Build nhanh nhất:** Game Server Parent (0.070s)
- **Thời gian Build trung bình:** ~21.5 giây mỗi service
- **Compilation:** Java 21 với Virtual Threads
- **Annotation Processing:** Lombok đã bật

### Tóm Tắt Dependencies
- **Spring Boot:** 3.5.3
- **Spring Cloud:** 2025.0.0
- **MySQL Connector:** 8.0.33+ (mysql-connector-j)
- **Protobuf:** 3.21.7
- **gRPC:** 1.50.2
- **Redis:** Lettuce client
- **Kafka:** 3.x
- **Flyway:** 9.x

### Tóm Tắt Database
- **Tổng số MySQL Instances:** 36 (một instance cho mỗi service có database)
- **Dải Port:** 3318-3357 (cũ), 33062-33096 (mới)
- **Database User:** tpnam
- **Database Password:** 121831
- **Root Password:** root (chỉ gm-service)

### Yêu Cầu Hạ Tầng
- **Bộ nhớ (Ước tính):**
  - MySQL: 7-17GB (36 instances × 200-500MB)
  - Redis: 2GB
  - Kafka: 1GB
  - Services: 25-50GB (51 services × 512MB-1GB)
  - **Tổng:** ~35-70GB RAM
  
- **CPU:** Khuyến nghị 8+ cores
- **Dung lượng đĩa:** 100GB+ (bao gồm logs, databases, backups)

### Docker Compose Files
- `docker/docker-compose-databases.yml` - Tất cả 36 MySQL instances
- `docker/docker-compose-infrastructure.yml` - Redis, Kafka, Zookeeper
- `docker/docker-compose-services.yml` - Tất cả 51 microservices

---

## 🚀 Thứ Tự Khởi Động Services

### Giai đoạn 1: Hạ tầng (Phải khởi động trước)
1. **eureka-server** (8761) - Service discovery
2. **config-service** (8888) - Cấu hình
3. **gateway-service** (8083) - API Gateway

### Giai đoạn 2: Core Services
4. **session-service** (8096) - Quản lý session
5. **webSocket-server** (8094) - WebSocket
6. **user-service** (8400) - Tài khoản người dùng
7. **role-service** (8410) - Quản lý nhân vật
8. **serverInfo-service** (8420) - Thông tin server

### Giai đoạn 3: Kinh tế & Vật phẩm
9. **wallet-service** (8200)
10. **item-service** (8330)
11. **bag-service** (8230)
12. **equip-service** (8240)
13. **shop-service** (8260)
14. **drop-service** (8250)
15. **gift-service** (8270)
16. **box-service** (8290)
17. **crafting-service** (8280)

### Giai đoạn 4: Chiến đấu & Tiến trình
18. **arena-service** (8084)
19. **battleserver-service** (8082)
20. **trial-service** (8094)
21. **task-service** (8095)
22. **world-service** (8084)
23. **territory-service** (8122)
24. **escort-service** (8129)
25. **gameworld-service** (8105)
26. **globalserver-service** (8100)

### Giai đoạn 5: Đồng hành
27. **pet-service** (8300)
28. **mount-service** (8310)
29. **angel-service** (8360)

### Giai đoạn 6: Cường hóa
30. **rune-service** (8320)
31. **artifact-service** (8370)
32. **shizhuang-service** (8350)
33. **starmap-service** (8120)

### Giai đoạn 7: Xã hội
34. **chat-service** (8470)
35. **friend-service** (8450)
36. **guild-service** (8440)
37. **mail-service** (8460)
38. **leaderboard-service** (8480)

### Giai đoạn 8: Quản trị & Hỗ trợ
39. **admin-service** (9091)
40. **gm-service** (8500)
41. **analytics-service** (8510)
42. **notification-service** (8520)
43. **moderation-service** (8530)
44. **anti-cheat-service** (8093)

### Giai đoạn 9: Tiện ích
45. **scheduler-service** (8550)
46. **file-service** (8540)
47. **localization-service** (8560)
48. **main-fb-service** (8128)
49. **iap-verify-service** (8220)
50. **report-service** (8210)
51. **dataaccess-service** (8340)

---

## 📝 Tham Khảo Nhanh

### Đăng nhập Admin Service
```
URL: http://localhost:9091
Tên đăng nhập: admin
Mật khẩu: admin123
```

### Eureka Dashboard
```
URL: http://localhost:8761
```

### API Gateway Base URL
```
URL: http://localhost:8083
```

### Lệnh Maven Thông Dụng
```bash
# Build tất cả services
mvn clean install -DskipTests

# Build một service
cd tên-service
mvn clean package

# Chạy một service
java -jar target/tên-service-1.0.0.jar
```

### Lệnh Docker Thông Dụng
```bash
# Khởi động tất cả databases
docker-compose -f docker/docker-compose-databases.yml up -d

# Dừng tất cả databases
docker-compose -f docker/docker-compose-databases.yml down

# Xem logs
docker-compose -f docker/docker-compose-databases.yml logs -f
```

---

## 🔗 Tài Liệu Liên Quan

- [DATABASE_SERVICE_MAPPING.md](../../DATABASE_SERVICE_MAPPING.md) - Mapping database đầy đủ
- [ADMIN_SERVICE_QUICK_START.md](../../ADMIN_SERVICE_QUICK_START.md) - Hướng dẫn admin service
- [GAME_MECHANICS_GUIDE.md](../../GAME_MECHANICS_GUIDE.md) - Tài liệu tính năng game
- [SERVICE_IMPLEMENTATION_COMPLETE.md](../../SERVICE_IMPLEMENTATION_COMPLETE.md) - Trạng thái implementation

---

**Cập nhật lần cuối:** 02/02/2026 00:56:27  
**Trạng thái Build:** ✅ THÀNH CÔNG  
**Tổng số Modules:** 53 (1 common-lib + 1 parent + 51 services)
