# Pet Service

**Version**: 1.0.0
**Phase**: P3 (Enhancement & Support)
**Port**: 8112
**Database**: game_pet

---

## 📋 Overview

Pet Service quản lý **hệ thống thú cưng (Pet System)** — thu thập, nâng cấp, tiến hóa thú cưng, hệ thống đá ngọc thú cưng, trang phục thú cưng, và di hài thú cưng. Thú cưng tăng combat power cho nhân vật.

### Core Features
- ✅ Thu thập và nâng cấp thú cưng
- ✅ Kỹ năng thú cưng (learn/lock/unlock)
- ✅ Tiến hóa thú cưng (Evolve / Grade Up)
- ✅ Đá ngọc thú cưng (Gem inlay/dismount/levelup)
- ✅ Đá thần thú cưng (TS Gem system)
- ✅ Trang phục thú cưng (Cloth wear/upgrade)
- ✅ Di hài thú cưng (Remains equip/upgrade)
- ✅ Hầm ngục thú cưng (Pet Dungeon)

---

## 🔌 API Endpoints

### Pet Core
```
GET    /api/pet/{userId}                        - Lấy toàn bộ thông tin pet
GET    /api/pet/{userId}/{petIndex}             - Lấy thông tin một pet
GET    /api/pet/{userId}/capability/{petIndex}  - Lấy combat power của pet
GET    /api/pet/{userId}/hasspace               - Kiểm tra còn chỗ trong pet bag
POST   /api/pet/{userId}/add                    - Thêm pet mới (param: petId)
POST   /api/pet/{userId}/levelup                - Nâng cấp pet (params: petIndex, num)
POST   /api/pet/{userId}/gradeup                - Grade up pet
POST   /api/pet/{userId}/evolve                 - Tiến hóa pet
POST   /api/pet/{userId}/fight                  - Tham chiến / rút pet
POST   /api/pet/{userId}/recalculate            - Tính lại combat power
DELETE /api/pet/{userId}/{petIndex}             - Xóa pet
```

### Pet Skills
```
POST   /api/pet/{userId}/skill/learn            - Học kỹ năng
POST   /api/pet/{userId}/skill/unlock           - Mở khóa kỹ năng
POST   /api/pet/{userId}/skill/lock             - Khóa kỹ năng
```

### Pet Gem (đá ngọc)
```
POST   /api/pet/{userId}/gem/inlay              - Khảm đá
POST   /api/pet/{userId}/gem/dismount           - Tháo đá
POST   /api/pet/{userId}/gem/levelup-bag        - Nâng cấp đá từ túi
POST   /api/pet/{userId}/gem/levelup-pet        - Nâng cấp đá trên pet
POST   /api/pet/{userId}/gem/onekey-levelup     - Nâng cấp một chạm
POST   /api/pet/gem/{userId}/inlay              - Khảm đá (via gem controller)
POST   /api/pet/gem/{userId}/levelup/bag        - Nâng cấp đá
POST   /api/pet/gem/{userId}/levelup/pet        - Nâng cấp đá trên pet
POST   /api/pet/gem/{userId}/onekey             - Một chạm nâng cấp
DELETE /api/pet/gem/{userId}/dismount           - Tháo đá
```

### Pet TS Gem (đá thần)
```
POST   /api/pet/{userId}/tsgem/inlay            - Khảm đá thần
POST   /api/pet/{userId}/tsgem/dismount         - Tháo đá thần
POST   /api/pet/{userId}/tsgem/levelup          - Nâng cấp đá thần
POST   /api/pet/{userId}/tsgem/onekey-levelup   - Một chạm nâng cấp đá thần
POST   /api/pet/{userId}/tsgem/refresh          - Refresh thuộc tính đá thần
POST   /api/pet/{userId}/tsgem/addattr          - Thêm thuộc tính đá thần
POST   /api/pet/gem/{userId}/tsgem/add          - Thêm đá thần
POST   /api/pet/gem/{userId}/tsgem/inlay        - Khảm đá thần
POST   /api/pet/gem/{userId}/tsgem/levelup      - Nâng cấp đá thần
POST   /api/pet/gem/{userId}/tsgem/onekey       - Một chạm nâng cấp
POST   /api/pet/gem/{userId}/tsgem/refresh      - Refresh đá thần
POST   /api/pet/gem/{userId}/tsgem/addattr      - Thêm thuộc tính
DELETE /api/pet/gem/{userId}/tsgem/dismount     - Tháo đá thần
GET    /api/pet/gem/{userId}/tsgem/{gemIndex}            - Thông tin đá thần
GET    /api/pet/gem/{userId}/tsgem/{gemIndex}/canrefresh - Có thể refresh không
```

### Pet Cloth (trang phục thú cưng)
```
POST   /api/pet/{userId}/cloth/upgrade          - Nâng cấp trang phục
POST   /api/pet/{userId}/cloth/wear             - Mặc trang phục
POST   /api/pet/{userId}/cloth/unequip          - Cởi trang phục
POST   /api/pet/cloth/{userId}/upgrade          - Nâng cấp trang phục
POST   /api/pet/cloth/{userId}/wear             - Mặc trang phục
DELETE /api/pet/cloth/{userId}/unequip          - Cởi trang phục
GET    /api/pet/cloth/{userId}/{clothId}        - Thông tin trang phục
GET    /api/pet/cloth/{userId}/bonus/{clothId}  - Bonus trang phục
```

### Pet Remains (di hài thú cưng)
```
POST   /api/pet/{userId}/remains/equip          - Trang bị di hài
POST   /api/pet/{userId}/remains/unequip        - Tháo di hài
POST   /api/pet/{userId}/remains/upgrade        - Nâng cấp di hài
POST   /api/pet/remains/{userId}/add            - Thêm di hài
POST   /api/pet/remains/{userId}/levelup        - Nâng cấp di hài
GET    /api/pet/remains/{userId}/{remainsIndex} - Thông tin di hài
GET    /api/pet/remains/{userId}/bonus          - Bonus di hài
GET    /api/pet/remains/{userId}/hasspace       - Còn chỗ không
DELETE /api/pet/remains/{userId}/{remainsIndex} - Xóa di hài
```

### Pet Dungeon (hầm ngục thú cưng)
```
GET    /api/pet/{userId}/dungeon                - Thông tin hầm ngục
POST   /api/pet/{userId}/dungeon/start          - Bắt đầu hầm ngục
POST   /api/pet/{userId}/dungeon/claim          - Nhận phần thưởng hầm ngục
```

---

## 🚀 Running

```bash
cd GameServer/pet-service
mvn clean install
mvn spring-boot:run
```

---

## 🔗 Integration Points

### Được gọi bởi
- **WebSocket server**: Xử lý pet protocol messages

### Gọi ra
- **bag-service**: Consume items khi nâng cấp/tiến hóa
- **wallet-service**: Consume currency

---

**Status**: ✅ Production Ready (Updated 2026-03-22)
**Last Updated**: 2026-03-22

