# LevelFund Server Implementation Note

## Muc tieu

Tai lieu nay dung de ghi lai cach implement server flow cho `LevelFund` de sau nay co can lam lai, migrate, hoac doi branch thi co the phuc dung nhanh.

Flow dung cua `LevelFund` khong chi la luu `phaseBuyFlag/commonFetchFlag/seniorFetchFlag`, ma phai:

- doc config tu `dengjijijin.json`
- validate dieu kien mua phase
- validate dieu kien nhan reward
- grant item qua `bag-service`
- chi set fetch flag sau khi grant thanh cong

## File lien quan

- `GameServer/activity-service/src/main/java/com/SouthMillion/activity_service/service/ActivityService.java`
- `GameServer/activity-service/src/main/java/com/SouthMillion/activity_service/entity/LevelFund.java`
- `GameServer/activity-service/src/main/java/com/SouthMillion/activity_service/repository/LevelFundRepository.java`
- `GameServer/webSocket-server/src/main/java/com/SouthMillion/webSocket_server/handler/activity/RandActivityHandler.java`
- `GameServer/config-service/src/main/resources/config/gameworld/logicconfig/randactivity/dengjijijin.json`
- `document/client/LineR/assets/script/modules/levelfund/LevelFundData.ts`
- `document/client/LineR/assets/script/modules/levelfund/LevelFundView.ts`

## Contract client-server

- Client rand activity type: `2050`
- Websocket normalize: `2050 -> 11`
- SC proto: `3011 / PB_SCRaLevelFundInfo`

Request contract can nho:

- `opType 0`: get info
- `opType 1`: claim reward, `param1 = 0 common / 1 senior`, `param2 = seq`
- `opType 2`: buy phase, `param1 = phase`
- `opType 3`: legacy claim common, `param1 = seq`
- `opType 4`: legacy claim senior, `param1 = seq`

## Config can dung

Config file: `dengjijijin.json`

Cac truong chinh:

- `gift_configure[].seq`
- `gift_configure[].phase`
- `gift_configure[].level`
- `gift_configure[].ordinary_item`
- `gift_configure[].senior_item`
- `phase_configure[].phase`
- `phase_configure[].show_level`
- `phase_configure[].buy_money`

Rule parse:

- `ordinary_item` co the la object
- `senior_item` thuong la array
- nen tai su dung helper parse item de tranh viet rieng 2 format

## Cac buoc implement dung

### 1. Them config cache trong ActivityService

Them:

- `LEVEL_FUND_CONFIG_PATH = "config/gameworld/logicconfig/randactivity/dengjijijin.json"`
- `LevelFundConfig levelFundConfigCache`

Can co 2 record tuong tu BoxFund:

- `LevelFundGiftConfig(seq, phase, level, ordinaryItems, seniorItems)`
- `LevelFundConfig(giftsBySeq, phaseShowLevels)`

### 2. Viet loader cho LevelFund config

Can viet:

- `getLevelFundConfig()`
- `loadLevelFundConfig()`

Logic:

- goi `configFeign.getFile(...)`
- parse `gift_configure` vao `Map<Integer, LevelFundGiftConfig>`
- parse `phase_configure` vao `Map<Integer, Integer> phaseShowLevels`
- neu loi thi fallback `LevelFundConfig.empty()`

### 3. Tach helper state

Can viet:

- `getOrCreateLevelFund(roleId)`
- `levelFundSnapshot(fund)`

Muc dich:

- tranh lap code trong handler
- giu response shape dung voi `PB_SCRaLevelFundInfo`

### 4. Implement mua phase

Method nen co: `buyLevelFundPhase(roleId, fund, config, phase)`

Can validate:

- `phase > 0`
- phase phai ton tai trong config
- role level phai dat `show_level`
- neu da mua roi thi bo qua

Sau do moi:

- set bit trong `phaseBuyFlag`
- save entity

Bit rule hien dang dung:

- `int bit = 1 << phase`

## 5. Implement claim reward

Method nen co: `claimLevelFundReward(roleId, fund, config, rewardType, seq)`

Can validate:

- `seq` ton tai trong `giftsBySeq`
- role level phai dat `gift.level`

Neu `rewardType == 0`:

- check common bit chua claim
- grant `ordinaryItems`
- grant thanh cong moi set `commonFetchFlag`

Neu `rewardType == 1`:

- check phase premium da mua chua
- check senior bit chua claim
- grant `seniorItems`
- grant thanh cong moi set `seniorFetchFlag`

Bit rule hien dang dung:

- `long bit = 1L << seq`

## 6. Grant reward qua bag-service

Nen tai su dung pattern da co:

- tao `BagDTOs.GrantReq`
- set `roleId`
- set `items`
- set `reason`
- goi `bagFeign.grantItems(request)`

Luu y:

- neu `bagFeign == null` thi phai log va fail
- neu grant throw exception thi khong duoc set fetch flag

## 7. Handler chinh

`handleLevelFund(...)` nen chi lam 3 viec:

- lay entity
- lay config
- dispatch theo `opType`

Khong nen viet logic grant/validate truc tiep trong switch qua dai, vi sau nay se rat kho sua.

## Diem de sai

### 1. Nham voi BoxFund box level

`LevelFund` validate bang role level, khong phai box level.

### 2. Set flag truoc khi grant

Day la loi nghiem trong nhat. Neu set flag truoc ma grant fail thi client se thay da nhan reward nhung item khong vao bag.

### 3. Quen validate premium phase

`senior reward` phai bi chan neu phase chua mua.

### 4. Quen doc config `show_level`

Neu bo qua buoc nay, phase co the mua som hon client cho phep.

### 5. Tin vao docs cu

Nhieu docs co the ghi `LevelFund` da done chi vi proto/entity da ton tai. Van phai check lai business logic trong `ActivityService`.

## Payment callback

Phan trong note nay chu yeu ghi logic business trong `activity-service`.

Neu can lam end-to-end day du, can kiem tra them callback thanh toan de dam bao khi user mua goi `LevelFund`, server co goi duoc logic `BUY_PHASE` hoac cap nhat `phaseBuyFlag` o dung noi.

Neu callback khong noi vao `activity-service`, premium reward van bi chan dung du server claim logic da dung.

## Cach verify sau khi implement

### Build

Chay:

```powershell
mvn -q -f GameServer/activity-service/pom.xml -DskipTests compile
```

Mong doi:

- exit code `0`

### Functional checklist

1. Vao activity, `opType 0` tra ve du 3 flag.
2. Claim common khi chua du level phai bi chan.
3. Claim common khi du level phai vao bag va set common bit.
4. Claim senior khi chua mua phase phai bi chan.
5. Claim senior khi da mua phase phai vao bag va set senior bit.
6. Claim lai reward da nhan phai idempotent.

## Ghi chu cuoi

Neu can implement lai nhanh, dung `BoxFund` lam mau gan nhat. Khac biet chinh:

- BoxFund claim dua theo `boxLevel`
- LevelFund claim dua theo `roleLevel`
- Config file cua LevelFund la `dengjijijin.json`

Trong truong hop can rollback tam thoi, co the rollback ve snapshot-only handler, nhung khi do reward se khong duoc grant va flow se khong dung nghiep vu.