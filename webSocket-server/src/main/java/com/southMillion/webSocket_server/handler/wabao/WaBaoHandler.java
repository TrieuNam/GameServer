package com.SouthMillion.webSocket_server.handler.wabao;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskCondition;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.BoxFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.box.BoxDTOs;
import org.SouthMillion.proto.Msgwabao.Msgwabao;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Handles treasure hunt / box gacha (挖宝) operations.
 *
 * Proto:
 *   1640 PB_CSWaBaoReq    — op_type + param1 + param2  (main operations)
 *   1641 PB_CSWaBaoSetReq — auto-open settings
 *
 * Responses:
 *   1642 PB_SCWaBaoInfo       — basic info ack (most ops)
 *   1644 PB_SCWaBaoItemInfo   — opened item result
 *   1649 PB_SCWaBaoSetingInfo — auto-open settings
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaBaoHandler implements MessageHandler {

    private final BoxFeign boxFeign;
    private final TaskProgressPublisher taskProgressPublisher;

    private static final int OP_GET_INFO       = 1;
    private static final int OP_OPEN           = 2;
    private static final int OP_WEAR           = 3;
    private static final int OP_SELL           = 4;
    private static final int OP_BUY            = 5;
    private static final int OP_LEVEL_UP       = 6;
    private static final int OP_QUICKEN        = 7;
    private static final int OP_LEVEL_REWARD   = 8;
    private static final int OP_LUCK_INFO      = 9;
    private static final int OP_LUCK_RECEIVE   = 10;
    private static final int OP_DECOMPOSE      = 11;
    private static final int OP_GET_SETTING    = 12;
    private static final int OP_GET_EQUIP_INFO = 13;

    @Override
    public int[] interests() {
        return new int[]{1640, 1641};
    }


    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                if (msgId == 1641) {
                    handleSetReq(session, payload);
                } else {
                    Msgwabao.PB_CSWaBaoReq req = Msgwabao.PB_CSWaBaoReq.parseFrom(payload);
                    int opType = req.hasOpType() ? req.getOpType() : 0;
                    int param1 = req.hasParam1()  ? req.getParam1()  : 0;
                    Long roleId = session.getRoleId();

                    log.debug("[WaBao] op={}, p1={}, roleId={}", opType, param1, roleId);

                    switch (opType) {
                        case OP_GET_INFO       -> handleGetInfo(session, roleId);
                        case OP_OPEN           -> handleOpen(session, roleId, param1);
                        case OP_WEAR           -> handleWear(session, roleId);
                        case OP_SELL           -> handleSell(session, roleId);
                        case OP_BUY            -> handleBuy(session, roleId);
                        case OP_LEVEL_UP       -> handleLevelUp(session, roleId);
                        case OP_QUICKEN        -> handleQuicken(session, roleId, param1);
                        case OP_LEVEL_REWARD   -> handleLevelReward(session, roleId, param1);
                        case OP_LUCK_INFO      -> handleLuckInfo(session, roleId);
                        case OP_LUCK_RECEIVE   -> handleLuckReceive(session, roleId, param1);
                        case OP_DECOMPOSE      -> handleDecompose(session, roleId);
                        case OP_GET_SETTING    -> handleGetSetting(session, roleId);
                        case OP_GET_EQUIP_INFO -> handleGetEquipInfo(session, roleId);
                        default -> {
                            log.warn("[WaBao] Unknown op={}", opType);
                            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("[WaBao] Error for roleId={}", session.getRoleId(), e);
                sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
            }
        });
    }

    // 1641: save auto-sweep stop conditions, then ack with current setting
    private void handleSetReq(PlayerSession session, byte[] payload) {
        try {
            Msgwabao.PB_CSWaBaoSetReq req = Msgwabao.PB_CSWaBaoSetReq.parseFrom(payload);
            if (req.hasWabaoSet()) {
                Msgwabao.PB_WaBaoSet set = req.getWabaoSet();
                BoxDTOs.BoxSettingResp settingData = new BoxDTOs.BoxSettingResp();
                settingData.setEquipEqality(set.hasEqality()     ? set.getEqality()     : 0);
                settingData.setOpenFiveMark(set.hasEqalityMark() ? set.getEqalityMark() : 0);
                settingData.setRetainMark(  set.hasNewRecord()   ? set.getNewRecord()   : 0);
                BoxDTOs.BoxSettingReq saveReq = new BoxDTOs.BoxSettingReq();
                saveReq.setRoleId(String.valueOf(session.getRoleId()));
                saveReq.setBoxSet(settingData);
                try { boxFeign.saveSetting(saveReq); }
                catch (Exception ex) {
                    log.warn("[WaBao] saveSetting failed (continuing): {}", ex.getMessage());
                }
            }
            BoxDTOs.BoxSettingResp setting = boxFeign.getSetting(session.getRoleId());
            sendSettingInfo(session, buildSettingInfo(setting));
        } catch (Exception e) {
            log.error("[WaBao] handleSetReq error", e);
            sendSettingInfo(session, Msgwabao.PB_SCWaBaoSetingInfo.newBuilder().build());
        }
    }

    // op=1: get info → 1642 PB_SCWaBaoInfo
    private void handleGetInfo(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.InfoResp info = boxFeign.info(roleId);
            Msgwabao.PB_SCWaBaoInfo.Builder b = Msgwabao.PB_SCWaBaoInfo.newBuilder();
            if (info != null) {
                b.setCollectionLevel(info.getBoxLevel());
                b.setCollectionBuyTimes(info.getBoxBuyTimes());
                if (info.getLevelUpEndEpoch() > 0)
                    b.setCollectionLevelUpTime((int) (info.getLevelUpEndEpoch() / 1000));
                b.setCollectionLevelFetchFlag(info.getLevelFetchFlag());
            }
            sendInfo(session, b.build());
        } catch (Exception e) {
            log.error("[WaBao] handleGetInfo error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=2: open box → 1644 PB_SCWaBaoItemInfo
    private void handleOpen(PlayerSession session, Long roleId, int count) {
        try {
            BoxDTOs.OpenReq req = new BoxDTOs.OpenReq();
            req.setRoleId(String.valueOf(roleId));
            req.setCount(Math.max(1, Math.min(count > 0 ? count : 1, 5)));
            req.setRoleLevel(1);
            BoxDTOs.OpenResp resp = boxFeign.open(req);
            reportTaskProgress(roleId, TaskCondition.OPEN_BOX.taskKey(), req.getCount());

            Msgwabao.PB_SCWaBaoItemInfo itemInfo;
            if (resp != null && resp.getOpenEquip() != null) {
                itemInfo = buildItemInfoFromEquip(resp.getOpenEquip());
            } else if (resp != null && resp.getCompareState() != null) {
                BoxDTOs.EquipInfo equipInfo = BoxDTOs.equipInfoFromCompareState(resp.getCompareState(), 1);
                itemInfo = (equipInfo != null && equipInfo.getEquipInfo() != null)
                        ? buildItemInfoFromEquip(equipInfo.getEquipInfo())
                        : Msgwabao.PB_SCWaBaoItemInfo.newBuilder().setResult(1).build();
            } else {
                itemInfo = Msgwabao.PB_SCWaBaoItemInfo.newBuilder().setResult(resp != null ? 1 : 0).build();
            }

            sendItemInfo(session, itemInfo);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleOpen error", e);
            sendItemInfo(session, Msgwabao.PB_SCWaBaoItemInfo.newBuilder().setResult(0).build());
        }
    }

    private void reportTaskProgress(Long roleId, String taskKey, int delta) {
        taskProgressPublisher.publish(roleId, taskKey, delta, "websocket-wabao");
    }

    // op=3: wear item
    private void handleWear(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.WearReq req = new BoxDTOs.WearReq();
            req.setRoleId(String.valueOf(roleId));
            BoxDTOs.OkResp wearResp = boxFeign.wear(req);

            boolean hasPendingItem = false;
            try {
                BoxDTOs.BoxCompareStateResp compareState = boxFeign.getCompareState(roleId);
                BoxDTOs.EquipInfo equipInfo = BoxDTOs.equipInfoFromCompareState(compareState, 0);
                if (equipInfo != null && equipInfo.getEquipInfo() != null) {
                    sendItemInfo(session, buildItemInfoFromEquip(equipInfo.getEquipInfo()));
                    hasPendingItem = true;
                }
            } catch (Exception ex) {
                log.debug("[WaBao] handleWear compare-state fetch skipped: {}", ex.toString());
            }

            if (wearResp != null && wearResp.isOk() && !hasPendingItem) {
                sendClearItemInfo(session);
            }
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleWear error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=4: sell item
    private void handleSell(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.SellReq req = new BoxDTOs.SellReq();
            req.setRoleId(String.valueOf(roleId));
            boxFeign.sell(req);
            sendClearItemInfo(session);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleSell error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=5: buy
    private void handleBuy(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.SimpleReq req = new BoxDTOs.SimpleReq();
            req.setRoleId(String.valueOf(roleId));
            boxFeign.buy(req);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleBuy error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=6: level up
    private void handleLevelUp(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.SimpleReq req = new BoxDTOs.SimpleReq();
            req.setRoleId(String.valueOf(roleId));
            boxFeign.levelUp(req);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleLevelUp error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=7: quicken (param1 = times)
    private void handleQuicken(PlayerSession session, Long roleId, int num) {
        try {
            BoxDTOs.QuickenReq req = new BoxDTOs.QuickenReq();
            req.setRoleId(String.valueOf(roleId));
            req.setNum(Math.max(1, num));
            boxFeign.quicken(req);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleQuicken error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=8: level reward (param1 = idx)
    private void handleLevelReward(PlayerSession session, Long roleId, int idx) {
        try {
            BoxDTOs.LevelRewardReq req = new BoxDTOs.LevelRewardReq();
            req.setRoleId(String.valueOf(roleId));
            req.setIdx(Math.max(1, idx));
            boxFeign.levelReward(req);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleLevelReward error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=9: luck unpack info
    private void handleLuckInfo(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.LuckInfoResp luck = boxFeign.luckInfo(roleId);
            Msgwabao.PB_SCWaBaoInfo.Builder b = Msgwabao.PB_SCWaBaoInfo.newBuilder();
            if (luck != null) b.setCollectionLevel(luck.getBoxLevel());
            sendInfo(session, b.build());
        } catch (Exception e) {
            log.error("[WaBao] handleLuckInfo error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=10: receive luck reward (param1 = seq)
    private void handleLuckReceive(PlayerSession session, Long roleId, int seq) {
        try {
            BoxDTOs.LuckReceiveReq req = new BoxDTOs.LuckReceiveReq();
            req.setRoleId(String.valueOf(roleId));
            req.setSeq(Math.max(0, seq));
            boxFeign.luckReceive(req);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleLuckReceive error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=11: decompose
    private void handleDecompose(PlayerSession session, Long roleId) {
        try {
            boxFeign.decompose(roleId);
            sendClearItemInfo(session);
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleDecompose error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    // op=12: get setting → 1649 PB_SCWaBaoSetingInfo
    private void handleGetSetting(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.BoxSettingResp setting = boxFeign.getSetting(roleId);
            sendSettingInfo(session, buildSettingInfo(setting));
        } catch (Exception e) {
            log.error("[WaBao] handleGetSetting error", e);
            sendSettingInfo(session, Msgwabao.PB_SCWaBaoSetingInfo.newBuilder().build());
        }
    }

    // op=13: get equip info → info ack
    private void handleGetEquipInfo(PlayerSession session, Long roleId) {
        try {
            BoxDTOs.EquipInfo equipInfo = boxFeign.equipInfo(roleId);
            if (equipInfo != null && equipInfo.getEquipInfo() != null) {
                sendItemInfo(session, buildItemInfoFromEquip(equipInfo.getEquipInfo()));
            } else {
                sendClearItemInfo(session);
            }
            handleGetInfo(session, roleId);
        } catch (Exception e) {
            log.error("[WaBao] handleGetEquipInfo error", e);
            sendInfo(session, Msgwabao.PB_SCWaBaoInfo.newBuilder().build());
        }
    }

    private Msgwabao.PB_SCWaBaoSetingInfo buildSettingInfo(BoxDTOs.BoxSettingResp s) {
        Msgwabao.PB_SCWaBaoSetingInfo.Builder b = Msgwabao.PB_SCWaBaoSetingInfo.newBuilder();
        if (s != null) {
            b.setBoxSet(Msgwabao.PB_WaBaoSet.newBuilder()
                    .setEqality(s.getEquipEqality())
                    .setEqalityMark(s.getOpenFiveMark())
                    .setNewRecord(s.getRetainMark())
                    .setNewBook(0)
                    .build());
        }
        return b.build();
    }

    /** SC 1642 PB_SCWaBaoInfo */
    private void sendInfo(PlayerSession session, Msgwabao.PB_SCWaBaoInfo info) {
        try { Emitters.emit(session, 1642, info.toByteArray()); }
        catch (Exception e) { log.error("[WaBao] sendInfo failed", e); }
    }

    /** SC 1643 PB_SCWaBaoMapInfo — real data from box-service */
    private void sendMapInfo(PlayerSession session) {
        try {
            BoxDTOs.WaBaoMapInfo data = boxFeign.getWaBaoMapInfo(session.getRoleId());
            Msgwabao.PB_SCWaBaoMapInfo.Builder b = Msgwabao.PB_SCWaBaoMapInfo.newBuilder();
            if (data != null) {
                b.setCurMap(data.getCurMap()).setUnlockedMap(data.getUnlockedMap());
                if (data.getMapConditionNum() != null)
                    data.getMapConditionNum().forEach(b::addMapConditionNum);
            }
            Emitters.emit(session, 1643, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[WaBao] sendMapInfo failed", e);
            try { Emitters.emit(session, 1643, Msgwabao.PB_SCWaBaoMapInfo.newBuilder().build().toByteArray()); } catch (Exception ignored) {}
        }
    }

    private Msgwabao.PB_SCWaBaoItemInfo buildItemInfoFromEquip(BoxDTOs.EquipRolled rolled) {
        Msgwabao.PB_SCWaBaoItemInfo.Builder b = Msgwabao.PB_SCWaBaoItemInfo.newBuilder();
        b.setResult(1);
        if (rolled != null) {
            Msgwabao.PB_WaBaoItemData.Builder item = Msgwabao.PB_WaBaoItemData.newBuilder();
            if (rolled.getItemId() != null) item.setItemId(rolled.getItemId());
            // Protocol has no quality/equipLevel fields here; keep integrity as 0.
            item.setIntegrity(0);
            if (rolled.getAttrType1() != null) item.setAttrType1(rolled.getAttrType1());
            if (rolled.getAttrValue1() != null) item.setAttrValue1(rolled.getAttrValue1());
            if (rolled.getAttrType2() != null) item.setAttrType2(rolled.getAttrType2());
            if (rolled.getAttrValue2() != null) item.setAttrValue2(rolled.getAttrValue2());
            b.setItemData(item.build());
        }
        return b.build();
    }

    private void sendItemInfo(PlayerSession session, Msgwabao.PB_SCWaBaoItemInfo info) {
        try {
            Emitters.emit(session, 1644, info.toByteArray());
        } catch (Exception e) {
            log.error("[WaBao] sendItemInfo failed", e);
        }
    }

    private void sendClearItemInfo(PlayerSession session) {
        Msgwabao.PB_SCWaBaoItemInfo clear = Msgwabao.PB_SCWaBaoItemInfo.newBuilder()
                .setResult(1)
                .setItemData(Msgwabao.PB_WaBaoItemData.newBuilder().setItemId(0).setIntegrity(0).build())
                .build();
        sendItemInfo(session, clear);
    }

    /** SC 1645 PB_SCWaBaoIntegrityInfo — real data from box-service */
    private void sendIntegrityInfo(PlayerSession session) {
        try {
            BoxDTOs.WaBaoIntegrityInfo data = boxFeign.getWaBaoIntegrity(session.getRoleId());
            Msgwabao.PB_SCWaBaoIntegrityInfo.Builder b = Msgwabao.PB_SCWaBaoIntegrityInfo.newBuilder();
            if (data != null) {
                b.setIsLogin(data.getIsLogin());
                // data.getDataList() — stub empty for now
            }
            Emitters.emit(session, 1645, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[WaBao] sendIntegrityInfo failed", e);
            try { Emitters.emit(session, 1645, Msgwabao.PB_SCWaBaoIntegrityInfo.newBuilder().build().toByteArray()); } catch (Exception ignored) {}
        }
    }

    /** SC 1646 PB_SCWaBaoCollectionListInfo — real data from box-service */
    private void sendCollectionList(PlayerSession session) {
        try {
            BoxDTOs.WaBaoCollectionInfo data = boxFeign.getWaBaoCollection(session.getRoleId());
            Msgwabao.PB_SCWaBaoCollectionListInfo.Builder b =
                    Msgwabao.PB_SCWaBaoCollectionListInfo.newBuilder();
            if (data != null) b.setIsLogin(data.getIsLogin());
            Emitters.emit(session, 1646, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[WaBao] sendCollectionList failed", e);
            try { Emitters.emit(session, 1646, Msgwabao.PB_SCWaBaoCollectionListInfo.newBuilder().build().toByteArray()); } catch (Exception ignored) {}
        }
    }

    /** SC 1647 PB_SCWaBaoToolInfo — real data from box-service */
    private void sendToolInfo(PlayerSession session) {
        try {
            boxFeign.getWaBaoToolInfo(session.getRoleId()); // fetch (stub empty list)
            Emitters.emit(session, 1647, Msgwabao.PB_SCWaBaoToolInfo.newBuilder().build().toByteArray());
        } catch (Exception e) {
            log.error("[WaBao] sendToolInfo failed", e);
            try { Emitters.emit(session, 1647, Msgwabao.PB_SCWaBaoToolInfo.newBuilder().build().toByteArray()); } catch (Exception ignored) {}
        }
    }

    /** SC 1648 PB_SCWaBaoTaskInfo — repeated int32 task_list (proto field) */
    private void sendTaskInfo(PlayerSession session) {
        try {
            BoxDTOs.WaBaoTaskInfo data = boxFeign.getWaBaoTaskInfo(session.getRoleId());
            Msgwabao.PB_SCWaBaoTaskInfo.Builder b = Msgwabao.PB_SCWaBaoTaskInfo.newBuilder();
            if (data != null) {
                if (data.getTaskFlag() != null) b.setTaskFlag(data.getTaskFlag());
                if (data.getTaskList() != null)
                    data.getTaskList().forEach(b::addTaskList);
                if (data.getTaskTypeNumList() != null)
                    data.getTaskTypeNumList().forEach(b::addTaskTypeNum);
            }
            Emitters.emit(session, 1648, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[WaBao] sendTaskInfo failed", e);
            try { Emitters.emit(session, 1648, Msgwabao.PB_SCWaBaoTaskInfo.newBuilder().build().toByteArray()); } catch (Exception ignored) {}
        }
    }

    /** SC 1649 PB_SCWaBaoSetingInfo */
    private void sendSettingInfo(PlayerSession session, Msgwabao.PB_SCWaBaoSetingInfo info) {
        try { Emitters.emit(session, 1649, info.toByteArray()); }
        catch (Exception e) { log.error("[WaBao] sendSettingInfo failed", e); }
    }

    /** SC 1650 PB_SCWaBaoCollectionBookInfo (empty stub — book data N/A) */
    private void sendCollectionBookInfo(PlayerSession session) {
        try { Emitters.emit(session, 1650, Msgwabao.PB_SCWaBaoCollectionBookInfo.newBuilder().build().toByteArray()); }
        catch (Exception e) { log.error("[WaBao] sendCollectionBookInfo failed", e); }
    }

    /** SC 1651 PB_SCWaBaoBookListInfo — repeated int32 activate_flag (proto field) */
    private void sendBookListInfo(PlayerSession session) {
        try {
            BoxDTOs.WaBaoBookListInfo data = boxFeign.getWaBaoBookListInfo(session.getRoleId());
            Msgwabao.PB_SCWaBaoBookListInfo.Builder b = Msgwabao.PB_SCWaBaoBookListInfo.newBuilder();
            if (data != null && data.getActivateFlagList() != null) {
                data.getActivateFlagList().forEach(b::addActivateFlag);
            }
            Emitters.emit(session, 1651, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[WaBao] sendBookListInfo failed", e);
            try { Emitters.emit(session, 1651, Msgwabao.PB_SCWaBaoBookListInfo.newBuilder().build().toByteArray()); } catch (Exception ignored) {}
        }
    }

    /**
     * pushAll — gửi đủ 10 SC types sau login (theo yêu cầu client MsgIdManger.ts)
     * SC: 1642(skip-login) 1643 1644(skip) 1645 1646 1647 1648 1649 1650 1651
     * 1642 bị bỏ qua ở đây vì boxHandler.pushAll đã push 1616 với cùng data (boxLevel, buyTimes…).
     * Client sẽ nhận 1642 ngay sau thao tác đầu tiên (open/wear/sell/…) qua handleGetInfo trong mỗi op.
     */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            sendMapInfo(session);               // 1643
            sendIntegrityInfo(session);         // 1645
            sendCollectionList(session);        // 1646
            sendToolInfo(session);              // 1647
            sendTaskInfo(session);              // 1648
            handleGetSetting(session, roleId);  // 1649
            sendCollectionBookInfo(session);    // 1650
            sendBookListInfo(session);          // 1651
        });
    }
}
