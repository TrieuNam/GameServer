# Phân tích handler trong `LoginBootstrapHandler.java`

> File gốc: `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/login/LoginBootstrapHandler.java`
> 
> Mục tiêu: phân biệt **handler nào là dữ liệu role/core**, **handler nào là dữ liệu module/game**, và xác định chỗ nào có rủi ro đẩy dữ liệu khi DB chưa có record thật.

---

## 1. Kết luận ngắn

`LoginBootstrapHandler` **không tự đọc JSON rồi ghi vào `Role` DB**.
Nó chỉ là **orchestrator / bootstrap dispatcher**:

- login xong thì gọi `Emitters.sendRoleInfoAck(ps, role)` để đẩy **core role snapshot**
- sau đó gọi hàng loạt `*.pushAll(ps)` để đẩy **snapshot của từng module game**

Vì vậy, nếu user mới login mà thấy `mount / pet / angel / equip appearance` xuất hiện dù DB role chưa có, thì có 2 tầng cần hiểu:

1. **`LoginBootstrapHandler` gọi module quá sớm**
2. **module handler/service phía sau đang trả dữ liệu default/config-derived dù DB module đó chưa có record thật**

---

## 2. Luồng dữ liệu trong `LoginBootstrapHandler`

### 2.1 Core login ACK
Trong `handleLogin(...)`, sau khi `role.listByUser` hoặc `role.create` xong, handler đẩy:

- `Emitters.sendLoginAck(ps, LOGIN_OK, 0)`
- `Emitters.sendTimeAck(...)`
- `Emitters.sendRoleInfoAck(ps, role)`

### 2.2 Bootstrap theo module
Sau đó khi client gửi `1450 / CS_ALL_INFO_REQ` (hoặc fallback timer chạy), handler gọi:

- `buildCoreBootstrap(ps, t0Bootstrap)`
- `buildDeferredBootstrap(ps, t0Bootstrap)`

> Đây là **fan-out bootstrap** theo module, **không phải Role DB snapshot thuần**.

---

## 3. Phân loại các handler trong `buildCoreBootstrap()`

| Handler | Vai trò | Nguồn dữ liệu chính | Loại dữ liệu | Nhận xét |
|---|---|---|---|---|
| `roleServiceHandler.pushAll(ps)` | đẩy `1401` role attrs/stats | `role-service` (`getOtherRole`) | **Role/module DB** | gần nhất với “dữ liệu role” |
| `bagHandler.pushAll(ps)` | đẩy túi đồ | `bag-service` (`bagFeign.list`) | **Module DB theo roleId** | không phải bảng `role`, nhưng là dữ liệu sở hữu của role |
| `equipHandler.pushAll(ps)` | đẩy equip list, fumo list, bag equip slots | `equip-service` | **Module DB theo roleId** | không phải core role row |
| `boxHandler.pushAll(ps)` | đẩy `1616/1617/1619` trạng thái box | `box-service` + remote lookups | **Module DB/cache theo roleId** | đây là dữ liệu hệ thống box, không phải dữ liệu role core |
| `taskHandler.reportDailyLogin(...).then(taskHandler.pushAll(ps))` | báo sự kiện login và đẩy current task | `task-service` | **Module DB + task config logic** | là dữ liệu quest/tutorial của game |
| `skillHandler.pushAll(ps)` | đẩy skill + talent | `skill-service` | **Module DB theo roleId** | không phải bảng `role` |

### Ghi chú quan trọng về `boxHandler`
Ví dụ bạn nêu:

```java
safe(() -> boxHandler.pushAll(ps), "box", ps, t0Bootstrap)
```

Đúng: đây **không phải dữ liệu `Role`**.
Nó là **dữ liệu module box** của game, chỉ gắn với `roleId` để biết người chơi nào đang mở hộp / setting gì / compare state gì.

---

## 4. Phân loại các handler trong `buildDeferredBootstrap()`

| Handler | Vai trò | Nguồn dữ liệu chính | Loại dữ liệu | Rủi ro với role mới |
|---|---|---|---|---|
| `openServerActivityHandler.pushAll(ps)` | event mở server | `activity-service` | **Module DB + event config** | trung bình |
| `blockHandler.pushAll(ps)` | block info | block-service | **Module DB** | thấp |
| `waBaoHandler.pushAll(ps)` | đào kho báu / loot progress | wabao-service | **Module DB** | thấp-trung bình |
| `shiZhuangHandler.pushAll(ps)` | fashion/fumo | fashion-service | **Module DB + config** | trung bình |
| `gemHandler.pushAll(ps)` | gem state | gem-service | **Module DB** | thấp |
| `scrollHandler.pushAll(ps)` | scroll system | scroll-service | **Module DB / gameplay** | thấp |
| `pagodaHandler.pushAll(ps)` | pagoda/tower progress | pagoda-service | **Module DB** | thấp |
| `lingZhuHandler.pushAll(ps)` | linh châu | lingzhu-service | **Module DB** | thấp |
| `runeHandler.pushAll(ps)` | rune state | rune-service | **Module DB** | thấp |
| `shenQiHandler.pushAll(ps)` | artifact/shenqi | shenqi-service | **Module DB** | thấp-trung bình |
| `petHandler.pushAll(ps)` | pet list | `pet-service` | **Module DB theo roleId** | **cao** nếu service synthesize default pet data khi DB rỗng |
| `angelHandler.pushAll(ps)` | angel info | `angel-service`/gRPC | **Module DB theo roleId** | **cao** nếu service trả default appearance/equip |
| `mountHandler.pushAll(ps)` | mount info/harness list | `mount-service` | **Module DB theo roleId** | **cao** nếu service/config trả default mount/harness |
| `friendHandler.pushAll(ps)` | friend list + requests | friend-service | **Module DB / social** | thấp |
| `mailHandler.pushAll(ps)` | mail list | mail-service | **Module DB** | thấp |
| `starMapHandler.pushAll(ps)` | starmap progress | starmap-service | **Module DB** | thấp |
| `arenaHandler.pushAll(ps)` | arena state | arena-service | **Module DB** | thấp |
| `escortHandler.pushAll(ps)` | escort state | escort-service | **Module DB** | thấp |
| `territoryHandler.pushAll(ps)` | territory state | territory-service | **Module DB** | thấp |
| `guildHandler.pushAll(ps)` | guild info | guild-service | **Module DB / social** | thấp |
| `mainFbHandler.pushAll(ps)` | main story progress | mainfb-service | **Module DB** | thấp |

