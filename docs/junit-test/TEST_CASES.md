# Kịch Bản Test Case - GameServer JUnit Tests

> **Phiên bản:** 2.0
> **Ngày tạo:** 2026-02-20
> **Cập nhật:** 2026-02-20
> **Phạm vi:** Tất cả service layer trong hệ thống GameServer (44 services)

---

## Mục Lục

**Nhóm Core**
1. [UserService](#1-userservice)
2. [AuthService](#2-authservice)
3. [SessionService](#3-sessionservice)
4. [WalletService](#4-walletservice)
5. [BagDomainService](#5-bagdomainservice)
6. [RoleService](#6-roleservice)
7. [TaskDomainService](#7-taskdomainservice)
8. [EquipService](#8-equipservice)
9. [ShopService](#9-shopservice)
10. [MailService](#10-mailservice)
11. [GuildService](#11-guildservice)
12. [ArenaService](#12-arenaservice)

**Nhóm Character & Progression**
13. [PetService](#13-petservice)
14. [ItemService](#14-itemservice)
15. [MountService](#15-mountservice)
16. [ArtifactService](#16-artifactservice)
17. [RuneService](#17-runeservice)
18. [BoxService](#18-boxservice)

**Nhóm Economy & Content**
19. [CraftingService](#19-craftingservice)
20. [PityService](#20-pityservice)
21. [GiftService](#21-giftservice)

**Nhóm Social**
22. [FriendService](#22-friendservice)
23. [ChatService](#23-chatservice)
24. [LeaderboardService](#24-leaderboardservice)
25. [RankingService](#25-rankingservice)

**Nhóm World**
26. [WorldService](#26-worldservice)
27. [SceneManagementService](#27-scenemanagementservice)

**Nhóm Admin & Safety**
28. [AnalyticsService](#28-analyticsservice)
29. [AntiCheatService](#29-anticheatservice)
30. [ModerationService](#30-moderationservice)
31. [ReportEventService](#31-reporteventservice)
32. [NotificationService](#32-notificationservice)
33. [IapVerifyService](#33-iapverifyservice)
34. [GMService](#34-gmservice)

**Nhóm Advanced Systems**
35. [ShizhuangService](#35-shizhuangservice)
36. [AngelService](#36-angelservice)
37. [TerritoryService](#37-territoryservice)
38. [TrialService](#38-trialservice)
39. [EscortService](#39-escortservice)
40. [StarMapService](#40-starmapservice)

**Nhóm Infrastructure**
41. [ConfigService](#41-configservice)
42. [FileService](#42-fileservice)
43. [LocalizationService](#43-localizationservice)
44. [CombatService](#44-combatservice)
45. [ServerInfoService](#45-serverinfoservice)

---

## Quy Ước Đặt Tên

| Ký hiệu | Ý nghĩa |
|---------|---------|
| `TC-XXX-001` | Test Case ID (XXX = viết tắt service) |
| `[P]` | Positive test – đầu vào hợp lệ, kết quả mong đợi thành công |
| `[N]` | Negative test – đầu vào không hợp lệ hoặc điều kiện lỗi |
| `[B]` | Boundary test – kiểm tra giá trị biên |
| `[I]` | Idempotency test – kiểm tra tính bất biến khi gọi nhiều lần |

---

## 1. UserService

**File:** `user-service/.../service/UserService.java`
**Mô tả:** Quản lý đăng ký tài khoản, đăng nhập, đổi mật khẩu, cập nhật trạng thái.

### 1.1 `createUser(account, username, password)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-USR-001 | [P] | Tạo user mới thành công | account="user01", username="Player1", password="Pass123!" | Trả về User có userId không null, passHash được mã hóa (BCrypt), không bằng rawPassword |
| TC-USR-002 | [N] | Account đã tồn tại | account trùng với user có sẵn | Ném exception (AccountAlreadyExistException hoặc tương đương) |
| TC-USR-003 | [N] | Username đã tồn tại | username trùng với user có sẵn | Ném exception |
| TC-USR-004 | [N] | Account null | account=null | Ném IllegalArgumentException hoặc ValidationException |
| TC-USR-005 | [N] | Password rỗng | password="" | Ném ValidationException |
| TC-USR-006 | [B] | Password đúng 1 ký tự | password="a" | Tạo thành công hoặc ném lỗi tuỳ business rule |

### 1.2 `login(accountOrUsername, password)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-USR-010 | [P] | Đăng nhập bằng account thành công | accountOrUsername="user01", password="Pass123!" | Trả về User hợp lệ |
| TC-USR-011 | [P] | Đăng nhập bằng username thành công | accountOrUsername="Player1", password="Pass123!" | Trả về User hợp lệ |
| TC-USR-012 | [N] | Sai mật khẩu | password="WrongPass" | Ném AuthenticationException |
| TC-USR-013 | [N] | Tài khoản không tồn tại | accountOrUsername="ghost" | Ném UserNotFoundException |
| TC-USR-014 | [N] | Tài khoản bị khóa (status=BANNED) | user có status=BANNED | Ném AccountDisabledException |
| TC-USR-015 | [N] | Tài khoản bị vô hiệu hóa (status=INACTIVE) | user có status=INACTIVE | Ném AccountDisabledException |

### 1.3 `changePassword(userId, oldPassword, newPassword)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-USR-020 | [P] | Đổi mật khẩu thành công | oldPassword đúng, newPassword hợp lệ | passHash được cập nhật; hash mới match với newPassword |
| TC-USR-021 | [N] | Mật khẩu cũ sai | oldPassword="WrongOld" | Ném InvalidPasswordException |
| TC-USR-022 | [N] | UserId không tồn tại | userId=99999L | Ném UserNotFoundException |
| TC-USR-023 | [N] | Mật khẩu mới trùng mật khẩu cũ | newPassword = oldPassword | Ném hoặc cho phép tuỳ business rule |

### 1.4 `updateStatus(userId, status)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-USR-030 | [P] | Khóa tài khoản | status=BANNED | User.status == BANNED sau khi gọi |
| TC-USR-031 | [P] | Kích hoạt lại tài khoản | status=ACTIVE | User.status == ACTIVE sau khi gọi |
| TC-USR-032 | [N] | UserId không tồn tại | userId không có trong DB | Ném UserNotFoundException |

---

## 2. AuthService

**File:** `user-service/.../service/AuthService.java`
**Mô tả:** Xác thực mật khẩu và kiểm tra trạng thái hoạt động của user (với cache Redis).

### 2.1 `verifyPassword(accountOrUsername, rawPassword)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-AUTH-001 | [P] | Xác thực đúng bằng account | account hợp lệ, rawPassword đúng | Trả về User object |
| TC-AUTH-002 | [P] | Xác thực đúng bằng username | username hợp lệ, rawPassword đúng | Trả về User object |
| TC-AUTH-003 | [N] | Mật khẩu sai | rawPassword không khớp hash | Trả về null hoặc ném exception |
| TC-AUTH-004 | [N] | User không tồn tại | accountOrUsername="nonexistent" | Trả về null hoặc ném exception |

### 2.2 `isActive(userId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-AUTH-010 | [P] | User đang hoạt động (status=ACTIVE) | userId của user ACTIVE | Trả về true |
| TC-AUTH-011 | [P] | User bị cấm (status=BANNED) | userId của user BANNED | Trả về false |
| TC-AUTH-012 | [I] | Cache hit – gọi 2 lần liên tiếp | cùng userId | Lần 2 lấy từ cache, không gọi DB; kết quả giống nhau |
| TC-AUTH-013 | [N] | UserId không tồn tại | userId=99999L | Trả về false hoặc ném exception |

---

## 3. SessionService

**File:** `session-service/.../service/SessionService.java`
**Mô tả:** Quản lý JWT token, đăng nhập/đăng xuất, refresh token, heartbeat, rate limiting.

### 3.1 `login(LoginReq, ip)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SES-001 | [P] | Đăng nhập thành công | credentials hợp lệ, ip hợp lệ | Trả về accessToken + refreshToken; accessToken là JWT hợp lệ |
| TC-SES-002 | [N] | Sai credentials | password sai | Ném AuthenticationException |
| TC-SES-003 | [N] | Rate limit theo IP bị vượt | Gọi > N lần từ cùng 1 IP trong X giây | Ném RateLimitException |
| TC-SES-004 | [N] | Rate limit theo username bị vượt | Gọi > N lần với cùng username | Ném RateLimitException |
| TC-SES-005 | [P] | IP mới sau khi rate limit hết hạn | Chờ hết window thì gọi lại | Cho phép đăng nhập |

### 3.2 `refresh(RefreshReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SES-010 | [P] | Refresh token hợp lệ | refreshToken còn hạn | Trả về accessToken mới |
| TC-SES-011 | [N] | Refresh token hết hạn | token quá hạn | Ném TokenExpiredException |
| TC-SES-012 | [N] | Refresh token giả mạo | chữ ký sai | Ném InvalidTokenException |
| TC-SES-013 | [N] | Refresh token đã bị thu hồi (đã logout) | token của session đã logout | Ném InvalidTokenException |

### 3.3 `heartbeat(accessToken)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SES-020 | [P] | Heartbeat token hợp lệ | accessToken còn hạn | Session được gia hạn; không ném exception |
| TC-SES-021 | [N] | Heartbeat token hết hạn | token hết hạn | Ném TokenExpiredException |

### 3.4 `logout(accessToken)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SES-030 | [P] | Logout thành công | accessToken hợp lệ | Session bị xóa khỏi Redis |
| TC-SES-031 | [N] | Logout token không tồn tại | token chưa bao giờ login | Không ném exception (idempotent) hoặc ném exception |

### 3.5 `introspect(accessToken)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SES-040 | [P] | Token hợp lệ, user active | accessToken đúng, user ACTIVE | Trả về IntrospectResp với active=true |
| TC-SES-041 | [N] | Token hợp lệ nhưng user bị banned | user có status=BANNED | Trả về active=false |
| TC-SES-042 | [N] | Token hết hạn | token expired | Trả về active=false hoặc ném exception |
| TC-SES-043 | [N] | Token giả mạo | chữ ký JWT sai | Trả về active=false hoặc ném exception |

### 3.6 `decodeAndVerify(token)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SES-050 | [P] | Decode JWT hợp lệ | JWT ký bằng secret đúng | Trả về claims (userId, account, deviceId, sessionId) |
| TC-SES-051 | [N] | JWT sai chữ ký | ký bằng secret khác | Ném JWTVerificationException |
| TC-SES-052 | [N] | JWT hết hạn | exp < now | Ném TokenExpiredException |
| TC-SES-053 | [N] | Chuỗi không phải JWT | "not-a-token" | Ném ParseException |

---

## 4. WalletService

**File:** `wallet-service/.../service/WalletService.java`
**Mô tả:** Quản lý tài khoản tiền ảo (gold, gem...), nạp tiền, trừ tiền, idempotency.

### 4.1 `batchAdd(BatchReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-WAL-001 | [P] | Nạp gold thành công | roleId hợp lệ, itemId=GOLD, amount=1000 | Balance tăng thêm 1000 |
| TC-WAL-002 | [P] | Nạp nhiều loại tiền cùng lúc | BatchReq gồm GOLD+GEM | Cả 2 balance đều tăng đúng |
| TC-WAL-003 | [I] | Idempotency – gọi 2 lần cùng idemKey | idemKey giống nhau | Balance chỉ tăng 1 lần |
| TC-WAL-004 | [N] | ItemId không phải virtual currency | itemId là item vật phẩm | Ném InvalidItemTypeException |
| TC-WAL-005 | [N] | Amount âm | amount=-100 | Ném IllegalArgumentException |
| TC-WAL-006 | [B] | Amount = 0 | amount=0 | Ném hoặc bỏ qua tuỳ business rule |
| TC-WAL-007 | [B] | Amount rất lớn (Long.MAX_VALUE) | amount=Long.MAX_VALUE | Xử lý overflow hoặc ném exception |

### 4.2 `batchCost(BatchReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-WAL-010 | [P] | Trừ tiền đủ số dư | balance=500, cost=200 | Balance còn 300; Ledger ghi -200 |
| TC-WAL-011 | [P] | Trừ toàn bộ số dư | balance=500, cost=500 | Balance = 0 |
| TC-WAL-012 | [N] | Số dư không đủ | balance=100, cost=500 | Ném InsufficientFundsException |
| TC-WAL-013 | [I] | Idempotency khi trừ | idemKey giống nhau, gọi 2 lần | Chỉ trừ 1 lần |
| TC-WAL-014 | [N] | RoleId không tồn tại | roleId=99999L | Ném RoleNotFoundException hoặc trả về lỗi |
| TC-WAL-015 | [B] | Cost = 0 | cost=0 | Bỏ qua hoặc ném exception |

### 4.3 `get(roleId, itemIds)` và `info(roleId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-WAL-020 | [P] | Lấy số dư của 1 currency | roleId hợp lệ, itemId=GOLD | Trả về balance đúng |
| TC-WAL-021 | [P] | Lấy tất cả tài khoản tiền của role | roleId hợp lệ | Trả về list WalletAccount đầy đủ |
| TC-WAL-022 | [P] | Role mới chưa có tài khoản | roleId mới tạo | Trả về balance=0 hoặc tạo mới WalletAccount |
| TC-WAL-023 | [N] | RoleId null | roleId=null | Ném IllegalArgumentException |

---

## 5. BagDomainService

**File:** `bag-service/.../service/BagDomainService.java`
**Mô tả:** Quản lý túi đồ (inventory), thêm/dùng/bán vật phẩm.

### 5.1 `list(roleId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-BAG-001 | [P] | Lấy túi đồ thành công | roleId hợp lệ có items | Trả về list BagItem đúng |
| TC-BAG-002 | [P] | Túi đồ trống | roleId mới | Trả về list rỗng |
| TC-BAG-003 | [P] | Không trả về item hết hạn | item có expireAt < now | Item hết hạn không xuất hiện trong kết quả |

### 5.2 `grant(userId, roleId, items, eventId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-BAG-010 | [P] | Cấp item mới thành công | item chưa có trong túi | BagItem được tạo với num đúng |
| TC-BAG-011 | [P] | Stack thêm vào item đã có | itemId đã tồn tại trong túi, stackable | num tăng lên đúng |
| TC-BAG-012 | [I] | Idempotency – cùng eventId gọi 2 lần | eventId giống nhau | Item chỉ được cấp 1 lần |
| TC-BAG-013 | [P] | Cấp item có hạn sử dụng | expireAt hợp lệ | BagItem.expireAt được lưu đúng |
| TC-BAG-014 | [P] | Cấp item bind | bind=true | BagItem.bind=true |
| TC-BAG-015 | [N] | ItemId không tồn tại | itemId=99999 | Ném ItemNotFoundException |

### 5.3 `use(roleId, UseItemReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-BAG-020 | [P] | Dùng item thành công | đủ số lượng | num giảm đúng; nếu num=0 thì xóa khỏi túi |
| TC-BAG-021 | [N] | Không đủ số lượng | num=1, dùng 5 | Ném InsufficientItemException |
| TC-BAG-022 | [N] | Item không trong túi | itemId không có | Ném ItemNotFoundException |
| TC-BAG-023 | [N] | Item đã hết hạn | expireAt < now | Ném ItemExpiredException |
| TC-BAG-024 | [B] | Dùng đúng toàn bộ số lượng | num=5, dùng 5 | BagItem bị xóa hoặc num=0 |

### 5.4 `sell(roleId, SellItemReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-BAG-030 | [P] | Bán item lấy gold thành công | item sellable, đủ số lượng | num giảm, WalletService được gọi để thêm gold |
| TC-BAG-031 | [N] | Bán item không bán được (bind) | item bind=true | Ném CannotSellBoundItemException |
| TC-BAG-032 | [N] | Không đủ số lượng để bán | num=2, bán 5 | Ném InsufficientItemException |
| TC-BAG-033 | [N] | Item không có giá bán | sellPrice=0 | Ném hoặc bán với giá 0 tuỳ rule |

---

## 6. RoleService

**File:** `role-service/.../service/RoleService.java`
**Mô tả:** Quản lý nhân vật, kinh nghiệm, cấp độ, sức chiến đấu.

### 6.1 `createRole(userId, roleName, job)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ROL-001 | [P] | Tạo nhân vật thành công | userId hợp lệ, roleName="Hero", job=WARRIOR | Role được tạo với level=1, exp=0 |
| TC-ROL-002 | [N] | Tên nhân vật đã tồn tại | roleName trùng | Ném RoleNameExistsException |
| TC-ROL-003 | [N] | Vượt số nhân vật tối đa per user | countByUserId >= maxRolesPerUser | Ném MaxRolesExceededException |
| TC-ROL-004 | [N] | Job không hợp lệ | job="INVALID_CLASS" | Ném InvalidJobException |
| TC-ROL-005 | [N] | UserId không tồn tại | userId=99999L | Ném UserNotFoundException |
| TC-ROL-006 | [B] | Tên nhân vật 1 ký tự | roleName="A" | Tạo thành công hoặc validation error |
| TC-ROL-007 | [B] | Tên nhân vật dài tối đa | roleName=String(100 chars) | Tạo thành công hoặc validation error |

### 6.2 `addExp(AddExpRequest)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ROL-010 | [P] | Thêm exp, chưa đủ level up | exp hiện tại + delta < ngưỡng level | Exp tăng, level không đổi |
| TC-ROL-011 | [P] | Thêm exp vừa đủ để level up 1 lần | exp đủ lên level tiếp theo | Level tăng 1, exp được điều chỉnh |
| TC-ROL-012 | [P] | Thêm exp đủ để level up nhiều lần cùng lúc | exp lớn, vượt nhiều ngưỡng | Level tăng đúng số lần, exp còn lại đúng |
| TC-ROL-013 | [B] | Level đã ở maxLevel, thêm exp | level = MAX_LEVEL | Exp không tăng hoặc bị cap, level không vượt max |
| TC-ROL-014 | [N] | Exp âm | delta=-100 | Ném IllegalArgumentException |
| TC-ROL-015 | [N] | RoleId không tồn tại | roleId=99999L | Ném RoleNotFoundException |

### 6.3 `calculateLevel(totalExp, expConfig)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ROL-020 | [P] | exp=0, level=1 | totalExp=0 | Trả về level 1 |
| TC-ROL-021 | [P] | exp đúng ngưỡng level 5 | totalExp = cumulativeExpToLevel5 | Trả về level 5 |
| TC-ROL-022 | [P] | exp giữa level 3 và 4 | totalExp ở giữa | Trả về level 3 |
| TC-ROL-023 | [B] | totalExp = Long.MAX_VALUE | totalExp quá lớn | Trả về maxLevel, không crash |

### 6.4 `calculateFightPower(level, job)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ROL-030 | [P] | Tính sức chiến đấu level 1 | level=1, job=WARRIOR | fightPower = base + jobMultiplier |
| TC-ROL-031 | [P] | Tính sức chiến đấu level cao | level=50, job=MAGE | fightPower = base + (50-1)*50 + jobMultiplier |
| TC-ROL-032 | [P] | Các job khác nhau cùng level | level=10, jobs khác nhau | fightPower khác nhau theo job |

---

## 7. TaskDomainService

**File:** `task-service/.../service/TaskDomainService.java`
**Mô tả:** Quản lý nhiệm vụ: tiến độ, hoàn thành, nhận thưởng.

### 7.1 `getAllTasks(playerId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-TSK-001 | [P] | Lấy danh sách nhiệm vụ | playerId hợp lệ | Trả về tất cả tasks với trạng thái hiện tại |
| TC-TSK-002 | [P] | Player mới chưa có progress | playerId mới | Trả về tasks với status=NOT_STARTED |
| TC-TSK-003 | [P] | Player có task đang làm | playerId có IN_PROGRESS task | Task IN_PROGRESS xuất hiện với progress đúng |

### 7.2 `reportProgress(TaskReportReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-TSK-010 | [P] | Báo cáo tiến độ task đang làm | playerId, taskKey="kill_monster", delta=1 | progressValue tăng 1 |
| TC-TSK-011 | [P] | Tiến độ đạt đủ → task COMPLETED | progressValue + delta >= target | status chuyển sang COMPLETED |
| TC-TSK-012 | [P] | Task NOT_STARTED, báo progress → IN_PROGRESS | taskKey chưa có progress | Tạo mới với status=IN_PROGRESS |
| TC-TSK-013 | [N] | Task đã CLAIMED, báo thêm | status=CLAIMED | Progress không thay đổi (ignore hoặc exception) |
| TC-TSK-014 | [N] | TaskKey không hợp lệ | taskKey="fake_task" | Ném hoặc bỏ qua |

### 7.3 `claim(TaskClaimReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-TSK-020 | [P] | Nhận thưởng task COMPLETED | status=COMPLETED | WalletService và BagService được gọi; status → CLAIMED |
| TC-TSK-021 | [N] | Task chưa hoàn thành | status=IN_PROGRESS | Ném TaskNotCompletedException |
| TC-TSK-022 | [N] | Task đã claim rồi | status=CLAIMED | Ném TaskAlreadyClaimedException |
| TC-TSK-023 | [N] | Task NOT_STARTED | status=NOT_STARTED | Ném TaskNotCompletedException |
| TC-TSK-024 | [P] | Phần thưởng gold | reward chứa gold=500 | WalletService.batchAdd được gọi với gold=500 |
| TC-TSK-025 | [P] | Phần thưởng là item | reward chứa itemId=X, quantity=1 | BagService.grant được gọi |

### 7.4 `claimAllCompletedTasks(playerId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-TSK-030 | [P] | Claim nhiều task cùng lúc | player có 3 COMPLETED tasks | Tất cả 3 được CLAIMED, rewards tích luỹ |
| TC-TSK-031 | [P] | Không có task nào COMPLETED | tất cả tasks là NOT_STARTED/CLAIMED | Không có action nào, không lỗi |

---

## 8. EquipService

**File:** `equip-service/.../service/EquipService.java`
**Mô tả:** Trang bị/tháo trang bị, bán, phân giải.

### 8.1 `equip(EquipReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-EQP-001 | [P] | Trang bị vào slot trống | slot chưa có equipment | EquipSlot được tạo, item bị consume từ bag |
| TC-EQP-002 | [P] | Thay thế trang bị cũ | slot đã có equipment | Equipment cũ trả về bag, equipment mới vào slot |
| TC-EQP-003 | [N] | Item không phải equipment | itemId là potion | Ném InvalidItemTypeException |
| TC-EQP-004 | [N] | Item không có trong túi | itemId không trong bag | Ném ItemNotFoundException |
| TC-EQP-005 | [N] | Item không đúng slot | axe vào slot boots | Ném InvalidEquipSlotException |
| TC-EQP-006 | [N] | Level nhân vật không đủ | equip yêu cầu level 30, role level 10 | Ném LevelRequirementException |

### 8.2 `unequip(UnequipReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-EQP-010 | [P] | Tháo trang bị thành công | slot có equipment | Item trả về bag, slot trống |
| TC-EQP-011 | [N] | Slot trống | slot không có equipment | Ném EmptySlotException |
| TC-EQP-012 | [N] | Túi đồ đầy | bag đầy khi tháo | Ném BagFullException |

### 8.3 `computeSell(req)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-EQP-020 | [P] | Tính giá bán trang bị thường | quality=COMMON, level=1 | Trả về coin+exp đúng formula |
| TC-EQP-021 | [P] | Tính giá bán trang bị hiếm | quality=RARE, level=5 | Giá cao hơn COMMON cùng level |
| TC-EQP-022 | [P] | Businessman bonus | có businessman buff | Coin tính thêm bonus % |

### 8.4 `decompose(req)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-EQP-030 | [P] | Phân giải trang bị thành công | equipment hợp lệ | Equipment bị xóa, materials được thêm vào bag |
| TC-EQP-031 | [N] | Equipment không tồn tại | itemId không có | Ném ItemNotFoundException |

---

## 9. ShopService

**File:** `shop-service/.../service/ShopService.java`
**Mô tả:** Cửa hàng: mua bán vật phẩm, giới hạn mua, shop bí ẩn.

### 9.1 `buy(BuyReq)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SHP-001 | [P] | Mua item bằng gold thành công | đủ gold, trong hạn mức | Gold trừ, item được cấp vào bag |
| TC-SHP-002 | [P] | Mua item bằng gem thành công | đủ gem | Gem trừ, item được cấp |
| TC-SHP-003 | [N] | Không đủ tiền | balance < price | Ném InsufficientFundsException |
| TC-SHP-004 | [N] | Vượt giới hạn mua ngày (DAILY) | đã mua đủ quota ngày | Ném QuotaExceededException |
| TC-SHP-005 | [N] | Vượt giới hạn mua vĩnh viễn (FOREVER) | đã mua đủ quota total | Ném QuotaExceededException |
| TC-SHP-006 | [P] | Quota mới ngày hôm sau | dayStr thay đổi sang ngày mới | Quota daily reset, có thể mua lại |
| TC-SHP-007 | [N] | EntryIndex không hợp lệ | entryIndex ngoài danh sách shop | Ném ShopItemNotFoundException |
| TC-SHP-008 | [N] | Level nhân vật chưa đủ | item yêu cầu level 20, role level 5 | Ném LevelRequirementException |

### 9.2 `refreshMystery(roleId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SHP-010 | [P] | Refresh shop bí ẩn | roleId hợp lệ | Trả về danh sách items mới ngẫu nhiên |
| TC-SHP-011 | [P] | Refresh nhiều lần | gọi 5 lần | Items có thể khác mỗi lần (ngẫu nhiên) |

### 9.3 `info(roleId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-SHP-020 | [P] | Khởi tạo shop lần đầu | roleId mới | Mystery shop được khởi tạo, trả về thông tin đầy đủ |
| TC-SHP-021 | [P] | Lấy thông tin shop đã có | roleId đã từng mua | Trả về quota hiện tại, items còn lại |

---

## 10. MailService

**File:** `mail-service/.../service/MailService.java`
**Mô tả:** Hệ thống thư trong game: gửi thư, nhận thưởng từ thư, xóa thư.

### 10.1 `sendMail(SendMailRequest)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-MAI-001 | [P] | Gửi thư thường (không attachment) | receiverId hợp lệ, title, content | Mail được lưu, isRead=false, isClaimedAttachment=false |
| TC-MAI-002 | [P] | Gửi thư có attachment | kèm items | Mail và MailAttachment được lưu |
| TC-MAI-003 | [P] | Gửi thư có thời hạn | expiresAt = now + 7days | expiresAt lưu đúng |
| TC-MAI-004 | [N] | ReceiverId không tồn tại | receiverId=99999L | Ném PlayerNotFoundException |
| TC-MAI-005 | [N] | Title rỗng | title="" | Ném ValidationException |

### 10.2 `sendBulkMail(BulkMailRequest)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-MAI-010 | [P] | Gửi thư cho nhiều người | List<receiverId> 3 người | 3 Mail records được tạo |
| TC-MAI-011 | [P] | Gửi cho 1 người trong bulk | List chứa 1 receiverId | 1 Mail record được tạo |
| TC-MAI-012 | [N] | List rỗng | receivers=[] | Không tạo mail, không lỗi hoặc ném exception |

### 10.3 `claimAttachment(mailId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-MAI-020 | [P] | Nhận phần thưởng từ thư | mail có attachment chưa claim | BagService.grant được gọi; isClaimedAttachment=true |
| TC-MAI-021 | [N] | Thư không có attachment | attachments rỗng | Ném NoAttachmentException |
| TC-MAI-022 | [N] | Đã claim rồi | isClaimedAttachment=true | Ném AttachmentAlreadyClaimedException |
| TC-MAI-023 | [N] | Thư đã hết hạn | expiresAt < now | Ném MailExpiredException |
| TC-MAI-024 | [N] | MailId không tồn tại | mailId=99999L | Ném MailNotFoundException |

### 10.4 `deleteMail(mailId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-MAI-030 | [P] | Xóa thư không có attachment | mail thường | isDeleted=true |
| TC-MAI-031 | [P] | Xóa thư đã claim attachment | isClaimedAttachment=true | isDeleted=true |
| TC-MAI-032 | [N] | Xóa thư chưa claim attachment | isClaimedAttachment=false, có attachment | Ném CannotDeleteUnclaimedMailException |
| TC-MAI-033 | [N] | MailId không tồn tại | mailId=99999L | Ném MailNotFoundException |

### 10.5 `getMailList(roleId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-MAI-040 | [P] | Lấy danh sách thư | roleId có mail | Trả về list mails, unreadCount đúng |
| TC-MAI-041 | [P] | Không trả về mail đã xóa | mail có isDeleted=true | Không xuất hiện trong kết quả |
| TC-MAI-042 | [P] | Không trả về mail hết hạn | mail expiresAt < now | Không xuất hiện trong kết quả |

---

## 11. GuildService

**File:** `guild-service/.../service/GuildService.java`
**Mô tả:** Quản lý bang hội: tạo, gia nhập, quản trị thành viên, donate, nâng cấp công nghệ.

### 11.1 `createGuild(roleId, guildName, ...)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-GLD-001 | [P] | Tạo bang hội thành công | đủ gold (CREATE_GUILD_COST=10000) | Guild được tạo; Leader được thêm vào member (rank=3); Gold bị trừ |
| TC-GLD-002 | [N] | Không đủ gold | balance < 10000 | Ném InsufficientFundsException |
| TC-GLD-003 | [N] | Tên bang hội đã tồn tại | guildName trùng | Ném GuildNameExistsException |
| TC-GLD-004 | [N] | Player đang trong bang hội khác | roleId đã là member | Ném AlreadyInGuildException |
| TC-GLD-005 | [B] | Tên bang hội 1 ký tự | guildName="G" | Tạo thành công hoặc validation error |

### 11.2 `applyToGuild(roleId, guildId, message)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-GLD-010 | [P] | Nộp đơn xin vào bang | player không trong bang nào | GuildApplication với status=PENDING được tạo |
| TC-GLD-011 | [N] | Player đang trong bang hội | roleId đã là member | Ném AlreadyInGuildException |
| TC-GLD-012 | [N] | Đã có đơn pending | application PENDING tồn tại | Ném ApplicationAlreadyExistsException |
| TC-GLD-013 | [N] | Guild không tồn tại | guildId=99999L | Ném GuildNotFoundException |
| TC-GLD-014 | [N] | Guild đầy (50 thành viên) | memberCount=MAX_MEMBERS | Ném GuildFullException |

### 11.3 `processApplication(officerId, applicationId, approve)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-GLD-020 | [P] | Chấp nhận đơn | approve=true, officer có quyền | Applicant trở thành Member (rank=1); application status=APPROVED |
| TC-GLD-021 | [P] | Từ chối đơn | approve=false | application status=REJECTED; applicant không vào guild |
| TC-GLD-022 | [N] | Officer không có quyền (rank=Member) | rank=1 | Ném InsufficientPermissionException |
| TC-GLD-023 | [N] | Application không tồn tại | applicationId=99999L | Ném ApplicationNotFoundException |
| TC-GLD-024 | [N] | Application đã xử lý | status=APPROVED/REJECTED | Ném ApplicationAlreadyProcessedException |

### 11.4 `kickMember(leaderId/officerId, targetRoleId, guildId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-GLD-030 | [P] | Kick thành viên thường | leader kick member (rank=1) | Member bị xóa khỏi guild |
| TC-GLD-031 | [P] | Officer kick thành viên | officer (rank=2) kick member (rank=1) | Member bị xóa |
| TC-GLD-032 | [N] | Kick người có rank cao hơn | officer (rank=2) kick officer khác | Ném InsufficientPermissionException |
| TC-GLD-033 | [N] | Kick chính mình | leaderId = targetRoleId | Ném CannotKickSelfException |
| TC-GLD-034 | [N] | Target không trong guild | targetRoleId không phải member | Ném NotInGuildException |

### 11.5 `donate(roleId, guildId, amount)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-GLD-040 | [P] | Donate thành công | đủ gold | Gold bị trừ; guild.funds tăng; member.contribution tăng |
| TC-GLD-041 | [N] | Không đủ gold | balance < amount | Ném InsufficientFundsException |
| TC-GLD-042 | [N] | Không phải thành viên bang | roleId không là member | Ném NotInGuildException |

### 11.6 `upgradeTech(leaderId, guildId, techType)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-GLD-050 | [P] | Nâng cấp công nghệ thành công | đủ funds | techLevel tăng 1; funds giảm |
| TC-GLD-051 | [N] | Không đủ quỹ bang | funds < cost | Ném InsufficientGuildFundsException |
| TC-GLD-052 | [N] | Công nghệ đã đạt max level | techLevel = maxLevel | Ném TechMaxLevelException |
| TC-GLD-053 | [N] | Người gọi không phải leader | rank < 3 | Ném InsufficientPermissionException |

### 11.7 `disbandGuild(leaderId, guildId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-GLD-060 | [P] | Giải tán bang thành công | leaderId là leader | Guild.active=false; disbandedAt được set; tất cả members bị xóa |
| TC-GLD-061 | [N] | Không phải leader | rank < 3 | Ném InsufficientPermissionException |
| TC-GLD-062 | [N] | Guild không tồn tại | guildId=99999L | Ném GuildNotFoundException |

---

## 12. ArenaService

**File:** `arena-service/.../service/ArenaService.java`
**Mô tả:** Hệ thống PvP đấu trường: matchmaking, rating, rewards, giới hạn thử thách.

### 12.1 `processBattle(BattleRequest)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ARN-001 | [P] | Người có rating cao thắng | player1 rating 1500 vs player2 rating 1000 | player1 có xác suất thắng > 50%; rating thay đổi đúng |
| TC-ARN-002 | [P] | Rating bằng nhau | player1 vs player2 cùng rating 1000 | Xác suất thắng ~50% mỗi bên |
| TC-ARN-003 | [P] | Underdog thắng | player có rating thấp hơn thắng | Rating thay đổi nhiều hơn (upset bonus) |
| TC-ARN-004 | [P] | Consecutive win bonus | player thắng liên tiếp 3 lần | Rating có thêm CONSECUTIVE_WIN_BONUS=5 |
| TC-ARN-005 | [P] | Battle history được lưu | sau mỗi trận | ArenaBattleHistory record được tạo |
| TC-ARN-006 | [N] | Player không tồn tại | player1Id=99999L | Ném PlayerNotFoundException |
| TC-ARN-007 | [N] | Không còn thử thách | challengesUsedToday >= 10 | Ném NoChallengesRemainingException |

### 12.2 `findOpponent(playerId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ARN-010 | [P] | Tìm đối thủ trong vùng rating | player rating 1000 | Tìm opponent trong range [800, 1200] (±200) |
| TC-ARN-011 | [P] | Không tìm thấy đối thủ trong range | không ai trong ±200 | Trả về null hoặc bất kỳ opponent |
| TC-ARN-012 | [N] | Player không tồn tại | playerId=99999L | Ném PlayerNotFoundException |

### 12.3 `getChallengesRemaining(playerId)` và `consumeChallenge(playerId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ARN-020 | [P] | Lấy số thử thách còn lại | player mới | Trả về 10 (MAX) |
| TC-ARN-021 | [P] | Tiêu thụ 1 thử thách | challengesUsedToday=5 | Sau khi gọi: challengesUsedToday=6 |
| TC-ARN-022 | [P] | Reset hàng ngày | lastResetDate < today | challengesUsedToday reset về 0 |
| TC-ARN-023 | [N] | Hết thử thách | challengesUsedToday=10 | Ném NoChallengesRemainingException |

### 12.4 `getTop100Rankings()` và `getRankings(playerId)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ARN-030 | [P] | Lấy top 100 | DB có đủ player | Trả về list 100 player, sort theo rating DESC |
| TC-ARN-031 | [P] | Top 100 khi ít hơn 100 player | chỉ có 50 player | Trả về 50 player |
| TC-ARN-032 | [P] | Lấy rank của player | playerId hợp lệ | Trả về rank đúng |

### 12.5 `calculateRankReward(currentRank)`

| ID | Loại | Mô tả | Đầu vào | Kết quả mong đợi |
|----|------|-------|---------|-----------------|
| TC-ARN-040 | [P] | Top 10 nhận 10,000 gold | rank=5 | reward = 10000 |
| TC-ARN-041 | [P] | Top 11-50 nhận 5,000 gold | rank=25 | reward = 5000 |
| TC-ARN-042 | [P] | Top 51-100 nhận 2,000 gold | rank=75 | reward = 2000 |
| TC-ARN-043 | [P] | Ngoài top 100 nhận 500 gold | rank=150 | reward = 500 |
| TC-ARN-044 | [B] | Ranh giới rank=10 | rank=10 | reward = 10000 (nằm trong top 10) |
| TC-ARN-045 | [B] | Ranh giới rank=11 | rank=11 | reward = 5000 (top 11-50) |
| TC-ARN-046 | [B] | Ranh giới rank=100 | rank=100 | reward = 2000 (top 51-100) |
| TC-ARN-047 | [B] | Ranh giới rank=101 | rank=101 | reward = 500 (ngoài top 100) |

---

---

## 13. PetService

**File:** `pet-service/.../service/PetService.java`
**Mo ta:** Quan ly thu cung: mo khoa, len cap, tien hoa, ky nang, trang bi.

### 13.1 `activatePet(roleId, petTemplateId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-PET-001 | [P] | Mo khoa thu cung moi thanh cong | petTemplateId hop le, du nguyen lieu | Pet duoc tao voi index moi, nguyen lieu bi tru |
| TC-PET-002 | [N] | Khong du nguyen lieu | nguyen lieu < yeu cau | Nem InsufficientMaterialsException |
| TC-PET-003 | [N] | Thu cung da co san | petTemplateId da active | Nem PetAlreadyActiveException |
| TC-PET-004 | [N] | Khong con slot trong | tat ca slot da day | Nem PetSlotFullException |
| TC-PET-005 | [N] | petTemplateId khong hop le | id=99999 | Nem PetTemplateNotFoundException |

### 13.2 `upgradePet(roleId, petId, materialIds)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-PET-010 | [P] | Len cap thu cung thanh cong | du nguyen lieu | Pet.level tang 1, stat tang tuong ung |
| TC-PET-011 | [P] | Len cap nhieu lan lien tiep | goi 3 lan | Level tang dung 3 lan |
| TC-PET-012 | [N] | Nguyen lieu khong du | materialIds thieu | Nem InsufficientMaterialsException |
| TC-PET-013 | [N] | Pet da max level | level = MAX_LEVEL | Nem PetMaxLevelException |
| TC-PET-014 | [N] | petId khong thuoc roleId | petId cua role khac | Nem PetNotFoundException |

### 13.3 `evolvePet(roleId, petId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-PET-020 | [P] | Tien hoa thu cung thanh cong | du dieu kien level + grade | Pet.gradeId tang, stats reset/tang |
| TC-PET-021 | [N] | Chua du level de tien hoa | level < yeu cau | Nem LevelRequirementException |
| TC-PET-022 | [N] | Chua du grade de tien hoa | grade < yeu cau | Nem GradeRequirementException |
| TC-PET-023 | [N] | Pet da o tien hoa toi da | da o max evolution | Nem MaxEvolutionException |

### 13.4 `setActivePet(roleId, petId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-PET-030 | [P] | Chon thu cung chien dau | petId hop le, slot trong | Pet duoc danh dau la fight pet |
| TC-PET-031 | [P] | Thay the fight pet cu | da co 2 fight pets | Pet cu bi bo, pet moi vao slot |
| TC-PET-032 | [N] | petId khong thuoc roleId | petId cua role khac | Nem PetNotFoundException |

---

## 14. ItemService

**File:** `item-service/.../service/ItemService.java`
**Mo ta:** Quan ly metadata cua tat ca vat pham trong game.

### 14.1 `meta(itemId)` va `batch(ids)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ITM-001 | [P] | Lay metadata item hop le | itemId=1001 | Tra ve ItemMetaDTO day du |
| TC-ITM-002 | [N] | ItemId khong ton tai | itemId=99999 | Nem ItemNotFoundException hoac tra null |
| TC-ITM-003 | [P] | Batch lay nhieu item | ids=[1001,1002,1003] | Tra ve Map voi 3 entries |
| TC-ITM-004 | [P] | Batch co 1 id khong ton tai | ids=[1001, 99999] | Tra ve Map voi 1 entry hop le, bo qua id khong ton tai |
| TC-ITM-005 | [P] | Batch list rong | ids=[] | Tra ve Map rong |

### 14.2 `typeOf(itemId)`, `exists(itemId)`, `validStack(itemId, count)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ITM-010 | [P] | Lay loai item hop le | itemId=GOLD | Tra ve "VIRTUAL_CURRENCY" |
| TC-ITM-011 | [P] | Kiem tra item ton tai | itemId hop le | Tra ve true |
| TC-ITM-012 | [P] | Kiem tra item khong ton tai | itemId=99999 | Tra ve false |
| TC-ITM-013 | [P] | So luong stack hop le | itemId stackable, count <= maxStack | Tra ve true |
| TC-ITM-014 | [N] | So luong stack vuot qua gioi han | count > maxStack | Tra ve false |
| TC-ITM-015 | [B] | Count = 0 | count=0 | Tra ve false hoac true tuy rule |

---

## 15. MountService

**File:** `mount-service/.../service/MountServiceImpl.java`
**Mo ta:** Quan ly ngua/vat cuoi: mo khoa, len cap, tang cap, trang bi, skin.

### 15.1 `unlockMount(userId, mountId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-MNT-001 | [P] | Mo khoa ngua thanh cong | mountId hop le, du nguyen lieu | Mount duoc tao, nguyen lieu bi tru |
| TC-MNT-002 | [N] | Ngua da duoc mo | mountId da ton tai | Nem MountAlreadyUnlockedException |
| TC-MNT-003 | [N] | Khong du nguyen lieu | vat lieu < yeu cau | Nem InsufficientMaterialsException |
| TC-MNT-004 | [N] | MountId khong hop le | mountId=99999 | Nem MountTemplateNotFoundException |

### 15.2 `levelUpMount(userId, mountIndex)` va `gradeUpMount(userId, mountIndex)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-MNT-010 | [P] | Len cap ngua thanh cong | du exp/nguyen lieu | Mount.level tang 1, power tang |
| TC-MNT-011 | [N] | Ngua da max level | level = MAX | Nem MaxLevelException |
| TC-MNT-012 | [P] | Tang cap ngua thanh cong | du grade material | Mount.grade tang 1 |
| TC-MNT-013 | [N] | Ngua da max grade | grade = MAX_GRADE | Nem MaxGradeException |
| TC-MNT-014 | [N] | `canLevelUp` tra ve false | dieu kien khong du | Nem CannotLevelUpException |

### 15.3 `equipMount(userId, mountIndex)` va `unequipMount(userId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-MNT-020 | [P] | Trang bi ngua | mount hop le | Mount duoc danh dau equipped=true; ngua cu bi unequip |
| TC-MNT-021 | [P] | Thao ngua | dang co ngua equipped | Mount.equipped=false |
| TC-MNT-022 | [N] | Thao khi khong co ngua | chua equipped | Nem NoMountEquippedException |

### 15.4 `calculateMountPower(mount)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-MNT-030 | [P] | Tinh power ngua cap 1 | level=1, grade=1 | Power = base theo cong thuc |
| TC-MNT-031 | [P] | Tinh power ngua cap cao | level=50, grade=5 | Power cao hon level 1 |
| TC-MNT-032 | [B] | Mount null | mount=null | Nem NullPointerException hoac tra 0 |

---

## 16. ArtifactService

**File:** `artifact-service/.../service/ArtifactServiceImpl.java`
**Mo ta:** Quan ly bo boi/than khiet: mo khoa, len cap, tinh luyen, thuc tinh.

### 16.1 `unlockArtifact(userId, artifactId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ART-001 | [P] | Mo khoa artifact thanh cong | artifactId hop le, du dieu kien | Artifact duoc tao voi stats mac dinh |
| TC-ART-002 | [N] | Artifact da mo | da ton tai | Nem ArtifactAlreadyUnlockedException |
| TC-ART-003 | [N] | Khong du nguyen lieu | material < yeu cau | Nem InsufficientMaterialsException |

### 16.2 `levelUpArtifact` va `gradeUpArtifact`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ART-010 | [P] | Len cap artifact | du exp | level tang 1, power tang |
| TC-ART-011 | [N] | Da max level | level = MAX | Nem MaxLevelException |
| TC-ART-012 | [P] | Tang cap artifact | du grade material | grade tang 1, new stats unlock |
| TC-ART-013 | [N] | Da max grade | grade = MAX_GRADE | Nem MaxGradeException |

### 16.3 `refineArtifact(userId, artifactIndex)` va `awakenArtifact(userId, artifactIndex)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ART-020 | [P] | Tinh luyen artifact | du vat lieu tinh luyen | Sub-stats duoc refresh ngau nhien |
| TC-ART-021 | [N] | Khong du vat lieu tinh luyen | material < yeu cau | Nem InsufficientMaterialsException |
| TC-ART-022 | [N] | `canRefine` false | dieu kien khong dat | Nem CannotRefineException |
| TC-ART-023 | [P] | Thuc tinh artifact | du dieu kien | Artifact.awaken=true, bonus stats mo khoa |
| TC-ART-024 | [N] | `canAwaken` false | chua du level/grade | Nem CannotAwakenException |

### 16.4 `refreshAttributes(userId, artifactIndex, lockFlag)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ART-030 | [P] | Refresh tat ca attributes | lockFlag=0 | Tat ca sub-stats duoc roll lai |
| TC-ART-031 | [P] | Refresh voi 1 stat bi khoa | lockFlag=1 (bit 0) | Stat bi khoa giu nguyen, con lai roll moi |
| TC-ART-032 | [N] | Khong du nguyen lieu refresh | mat chi refresh < yeu cau | Nem InsufficientMaterialsException |

---

## 17. RuneService

**File:** `rune-service/.../service/RuneServiceImpl.java`
**Mo ta:** Quan ly ngoc: tao, xoa, len cap, tinh luyen, lap vao slot trang bi.

### 17.1 `createRune(userId, runeId, quality)` va `deleteRune(userId, runeIndex)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RUN-001 | [P] | Tao ngoc moi | runeId hop le, quality hop le | Rune duoc tao voi index moi |
| TC-RUN-002 | [N] | runeId khong hop le | runeId=99999 | Nem RuneTemplateNotFoundException |
| TC-RUN-003 | [P] | Xoa ngoc khong dang trang bi | equipped=false | Rune bi xoa |
| TC-RUN-004 | [N] | Xoa ngoc dang trang bi | equipped=true | Nem RuneEquippedException |
| TC-RUN-005 | [N] | Xoa ngoc khong ton tai | runeIndex sai | Nem RuneNotFoundException |

### 17.2 `levelUpRune`, `upgradeRuneQuality`, `upgradeRuneStar`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RUN-010 | [P] | Len cap ngoc | du exp | rune.level tang, stat tang |
| TC-RUN-011 | [N] | Ngoc max level | level = MAX | Nem MaxLevelException |
| TC-RUN-012 | [P] | Tang chat luong ngoc | du nguyen lieu | rune.quality tang, new stat unlock |
| TC-RUN-013 | [N] | Ngoc max quality | quality = MAX_QUALITY | Nem MaxQualityException |
| TC-RUN-014 | [P] | Tang sao ngoc | du nguyen lieu | rune.starLevel tang |
| TC-RUN-015 | [N] | Ngoc max sao | starLevel = MAX_STAR | Nem MaxStarException |

### 17.3 `equipRune(userId, runeIndex, equipSlot)` va `unequipRune`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RUN-020 | [P] | Lap ngoc vao slot trong | slot trong | rune.equipSlot duoc set |
| TC-RUN-021 | [P] | Thay ngoc cu trong slot | slot da co ngoc | Ngoc cu bi unequip, ngoc moi vao slot |
| TC-RUN-022 | [N] | Slot khong hop le | equipSlot=99 | Nem InvalidSlotException |
| TC-RUN-023 | [P] | Thao ngoc | rune dang equipped | rune.equipSlot = null |

### 17.4 `calculateTotalRunePower(userId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RUN-030 | [P] | Tinh tong power cua tat ca ngoc | userId co 5 runes | Tra ve tong power dung |
| TC-RUN-031 | [P] | Khong co ngoc nao | userId chua co rune | Tra ve 0 |

---

## 18. BoxService

**File:** `box-service/.../service/BoxService.java`
**Mo ta:** Quan ly hop bao: mo hop, nhan may man, phan giai, cai dat.

### 18.1 `open(OpenReq)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-BOX-001 | [P] | Mo hop bao thanh cong | co hop trong bag | Item duoc cap vao bag, hop bi tru |
| TC-BOX-002 | [N] | Khong co hop | so luong = 0 | Nem InsufficientItemException |
| TC-BOX-003 | [P] | Mo nhieu hop cung luc | quantity=10 | 10 ket qua duoc tra ve |

### 18.2 `levelUp(roleId)` va `levelReward(roleId, idx)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-BOX-010 | [P] | Len cap box system | du dieu kien | boxLevel tang, thu nhap moi mo khoa |
| TC-BOX-011 | [N] | Box da max level | level = MAX | Nem MaxLevelException |
| TC-BOX-012 | [P] | Nhan phan thuong theo level | idx hop le, chua nhan | Reward duoc cap, idx danh dau claimed |
| TC-BOX-013 | [N] | Phan thuong da nhan | idx da claimed | Nem RewardAlreadyClaimedException |

### 18.3 `luckReceive(roleId, seq)` va `quicken(roleId, num)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-BOX-020 | [P] | Nhan phan thuong may man | seq hop le | Reward duoc cap |
| TC-BOX-021 | [N] | seq khong hop le | seq=99 | Nem InvalidSeqException |
| TC-BOX-022 | [P] | Tang toc mo hop | num=5 | 5 lan mo hop duoc xu ly nhanh |
| TC-BOX-023 | [N] | Khong du gold de tang toc | balance < cost | Nem InsufficientFundsException |

### 18.4 `decompose(roleId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-BOX-030 | [P] | Phan giai items thanh cong | co items co the phan giai | Items bi xoa, nguyen lieu duoc cap |
| TC-BOX-031 | [P] | Khong co gi de phan giai | bag rong | Tra ve DecomposeResp rong, khong loi |

---

## 19. CraftingService

**File:** `crafting-service/.../service/CraftingService.java`
**Mo ta:** He thong che tao: xem cong thuc, bat dau che tao, nhan san pham.

### 19.1 `getRecipes(roleId, level)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CRF-001 | [P] | Lay tat ca cong thuc | level=null | Tra ve tat ca RecipeInfo |
| TC-CRF-002 | [P] | Loc cong thuc theo level | level=10 | Chi tra cong thuc yeu cau level <= 10 |
| TC-CRF-003 | [P] | Khong co cong thuc nao | DB rong | Tra ve list rong |

### 19.2 `startCraft(CraftRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CRF-010 | [P] | Bat dau che tao thanh cong | du nguyen lieu va gold | CraftingStatus duoc tao, nguyen lieu bi tru |
| TC-CRF-011 | [N] | Thieu nguyen lieu | material < yeu cau | Nem InsufficientMaterialsException |
| TC-CRF-012 | [N] | Khong du gold | balance < cost | Nem InsufficientFundsException |
| TC-CRF-013 | [N] | RecipeId khong ton tai | recipeId=99999 | Nem RecipeNotFoundException |
| TC-CRF-014 | [N] | Dang co craft chua xong | status=IN_PROGRESS | Nem CraftingInProgressException |

### 19.3 `claim(ClaimRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CRF-020 | [P] | Nhan san pham hoan thanh | craft COMPLETED | Item duoc cap vao bag, status=CLAIMED |
| TC-CRF-021 | [N] | Craft chua hoan thanh | remainingTime > 0 | Nem CraftingNotFinishedException |
| TC-CRF-022 | [N] | Khong co craft nao | craftId khong ton tai | Nem CraftingNotFoundException |

---

## 20. PityService

**File:** `drop-service/.../service/PityService.java`
**Mo ta:** He thong pity (bao dam do hiem): dem so lan mo khong co rare, reset khi co.

### 20.1 `incr(group, roleId)` va `get(group, roleId)` va `reset(group, roleId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-PTY-001 | [P] | Tang counter pity | group="banner_A", roleId hop le | Tra ve gia tri moi = gia tri cu + 1 |
| TC-PTY-002 | [P] | Lay counter hien tai | group="banner_A", roleId hop le | Tra ve so nguyen >= 0 |
| TC-PTY-003 | [P] | Lay counter chua ton tai | roleId moi | Tra ve 0 |
| TC-PTY-004 | [P] | Reset counter | counter=89 | Counter = 0 sau reset |
| TC-PTY-005 | [I] | Reset 2 lan | goi reset 2 lan | Counter van = 0, khong loi |

### 20.2 `thresholdFor(dropId)` va `rareListFor(dropId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-PTY-010 | [P] | Lay nguong pity | dropId hop le | Tra ve nguong (vi du: 90) |
| TC-PTY-011 | [P] | Lay danh sach rare items | dropId hop le | Tra ve List<Integer> khong rong |
| TC-PTY-012 | [N] | dropId khong ton tai | dropId=99999 | Tra ve default hoac nem exception |

---

## 21. GiftService

**File:** `gift-service/.../service/GiftService.java`
**Mo ta:** Quan ly hop qua: xem thong tin, mo hop qua lay item.

### 21.1 `info(giftItemId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-GFT-001 | [P] | Lay thong tin hop qua | giftItemId hop le | Tra ve GiftInfoResp voi item pool |
| TC-GFT-002 | [N] | giftItemId khong ton tai | giftItemId=99999 | Nem GiftNotFoundException |

### 21.2 `open(OpenReq)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-GFT-010 | [P] | Mo hop qua thanh cong | co hop trong bag | Item ngau nhien duoc chon, cap vao bag, hop bi tru |
| TC-GFT-011 | [N] | Khong co hop qua | so luong = 0 | Nem InsufficientItemException |
| TC-GFT-012 | [P] | Item pool co nhieu item | pool lon | Item duoc chon theo ty le xac suat |

---

## 22. FriendService

**File:** `friend-service/.../service/FriendService.java`
**Mo ta:** Quan ly ban be, loi moi, chan, tang qua.

### 22.1 `sendFriendRequest(AddFriendRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-FRD-001 | [P] | Gui loi moi ket ban | target hop le, chua la ban | FriendRequest duoc tao voi status=PENDING |
| TC-FRD-002 | [N] | Da la ban be | target da la friend | Nem AlreadyFriendsException |
| TC-FRD-003 | [N] | Da gui loi moi roi | request PENDING da ton tai | Nem RequestAlreadySentException |
| TC-FRD-004 | [N] | Target da chan minh | bi block boi target | Nem BlockedException |
| TC-FRD-005 | [N] | Target khong ton tai | targetId=99999 | Nem PlayerNotFoundException |
| TC-FRD-006 | [N] | Tu gui loi moi cho chinh minh | roleId = targetId | Nem CannotAddSelfException |

### 22.2 `handleFriendRequest(HandleRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-FRD-010 | [P] | Chap nhan loi moi ket ban | approve=true | Ca 2 duoc them vao danh sach ban; request status=ACCEPTED |
| TC-FRD-011 | [P] | Tu choi loi moi | approve=false | request status=REJECTED; khong them ban |
| TC-FRD-012 | [N] | Request khong ton tai | requestId=99999 | Nem RequestNotFoundException |
| TC-FRD-013 | [N] | Request da xu ly | status=ACCEPTED/REJECTED | Nem RequestAlreadyProcessedException |

### 22.3 `removeFriend(roleId, friendId)` va `blockPlayer(BlockRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-FRD-020 | [P] | Xoa ban be | ca 2 la ban | Ca 2 bi xoa khoi danh sach ban cua nhau |
| TC-FRD-021 | [N] | Xoa nguoi khong phai ban | target khong phai friend | Nem NotFriendsException |
| TC-FRD-022 | [P] | Chan nguoi choi | blockerId != blockedId | BlockedRecord duoc tao |
| TC-FRD-023 | [N] | Chan chinh minh | blockerId = blockedId | Nem CannotBlockSelfException |
| TC-FRD-024 | [N] | Da chan roi | da co block record | Nem AlreadyBlockedException |

### 22.4 `giveGift(GiveGiftRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-FRD-030 | [P] | Tang qua cho ban | ca 2 la ban, du dieu kien | Item cua nguoi tang bi tru, friendship points tang |
| TC-FRD-031 | [N] | Target khong phai ban | khong trong friend list | Nem NotFriendsException |
| TC-FRD-032 | [N] | Khong du item de tang | so luong < yeu cau | Nem InsufficientItemException |

---

## 23. ChatService

**File:** `chat-service/.../service/ChatService.java`
**Mo ta:** He thong chat: gui tin nhan, lich su, cam chat.

### 23.1 `sendMessage(SendMessageRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CHT-001 | [P] | Gui tin nhan kenh the gioi | channel=WORLD, content hop le | MessageInfo duoc luu, tra ve MessageInfo |
| TC-CHT-002 | [P] | Gui tin nhan rieng tu | channel=PRIVATE, targetId hop le | MessageInfo duoc luu cho ca 2 |
| TC-CHT-003 | [P] | Gui tin nhan bang hoi | channel=GUILD | MessageInfo duoc broadcast cho guild |
| TC-CHT-004 | [N] | Nguoi gui dang bi cam chat | roleId bi mute | Nem PlayerMutedException |
| TC-CHT-005 | [N] | Noi dung rong | content="" | Nem ValidationException |
| TC-CHT-006 | [N] | Noi dung qua dai | content > maxLength | Nem ContentTooLongException |
| TC-CHT-007 | [N] | Channel khong hop le | channel="UNKNOWN" | Nem InvalidChannelException |

### 23.2 `getHistory(GetHistoryRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CHT-010 | [P] | Lay lich su chat | channel=WORLD, limit=50 | Tra ve list MessageInfo moi nhat |
| TC-CHT-011 | [P] | Lich su rong | chua co tin nhan | Tra ve list rong |

### 23.3 `mutePlayer` va `unmutePlayer`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CHT-020 | [P] | Cam chat nguoi choi | roleId hop le, duration=60 (phut) | MuteRecord duoc tao, isUserMuted=true |
| TC-CHT-021 | [P] | Bo cam chat | roleId dang bi mute | MuteRecord bi xoa/vo hieu, isUserMuted=false |
| TC-CHT-022 | [N] | Bo cam cho nguoi khong bi cam | roleId khong bi mute | Nem NotMutedException hoac no-op |

---

## 24. LeaderboardService

**File:** `leaderboard-service/.../service/LeaderboardService.java`
**Mo ta:** Quan ly bang xep hang: cap nhat diem, lay bang xep hang.

### 24.1 `updateScore(UpdateScoreRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-LDB-001 | [P] | Cap nhat diem xep hang | roleId hop le, score moi | RankingInfo duoc cap nhat, rank duoc tinh lai |
| TC-LDB-002 | [P] | Diem thap hon diem cu | newScore < currentScore | Khong cap nhat hoac cap nhat tuy rule |
| TC-LDB-003 | [N] | RoleId khong ton tai | roleId=99999 | Nem PlayerNotFoundException |

### 24.2 `getLeaderboard(rankingType, currentRoleId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-LDB-010 | [P] | Lay bang xep hang day du | rankingType=FIGHT_POWER | Tra ve top players + rank cua currentRoleId |
| TC-LDB-011 | [P] | currentRoleId khong trong top | rank > 100 | Tra ve rank chinh xac cua player ngoai top |
| TC-LDB-012 | [N] | rankingType khong hop le | rankingType=99 | Nem InvalidRankingTypeException |

---

## 25. RankingService

**File:** `leaderboard-service/.../service/RankingService.java`
**Mo ta:** Quan ly bang xep hang Redis (sorted set): arena, season, trial.

### 25.1 `updateArenaRanking` va `getTopArenaPlayers`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RNK-001 | [P] | Cap nhat arena rank sau khi thang | roleId, rating=1200 | Redis sorted set duoc cap nhat voi score=1200 |
| TC-RNK-002 | [P] | Cap nhat arena rank sau khi thua | rating=900 | Score duoc cap nhat dung |
| TC-RNK-003 | [P] | Lay top 10 arena | topN=10 | Tra ve 10 player co rating cao nhat |
| TC-RNK-004 | [P] | Lay top khi it nguoi | topN=10, chi co 5 player | Tra ve 5 player |

### 25.2 `updateTrialRanking` va `getTopTrialPlayers`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RNK-010 | [P] | Cap nhat trial rank | trialId=1, score=5000, stars=3 | Score duoc luu dung trong Redis |
| TC-RNK-011 | [P] | Lay top trial players | trialId=1, topN=10 | Tra ve top 10 cho trial do |

---

## 26. WorldService

**File:** `world-service/.../service/WorldService.java`
**Mo ta:** Quan ly trang thai the gioi: su kien, boss, trang thai server.

### 26.1 `getGlobalState()` va `updateGlobalState`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-WLD-001 | [P] | Lay trang thai the gioi | - | Tra ve WorldStateDTO voi onlinePlayers, serverStatus |
| TC-WLD-002 | [P] | Cap nhat trang thai | stateData={"maintenance":true} | WorldState duoc cap nhat, luu DB |
| TC-WLD-003 | [P] | Trang thai bao tri | serverStatus=MAINTENANCE | Tra ve maintenance=true |

### 26.2 `spawnBoss` va `updateBossHp`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-WLD-010 | [P] | Tao boss the gioi | bossName, level, maxHp | WorldBoss duoc tao voi hp=maxHp, active=true |
| TC-WLD-011 | [P] | Giam HP boss | damage=1000, attackerId hop le | boss.hp giam 1000, killer duoc ghi nhan |
| TC-WLD-012 | [P] | Boss chet khi HP = 0 | damage >= remainingHp | boss.alive=false, killTime duoc ghi nhan |
| TC-WLD-013 | [N] | Boss khong ton tai | bossId=99999 | Nem BossNotFoundException |

### 26.3 `createEvent` va `activateEvent` / `deactivateEvent`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-WLD-020 | [P] | Tao su kien moi | event hop le | WorldEvent duoc tao, active=false |
| TC-WLD-021 | [P] | Bat su kien | eventId hop le | event.active=true |
| TC-WLD-022 | [P] | Tat su kien | eventId dang active | event.active=false |
| TC-WLD-023 | [N] | Bat su kien da active | eventId da active | Khong loi hoac Nem EventAlreadyActiveException |

---

## 27. SceneManagementService

**File:** `world-service/.../service/SceneManagementService.java`
**Mo ta:** Quan ly scene: vao/ra scene, cap nhat vi tri, nhat item, tuong tac NPC.

### 27.1 `enterScene` va `leaveScene`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-SCN-001 | [P] | Vao scene thanh cong | roleId, sceneId hop le, vi tri hop le | roleId duoc them vao playersInScene |
| TC-SCN-002 | [N] | SceneId khong ton tai | sceneId=99999 | Nem SceneNotFoundException |
| TC-SCN-003 | [P] | Ra khoi scene | roleId dang o trong scene | roleId bi xoa khoi playersInScene |
| TC-SCN-004 | [N] | Ra khoi scene chua vao | roleId khong trong scene | Nem PlayerNotInSceneException hoac no-op |

### 27.2 `updatePosition`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-SCN-010 | [P] | Cap nhat vi tri hop le | toa do hop le, speed binh thuong | Vi tri duoc cap nhat thanh cong |
| TC-SCN-011 | [N] | Speed qua nhanh (speedhack) | speed > MAX_SPEED | AntiCheat duoc kich hoat, bao cao SuspiciousActivity |
| TC-SCN-012 | [N] | Teleport tu xa (cheat) | distance di chuyen > MAX_DISTANCE_PER_TICK | AntiCheat duoc kich hoat |

### 27.3 `pickupItem` va `interactNpc`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-SCN-020 | [P] | Nhat item trong scene | itemUid ton tai trong scene | Item bi xoa khoi scene, duoc cap vao bag |
| TC-SCN-021 | [N] | Item khong ton tai trong scene | itemUid khong co | Nem ItemNotFoundException |
| TC-SCN-022 | [P] | Tuong tac NPC dialog | npcId hop le, interactType=DIALOG | Tra ve dialog content |
| TC-SCN-023 | [P] | Tuong tac NPC shop | interactType=SHOP | Tra ve shop info |
| TC-SCN-024 | [N] | NPC khong ton tai | npcId=99999 | Nem NpcNotFoundException |

---

## 28. AnalyticsService

**File:** `analytics-service/.../service/AnalyticsService.java`
**Mo ta:** Thu thap va phan tich du lieu hanh vi nguoi choi, KPI.

### 28.1 `trackEvent(playerId, eventType, eventCategory, eventData, sessionId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ANL-001 | [P] | Ghi nhan su kien mua hang | eventType="PURCHASE", data hop le | PlayerEvent duoc luu, KPI duoc cap nhat |
| TC-ANL-002 | [P] | Ghi nhan su kien dang nhap | eventType="LOGIN" | PlayerEvent duoc luu, loginCount tang |
| TC-ANL-003 | [P] | Ghi nhan nhieu su kien | goi 5 lan | 5 PlayerEvent records duoc tao |
| TC-ANL-004 | [N] | PlayerId am | playerId=-1 | Nem ValidationException |

### 28.2 `getPlayerKpi` va `getTopSpenders`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ANL-010 | [P] | Lay KPI ngay cu the | playerId, date hop le | Tra ve PlayerKpi dung ngay |
| TC-ANL-011 | [P] | Lay top spenders | since = 7 ngay truoc | Tra ve list sap xep theo chi tieu giam dan |
| TC-ANL-012 | [P] | Lay most active users | since = 30 ngay truoc | Tra ve list sap xep theo session count giam dan |
| TC-ANL-013 | [P] | Khong co du lieu | playerId moi, chua co event | Tra ve null hoac PlayerKpi rong |

---

## 29. AntiCheatService

**File:** `anti-cheat-service/.../service/AntiCheatService.java`
**Mo ta:** Phat hien gian lan: speed hack, damage hack, resource hack.

### 29.1 `reportMovement(userId, x, y, z, speed)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ACH-001 | [P] | Di chuyen toc do binh thuong | speed <= MAX_SPEED | Khong tao bao cao gian lan |
| TC-ACH-002 | [N] | Di chuyen qua nhanh | speed = MAX_SPEED * 2 | SuspiciousActivity duoc tao, CheatReport neu du bao cao |
| TC-ACH-003 | [N] | Teleport (nhay xa) | toa do thay doi dot ngot | SuspiciousActivity duoc tao |

### 29.2 `reportDamage` va `reportResourceGain`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ACH-010 | [P] | Damage hop le | damage = expectedDamage | Khong tao bao cao |
| TC-ACH-011 | [N] | Damage qua cao | damage = expectedDamage * 10 | SuspiciousActivity duoc tao |
| TC-ACH-012 | [N] | Tai nguyen nhan duoc vuot muc | amount = expectedAmount * 5 | SuspiciousActivity duoc tao |

### 29.3 `reviewCheatReport` va `analyzePlayerBehavior`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ACH-020 | [P] | GM xem xet bao cao | reportId hop le, status=CONFIRMED | CheatReport.status=CONFIRMED, action duoc ghi nhan |
| TC-ACH-021 | [N] | Report khong ton tai | reportId=99999 | Nem ReportNotFoundException |
| TC-ACH-022 | [P] | Phan tich hanh vi 7 ngay | userId, days=7 | Tra ve Map voi thong ke day du |

---

## 30. ModerationService

**File:** `moderation-service/.../service/ModerationService.java`
**Mo ta:** Kiem duyet noi dung: loc tu ngu, bao cao vi pham, cam nguoi dung.

### 30.1 `filterMessage(userId, message)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-MOD-001 | [P] | Tin nhan sach | noi dung hop le | Tra ve {filtered: false, message: original} |
| TC-MOD-002 | [P] | Tin nhan co tu cam | chua tu xau | Tra ve {filtered: true, message: censored} |
| TC-MOD-003 | [N] | Nguoi gui dang bi ban | isUserBanned=true | Nem UserBannedException |
| TC-MOD-004 | [N] | Nguoi gui dang bi mute | isUserMuted=true | Nem UserMutedException |

### 30.2 `muteUser` va `banUser`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-MOD-010 | [P] | Cam chat 60 phut | userId, hours=1, reason hop le | isUserMuted=true; muteExpiresAt duoc set |
| TC-MOD-011 | [P] | Cam tai khoan 24h | userId, hours=24 | isUserBanned=true; banExpiresAt duoc set |
| TC-MOD-012 | [P] | Cam vinh vien | hours=0 hoac Integer.MAX | Ban vinh vien |
| TC-MOD-013 | [N] | UserId khong ton tai | userId=99999 | Nem UserNotFoundException |

### 30.3 `recordViolation` va `handleReport`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-MOD-020 | [P] | Ghi nhan vi pham nhe | severity=1 | Violation duoc luu |
| TC-MOD-021 | [P] | Vi pham nghiem trong tu dong cam | severity=10, vuot nguong tu dong cam | User bi tu dong ban |
| TC-MOD-022 | [P] | Xu ly bao cao chap nhan | approve=true | Report duoc dong, action duoc thuc hien |
| TC-MOD-023 | [P] | Xu ly bao cao tu choi | approve=false | Report duoc dong, khong action |

---

## 31. ReportEventService

**File:** `report-service/.../service/ReportEventService.java`
**Mo ta:** Quan ly su kien bao cao tu client (device, session data).

### 31.1 `processReportToDTO(data)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RPT-001 | [P] | Xu ly data base64 hop le | data = base64 hop le | ReportResultDTO day du |
| TC-RPT-002 | [N] | Data khong phai base64 | data = "not-base64" | Nem IllegalArgumentException hoac ParseException |
| TC-RPT-003 | [N] | Data null | data = null | Nem NullPointerException hoac ValidationException |

### 31.2 `findByType`, `findByDateRange`, `getStatistics`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-RPT-010 | [P] | Tim kiem bao cao theo loai | type=1 | Tra ve list ReportEvent dung loai |
| TC-RPT-011 | [P] | Tim kiem theo khoang thoi gian | start, end hop le | Chi tra event trong khoang thoi gian |
| TC-RPT-012 | [P] | Lay thong ke | - | Tra ve ReportStatsDTO voi tong so, phan loai |

---

## 32. NotificationService

**File:** `notification-service/.../service/NotificationService.java`
**Mo ta:** Quan ly thong bao: tao, gui, doc, lay danh sach.

### 32.1 `createNotification(playerId, type, title, message, data)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-NTF-001 | [P] | Tao thong bao IN_GAME | type="IN_GAME" | Notification duoc tao, status=PENDING |
| TC-NTF-002 | [P] | Tao thong bao PUSH | type="PUSH" | Notification duoc tao, goi push service |
| TC-NTF-003 | [P] | Tao thong bao EMAIL | type="EMAIL" | Notification duoc tao, goi email service |
| TC-NTF-004 | [N] | PlayerId khong ton tai | playerId=99999 | Nem PlayerNotFoundException |
| TC-NTF-005 | [N] | Type khong hop le | type="UNKNOWN" | Nem InvalidNotificationTypeException |

### 32.2 `markAsRead`, `getUnreadNotifications`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-NTF-010 | [P] | Danh dau da doc | notificationId hop le | Notification.isRead=true, readAt duoc set |
| TC-NTF-011 | [N] | notificationId khong ton tai | notificationId=99999 | Nem NotificationNotFoundException |
| TC-NTF-012 | [P] | Lay danh sach chua doc | playerId co 3 unread | Tra ve 3 notifications |
| TC-NTF-013 | [P] | Tat ca da doc | khong co unread | Tra ve list rong |

---

## 33. IapVerifyService

**File:** `iap-verify-service/.../service/IapVerifyService.java`
**Mo ta:** Xac minh giao dich mua hang trong ung dung (Google Play, App Store).

### 33.1 `verifyPurchase(userId, platform, productId, purchaseToken, packageName)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-IAP-001 | [P] | Xac minh Google Play hop le | platform=GOOGLE, token hop le | Purchase duoc tao, status=VERIFIED |
| TC-IAP-002 | [P] | Xac minh App Store hop le | platform=APPLE, token hop le | Purchase duoc tao, status=VERIFIED |
| TC-IAP-003 | [N] | Token gia mao | token khong hop le voi Google/Apple | Nem PurchaseVerificationException |
| TC-IAP-004 | [N] | Token da su dung (duplicate) | token da ton tai trong DB | Nem DuplicatePurchaseException |
| TC-IAP-005 | [N] | Platform khong ho tro | platform="UNKNOWN" | Nem UnsupportedPlatformException |

### 33.2 `consumePurchase` va `createRefundRequest`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-IAP-010 | [P] | Consume purchase thanh cong | purchaseId co status=VERIFIED | Purchase.status=CONSUMED |
| TC-IAP-011 | [N] | Consume purchase chua verify | status=PENDING | Nem PurchaseNotVerifiedException |
| TC-IAP-012 | [P] | Tao yeu cau hoan tien | purchaseId hop le, reason hop le | RefundRequest duoc tao voi status=PENDING |
| TC-IAP-013 | [N] | Purchase da hoat toan hoac bi huy | status=REFUNDED | Nem CannotRefundException |

### 33.3 `getSuspiciousPurchases`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-IAP-020 | [P] | Lay giao dich nghi ngo | threshold=70 | Chi tra purchase co fraudScore >= 70 |
| TC-IAP-021 | [P] | Khong co giao dich nghi ngo | tat ca fraudScore < threshold | Tra ve list rong |

---

## 34. GMService

**File:** `gm-service/.../service/GMService.java`
**Mo ta:** Cong cu quan tri: cap/xoa item, dieu chinh tien te, cam tai khoan, broadcast.

### 34.1 `giveItems` va `removeItems`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-GMX-001 | [P] | GM cap item cho player | gmId hop le, playerId hop le, itemId hop le | BagService.grant duoc goi; GMActionLog duoc tao |
| TC-GMX-002 | [N] | Player khong ton tai | playerId=99999 | Nem PlayerNotFoundException |
| TC-GMX-003 | [N] | ItemId khong hop le | itemId=99999 | Nem ItemNotFoundException |
| TC-GMX-004 | [P] | GM xoa item cua player | itemId va quantity hop le | BagService.use duoc goi; GMActionLog duoc tao |
| TC-GMX-005 | [N] | Player khong du item de xoa | so luong < yeu cau | Nem InsufficientItemException |

### 34.2 `addCurrency` va `deductCurrency`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-GMX-010 | [P] | GM them tien cho player | currencyType=GOLD, amount=10000 | WalletService.batchAdd duoc goi; log duoc tao |
| TC-GMX-011 | [P] | GM tru tien cua player | currencyType=GEM, amount=100 | WalletService.batchCost duoc goi |
| TC-GMX-012 | [N] | Player khong du tien de tru | balance < amount | Nem InsufficientFundsException |

### 34.3 `banUser` va `broadcastMessage`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-GMX-020 | [P] | Ban user | userId hop le, durationDays=7 | User.status=BANNED; GMActionLog duoc tao |
| TC-GMX-021 | [P] | Unban user | userId dang bi ban | User.status=ACTIVE; GMActionLog duoc tao |
| TC-GMX-022 | [N] | Ban user khong ton tai | userId=99999 | Nem UserNotFoundException |
| TC-GMX-023 | [P] | Broadcast tin nhan | message hop le | Tra ve "OK"; log broadcast duoc tao |

---

## 35. ShizhuangService

**File:** `shizhuang-service/.../service/ShizhuangService.java`
**Mo ta:** Quan ly trang phuc dac biet (shizhuang): danh sach, kich hoat, mac, len cap.

### 35.1 `listByRole(roleId)` va `getInfo(roleId, shizhuangId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-SHZ-001 | [P] | Lay danh sach shizhuang | roleId hop le | Tra ve ShizhuangListResp day du |
| TC-SHZ-002 | [P] | Role chua co shizhuang nao | roleId moi | Tra ve list rong |
| TC-SHZ-003 | [P] | Lay thong tin 1 shizhuang | roleId, shizhuangId hop le | Tra ve ShizhuangInfo cu the |
| TC-SHZ-004 | [N] | shizhuangId khong ton tai | shizhuangId=99999 | Nem ShizhuangNotFoundException |

### 35.2 `activate(ActivateReq)` va `wear(WearReq)` va `levelUp(LevelUpReq)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-SHZ-010 | [P] | Kich hoat shizhuang moi | du dieu kien | Shizhuang duoc mo khoa cho role |
| TC-SHZ-011 | [N] | Da kich hoat | shizhuangId da active | Nem ShizhuangAlreadyActiveException |
| TC-SHZ-012 | [P] | Mac shizhuang | shizhuang da mo khoa | Shizhuang duoc mac, cai cu duoc thao |
| TC-SHZ-013 | [N] | Mac shizhuang chua kich hoat | chua unlock | Nem ShizhuangNotActivatedException |
| TC-SHZ-014 | [P] | Len cap shizhuang | du gold | shizhuang.level tang, gold bi tru |
| TC-SHZ-015 | [N] | Khong du gold | balance < cost | Nem InsufficientFundsException |

---

## 36. AngelService

**File:** `angel-service/.../service/AngelServiceImpl.java`
**Mo ta:** Quan ly thien than: mo khoa, len cap, tien hoa, ky nang, dung hinh.

### 36.1 `unlockAngel(userId, angelId)` va `levelUpAngel` va `gradeUpAngel`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ANG-001 | [P] | Mo khoa thien than | du nguyen lieu | Angel duoc tao voi mac dinh |
| TC-ANG-002 | [N] | Da mo khoa roi | da ton tai | Nem AngelAlreadyUnlockedException |
| TC-ANG-003 | [N] | Khong du nguyen lieu | material < yeu cau | Nem InsufficientMaterialsException |
| TC-ANG-004 | [P] | Len cap thien than | du exp/material | angel.level tang, power tang |
| TC-ANG-005 | [N] | Max level | level = MAX | Nem MaxLevelException |
| TC-ANG-006 | [P] | Tang cap thien than | du grade material | angel.grade tang |
| TC-ANG-007 | [N] | Max grade | grade = MAX_GRADE | Nem MaxGradeException |

### 36.2 `evolveAngel` va `upgradeSkill`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ANG-010 | [P] | Tien hoa thien than | canEvolve=true | Angel evolution tang, bonus stats mo khoa |
| TC-ANG-011 | [N] | canEvolve false | dieu kien chua du | Nem CannotEvolveException |
| TC-ANG-012 | [P] | Nang cap ky nang | skillSlot hop le, du material | angelSkill[skillSlot].level tang |
| TC-ANG-013 | [N] | skillSlot sai | skillSlot < 0 hoac > max | Nem InvalidSkillSlotException |

### 36.3 `equipAngel` va `renameAngel`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ANG-020 | [P] | Trang bi thien than | angelIndex hop le | Angel duoc equipped=true, angel cu bi unequip |
| TC-ANG-021 | [P] | Thao thien than | dang co equipped | Angel.equipped=false |
| TC-ANG-022 | [P] | Doi ten thien than | newName hop le, du gold | Angel.name duoc cap nhat, gold bi tru |
| TC-ANG-023 | [N] | Ten qua dai | newName > maxLength | Nem InvalidNameException |

---

## 37. TerritoryService

**File:** `territory-service/.../service/TerritoryServiceImpl.java`
**Mo ta:** Quan ly lanh tho: len cap, toa nha, thu tai nguyen, defense/attack.

### 37.1 `createTerritory` va `levelUpTerritory`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-TRT-001 | [P] | Tao lanh tho lan dau | userId chua co territory | Territory duoc tao voi level=1 |
| TC-TRT-002 | [N] | Da co lanh tho | userId da co territory | Nem TerritoryAlreadyExistsException |
| TC-TRT-003 | [P] | Len cap lanh tho | canLevelUp=true, du nguyen lieu | territory.level tang, thu nhap tang |
| TC-TRT-004 | [N] | Khong du dieu kien len cap | canLevelUp=false | Nem CannotLevelUpException |

### 37.2 `constructBuilding` va `upgradeBuilding` va `finishConstruction`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-TRT-010 | [P] | Xay toa nha moi | slotId trong, buildingId hop le | TerritoryBuilding tao voi status=CONSTRUCTING |
| TC-TRT-011 | [N] | Slot da co toa nha | slotId da co building | Nem SlotOccupiedException |
| TC-TRT-012 | [P] | Nang cap toa nha | canUpgradeBuilding=true | building.level tang, status=UPGRADING |
| TC-TRT-013 | [P] | Hoan thanh xay dung | remainingTime <= 0 | building.status=COMPLETED |
| TC-TRT-014 | [P] | Hoan thanh ngay lap tuc | du gem | building.status=COMPLETED, gem bi tru |

### 37.3 `collectResources` va `getTotalDefense`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-TRT-020 | [P] | Thu hoach tai nguyen | co tai nguyen tich luy | Wallet/Bag duoc cap nhat, lastCollectTime reset |
| TC-TRT-021 | [P] | Thu hoach khi khong co gi | tai nguyen = 0 | Khong thay doi, khong loi |
| TC-TRT-022 | [P] | Tinh tong defense | co nhieu buildings | Tra ve tong diem phong thu chinh xac |
| TC-TRT-023 | [P] | Tinh prosperity | co cac building | Tra ve tong prosperity dung |

---

## 38. TrialService

**File:** `trial-service/.../service/TrialServiceImpl.java`
**Mo ta:** Quan ly thu thach: bat dau, hoan thanh, nhan thuong, reset.

### 38.1 `startTrial(userId, trialId)` va `completeTrial`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-TRL-001 | [P] | Bat dau thu thach | canStartTrial=true | TrialRecord.status=IN_PROGRESS, attemptCount tang |
| TC-TRL-002 | [N] | Het luot thu | hasAttemptsRemaining=false | Nem NoAttemptsRemainingException |
| TC-TRL-003 | [N] | Thu thach dang trong trang thai IN_PROGRESS | status=IN_PROGRESS | Nem TrialAlreadyInProgressException |
| TC-TRL-004 | [P] | Hoan thanh thu thach voi 3 sao | stars=3, score=10000 | TrialRecord cap nhat, reward duoc cap, leaderboard duoc cap nhat |
| TC-TRL-005 | [P] | Hoan thanh voi diem cao moi | score > bestScore | bestScore duoc cap nhat |
| TC-TRL-006 | [P] | Hoan thanh nhung diem thap hon | score < bestScore | bestScore giu nguyen |

### 38.2 `claimReward` va `resetDailyAttempts`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-TRL-010 | [P] | Nhan thuong giai doan | stageId hop le, chua nhan | Reward duoc cap, stageId trong claimedRewards |
| TC-TRL-011 | [N] | Phan thuong da nhan | isRewardClaimed=true | Tra ve false hoac nem exception |
| TC-TRL-012 | [P] | Reset luot hang ngay | lastResetDate < today | attemptCount reset ve 0 |

---

## 39. EscortService

**File:** `escort-service/.../service/EscortServiceImpl.java`
**Mo ta:** Quan ly ho tong: tao nhiem vu, bat dau, cap nhat tien do, hoan thanh, nhan thuong.

### 39.1 `generateMission` va `startMission`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ESC-001 | [P] | Tao nhiem vu moi | quality=NORMAL | EscortMission duoc tao voi status=AVAILABLE |
| TC-ESC-002 | [P] | Tao nhiem vu chat luong cao | quality=GOLD | Reward cao hon, difficulty cao hon |
| TC-ESC-003 | [P] | Bat dau nhiem vu | missionId trong danh sach AVAILABLE | mission.status=IN_PROGRESS, startTime duoc set |
| TC-ESC-004 | [N] | Da co mission dang thuc hien | hasActiveMission=true | Nem ActiveMissionExistsException |
| TC-ESC-005 | [N] | Mission khong ton tai | missionId=99999 | Nem MissionNotFoundException |

### 39.2 `completeMission` va `claimReward`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ESC-010 | [P] | Hoan thanh nhiem vu | progress = 100% | mission.status=COMPLETED, completedAt duoc set |
| TC-ESC-011 | [N] | Tien do chua dat 100% | progress < 100 | Nem MissionNotCompleteException |
| TC-ESC-012 | [P] | Nhan thuong nhiem vu | status=COMPLETED | Reward duoc cap vao wallet/bag, status=CLAIMED |
| TC-ESC-013 | [N] | Da nhan thuong | status=CLAIMED | Nem RewardAlreadyClaimedException |

### 39.3 `refreshMissions` va `getStats`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-ESC-020 | [P] | Lam moi danh sach nhiem vu | canRefresh=true | Danh sach nhiem vu moi duoc tao |
| TC-ESC-021 | [N] | Khong du luot lam moi | canRefresh=false | Nem CannotRefreshException |
| TC-ESC-022 | [P] | Lay thong ke ho tong | userId hop le | Tra ve EscortStats day du |

---

## 40. StarMapService

**File:** `starmap-service/.../service/StarMapServiceImpl.java`
**Mo ta:** Quan ly ban do sao: sao, chom sao, nang cap, tinh power.

### 40.1 `activateStar(userId, starId)` va `levelUpStar`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-STM-001 | [P] | Kich hoat sao | starId hop le, du dieu kien | Star.active=true |
| TC-STM-002 | [N] | Sao da active | star.active=true | Nem StarAlreadyActiveException |
| TC-STM-003 | [P] | Len cap sao | du energy | star.level tang, power tang |
| TC-STM-004 | [N] | Sao chua active ma len cap | star.active=false | Nem StarNotActiveException |

### 40.2 `unlockConstellation` va `levelUpConstellation`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-STM-010 | [P] | Mo khoa chom sao | tat ca stars trong chom da active | Constellation.unlocked=true, bonus stats |
| TC-STM-011 | [N] | Chom sao chua du sao active | co sao chua active | Nem ConstellationRequirementsException |
| TC-STM-012 | [P] | Len cap chom sao | du material | constellation.level tang |
| TC-STM-013 | [N] | Chom sao chua mo khoa | unlocked=false | Nem ConstellationNotUnlockedException |

### 40.3 `calculateTotalStarMapPower(userId)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-STM-020 | [P] | Tinh tong power | co nhieu sao va chom sao active | Tra ve tong power chinh xac |
| TC-STM-021 | [P] | Khong co sao nao active | userId moi | Tra ve 0 |

---

## 41. ConfigService

**File:** `config-service/.../service/ConfigService.java`
**Mo ta:** Quan ly cau hinh he thong: get, list, evict cache.

### 41.1 `get(path)`, `exists(path)`, `list(prefix)`, `evict(path)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CFG-001 | [P] | Lay config ton tai | path="game.expTable" | Tra ve ConfigEnvelope chua data |
| TC-CFG-002 | [P] | Lay config khong ton tai | path="game.unknown" | Tra ve Optional.empty() |
| TC-CFG-003 | [I] | Cache hit – goi 2 lan | cung path | Lan 2 lay tu cache, khong query DB |
| TC-CFG-004 | [P] | Kiem tra path ton tai | path hop le | Tra ve true |
| TC-CFG-005 | [P] | Kiem tra path khong ton tai | path="ghost" | Tra ve false |
| TC-CFG-006 | [P] | List configs theo prefix | prefix="game." | Tra ve tat ca path bat dau bang "game." |
| TC-CFG-007 | [P] | Evict config khoi cache | path dang duoc cache | Cache entry bi xoa; lan tiep theo query DB |

---

## 42. FileService

**File:** `file-service/.../service/FileService.java`
**Mo ta:** Upload va download file.

### 42.1 `uploadFile(file)` va `downloadFile(fileName)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-FIL-001 | [P] | Upload file anh | MultipartFile PNG hop le | Tra ve URL cua file da upload |
| TC-FIL-002 | [P] | Upload file lon | file <= MAX_SIZE | Upload thanh cong |
| TC-FIL-003 | [N] | Upload file qua lon | file > MAX_SIZE | Nem FileSizeLimitException |
| TC-FIL-004 | [N] | File null | file=null | Nem NullPointerException hoac ValidationException |
| TC-FIL-005 | [P] | Download file ton tai | fileName hop le | Tra ve byte[] chinh xac |
| TC-FIL-006 | [N] | Download file khong ton tai | fileName="ghost.png" | Nem FileNotFoundException |

---

## 43. LocalizationService

**File:** `localization-service/.../service/LocalizationService.java`
**Mo ta:** Quan ly ban dich da ngon ngu.

### 43.1 `translate(key, language)` va `getAll(language)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-LOC-001 | [P] | Dich key sang tieng Viet | key="item.sword", language="vi" | Tra ve "Kiem" |
| TC-LOC-002 | [P] | Dich key sang tieng Anh | key="item.sword", language="en" | Tra ve "Sword" |
| TC-LOC-003 | [I] | Cache hit | goi translate 2 lan cung key/lang | Lan 2 lay tu cache |
| TC-LOC-004 | [N] | Key khong ton tai | key="item.ghost" | Tra ve key goc hoac null |
| TC-LOC-005 | [N] | Language khong ho tro | language="klingon" | Tra ve ban dich mac dinh (en) hoac null |
| TC-LOC-006 | [P] | Lay tat ca ban dich 1 ngon ngu | language="vi" | Tra ve Map day du |
| TC-LOC-007 | [P] | Ngon ngu khong co ban dich nao | language="zz" | Tra ve Map rong |

---

## 44. CombatService

**File:** `battleserver-service/.../service/CombatService.java`
**Mo ta:** Tinh toan ket qua chien dau: 1v1, batch, validate stats, rewards.

### 44.1 `calculateCombat(CombatRequest)`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CMB-001 | [P] | Chien dau attacker manh hon | attackerStats >> defenderStats | attacker thang, CombatResult.winner=attacker |
| TC-CMB-002 | [P] | Chien dau can bang | stats xap xi bang nhau | Ket qua ngau nhien nhung co xu huong |
| TC-CMB-003 | [N] | Stats khong hop le | hp <= 0 | Nem InvalidStatsException |
| TC-CMB-004 | [P] | Tinh toan chi tiet chien dau | request hop le | Tra ve CombatResult voi log chi tiet |

### 44.2 `calculateBatchCombat` va `calculateRewards`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-CMB-010 | [P] | Batch 10 tran chien | List 10 CombatRequest | Tra ve List 10 CombatResult |
| TC-CMB-011 | [P] | Batch rong | List rong | Tra ve List rong |
| TC-CMB-012 | [P] | Tinh phan thuong nguoi thang level cao | winner level > loser level | Reward it hon (de khong farm) |
| TC-CMB-013 | [P] | Tinh phan thuong nguoi thang level thap | winner level < loser level | Reward nhieu hon (upset bonus) |

---

## 45. ServerInfoService

**File:** `serverInfo-service/.../service/ServerInfoService.java`
**Mo ta:** Quan ly thong tin server, thoi gian hop server.

### 45.1 `getServerInfo()` va `updateServerInfo(dto)` va `getServerCombineTime()`

| ID | Loai | Mo ta | Dau vao | Ket qua mong doi |
|----|------|-------|---------|-----------------|
| TC-SRV-001 | [P] | Lay thong tin server | - | Tra ve ServerInfoDto day du |
| TC-SRV-002 | [I] | Cache hit | goi 2 lan | Lan 2 lay tu cache |
| TC-SRV-003 | [P] | Cap nhat thong tin server | dto hop le | Thong tin duoc luu, cache duoc evict |
| TC-SRV-004 | [P] | Lay thoi gian hop server | - | Tra ve epoch timestamp hop le |
| TC-SRV-005 | [P] | Thong tin server chua duoc cau hinh | DB rong | Tra ve null hoac ServerInfoDto mac dinh |

---

## Tong Ket Test Cases

### Nhom Core

| Service | So TC |
|---------|-------|
| UserService | 15 |
| AuthService | 7 |
| SessionService | 18 |
| WalletService | 16 |
| BagDomainService | 15 |
| RoleService | 16 |
| TaskDomainService | 15 |
| EquipService | 12 |
| ShopService | 11 |
| MailService | 17 |
| GuildService | 28 |
| ArenaService | 22 |

### Nhom Character & Progression

| Service | So TC |
|---------|-------|
| PetService | 15 |
| ItemService | 10 |
| MountService | 13 |
| ArtifactService | 13 |
| RuneService | 16 |
| BoxService | 11 |

### Nhom Economy & Content

| Service | So TC |
|---------|-------|
| CraftingService | 10 |
| PityService | 7 |
| GiftService | 5 |

### Nhom Social

| Service | So TC |
|---------|-------|
| FriendService | 13 |
| ChatService | 11 |
| LeaderboardService | 6 |
| RankingService | 6 |

### Nhom World

| Service | So TC |
|---------|-------|
| WorldService | 13 |
| SceneManagementService | 12 |

### Nhom Admin & Safety

| Service | So TC |
|---------|-------|
| AnalyticsService | 8 |
| AntiCheatService | 8 |
| ModerationService | 10 |
| ReportEventService | 7 |
| NotificationService | 8 |
| IapVerifyService | 9 |
| GMService | 12 |

### Nhom Advanced Systems

| Service | So TC |
|---------|-------|
| ShizhuangService | 8 |
| AngelService | 13 |
| TerritoryService | 13 |
| TrialService | 9 |
| EscortService | 11 |
| StarMapService | 9 |

### Nhom Infrastructure

| Service | So TC |
|---------|-------|
| ConfigService | 7 |
| FileService | 6 |
| LocalizationService | 7 |
| CombatService | 8 |
| ServerInfoService | 5 |

---

### Tong Cong

| Nhom | So Service | So TC |
|------|-----------|-------|
| Core | 12 | 192 |
| Character & Progression | 6 | 78 |
| Economy & Content | 3 | 22 |
| Social | 4 | 36 |
| World | 2 | 25 |
| Admin & Safety | 7 | 62 |
| Advanced Systems | 6 | 63 |
| Infrastructure | 5 | 33 |
| **Tong** | **45** | **511** |

---

## Chiến Lược Test

### Công Nghệ Sử Dụng
- **JUnit 5** – Framework test chính
- **Mockito** – Mock dependencies (Feign clients, Repositories)
- **AssertJ** – Fluent assertion API
- **Spring Boot Test** – Integration test với `@SpringBootTest`
- **H2 In-Memory DB** – Integration test với DB



### Nguyên Tắc Test
1. **Mỗi method service ít nhất 1 positive test + 1 negative test**
2. **Boundary test cho tất cả giá trị biên**
3. **Idempotency test cho các operation có idemKey**
4. **Mock tất cả external dependencies** (Feign, Redis, Wallet/Bag cross-service calls)
5. **Verify interaction**: kiểm tra service A có gọi service B đúng khi cần
6. **Transaction test**: kiểm tra rollback khi exception xảy ra giữa chừng