---

## 5. Handler nào là “dữ liệu game”, handler nào là “dữ liệu DB”?

### 5.1 Gần với **core role / authoritative snapshot**
- `Emitters.sendRoleInfoAck(ps, role)`
- `roleServiceHandler.pushAll(ps)`

Đây là phần gần nhất với dữ liệu `Role` thực thụ.

### 5.2 **Dữ liệu module DB theo roleId**
Các handler còn lại đa số thuộc nhóm này:
- bag
n- equip
- box
- task
- skill
- pet
- angel
- mount
- mail
- friend
- guild
- arena
- mainfb
- ...

Chúng **không phải bảng `Role`**, nhưng vẫn là **DB/state của game gắn theo `roleId`**.

### 5.3 **Dữ liệu game/config-driven (hybrid)**
Một số handler vừa dùng DB, vừa dựa trên config/json/event definition:
- `taskHandler`
- `openServerActivityHandler`
- `shiZhuangHandler`
- `mountHandler` / `petHandler` / `angelHandler` (nếu service có default config mapping)

> Đây là nhóm dễ sinh bug kiểu: **DB chưa có nhưng config/default làm client tưởng là đã có**.

---

## 6. Trả lời đúng câu hỏi “DB không có thì không đẩy”

### Đúng về mặt business rule:
Nếu DB module chưa có record thật cho `roleId`, thì handler/module đó nên:

- trả về **empty snapshot**
- hoặc **không emit gì**
- tuyệt đối không dùng default từ config/json để dựng thành “user đã sở hữu”

### Vì vậy vấn đề không phải là:
- `LoginBootstrapHandler` tự ghi dữ liệu vào `Role`

### Mà là:
- `LoginBootstrapHandler` đang **gọi bootstrap module**
- và **module nào trả default khi DB rỗng** thì sẽ làm client hiển thị sai

---

## 7. Kết luận thực thi

### Điều chắc chắn đúng từ file này
1. `LoginBootstrapHandler` là **bootstrap dispatcher**
2. `boxHandler.pushAll(ps)` và các handler tương tự là **dữ liệu module game**, không phải dữ liệu `Role` thuần
3. `buildDeferredBootstrap()` là nơi fan-out nhiều module phụ — nên đây là **điểm orchestration có thể cần chặn/điều kiện hóa** cho role mới

### Điều cần rà tiếp nếu muốn fix gốc
Kiểm tra từng module sau xem có đang trả default/config-derived data dù DB rỗng không:
- `MountHandler` / `mount-service`
- `PetHandler` / `pet-service`
- `AngelHandler` / `angel-service`

---

## 8. Phán đoán chuẩn cho bug hiện tại

Với bug “user mới login đã thấy mount/pet/angel/visual”:

- **không phải do `roleServiceHandler.pushAll(ps)` nhét thẳng vào Role**
- **không phải do `boxHandler.pushAll(ps)` là dữ liệu Role**
- mà là do **bootstrap module phụ** + **module service trả dữ liệu không đủ chặt khi DB chưa có**

---

## 9. Trạng thái fix cuối cùng

Hiện tại đã có đủ 3 lớp fix:

1. **Gateway / bootstrap** (`LoginBootstrapHandler.java`)
   - role mới chỉ nhận `CORE` sớm
   - deferred gameplay modules bị chặn ở wave đầu

2. **Module service** (`mount/pet/angel`)
   - DB không có thì trả trạng thái rỗng / `hasData=false`
   - websocket handler sẽ không emit login snapshot giả

3. **Client render path**
   - đã bỏ fallback mặc định khiến `surfaceWeapon=0` vẫn hiện spear và `surfaceShield=0` vẫn hiện shield
   - đồng thời clear stale equip snapshot khi nhận full equip list mới

### Kết luận cuối
Bug này không chỉ nằm ở `LoginBootstrapHandler`.
Nó là chuỗi gồm:
- bootstrap quá sớm,
- service trả snapshot rỗng chưa đủ chặt,
- và client fallback skin mặc định.

Hiện cả 3 tầng đã được siết để đúng rule: **DB không có thì không hiển thị**.
