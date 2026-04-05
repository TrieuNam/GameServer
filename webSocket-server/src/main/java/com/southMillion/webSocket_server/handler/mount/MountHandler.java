package com.SouthMillion.webSocket_server.handler.mount;

import com.google.protobuf.InvalidProtocolBufferException;
import org.SouthMillion.proto.Msgmount.Msgmount;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.MountFeign;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Mount系统处理器 - P3优先级
 * 负责处理坐骑相关的所有操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MountHandler implements MessageHandler {

    private final MountFeign mountFeign;
    private final RoleServiceHandler roleServiceHandler;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // Mount操作类型常量
    private static final int OP_LEVEL_UP = 1;        // 升级
    private static final int OP_GRADE_UP = 2;        // 升阶
    private static final int OP_EXPLORE = 3;         // 探索
    private static final int OP_SET_APPEARANCE = 4;  // 设置外观
    private static final int OP_PIFU_UP = 5;         // 皮肤升级
    private static final int OP_SET_PIFU = 6;        // 设置皮肤
    private static final int OP_WEAR = 7;            // 穿戴
    private static final int OP_DECOMPOSE = 8;       // 分解
    private static final int OP_UNLOCK = 9;          // 解锁
    private static final int OP_ENTRY_REFRESH = 10;  // 刷新入口
    private static final int OP_BUY = 11;            // 购买
    private static final int OP_REFRESH_BUY = 12;    // 刷新购买
    private static final int OP_OPEN_BUY = 13;       // 打开购买

    @Override
    public int[] interests() {
        return new int[]{2140}; // PB_CSMountReq
    }

    /** Gọi sau login: chỉ đẩy mount info/harness list khi DB thực sự có dữ liệu sở hữu. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleIdStr = session.getRoleId();
        if (roleIdStr == null) return Mono.empty();
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = roleIdStr;
                java.util.Map<String, Object> mountData = mountFeign.getMountData(String.valueOf(roleId));
                if (!hasOwnedMountData(mountData)) {
                    log.debug("[Mount] skip pushAll because no mount/harness rows for roleId={}", roleId);
                    return;
                }
                sendMountInfo(session, mountData);
                sendMountList(session, mountData);
            } catch (NumberFormatException e) {
                log.warn("[Mount] pushAll: roleId không hợp lệ={}", roleIdStr);
            }
        });
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgmount.PB_CSMountReq req = Msgmount.PB_CSMountReq.parseFrom(payload);
            int operation = req.getReqType();
            Long roleId = session.getRoleId();

            log.info("Mount operation: roleId={}, operation={}, param={}", 
                    roleId, operation, req.getParam());

            boolean success = false;
            int resultParam1 = req.getParam();
            int resultParam2 = req.getParam2();

            java.util.Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("mountId", req.getParam());
            requestBody.put("param2", req.getParam2());

            java.util.Map<String, Object> result = null;

            switch (operation) {
                case OP_LEVEL_UP:
                    result = mountFeign.levelUp(String.valueOf(roleId), requestBody);
                    break;

                case OP_GRADE_UP:
                    result = mountFeign.gradeUp(String.valueOf(roleId), req.getParam());
                    break;

                case OP_EXPLORE:
                    result = mountFeign.explore(String.valueOf(roleId), requestBody);
                    break;

                case OP_SET_APPEARANCE:
                    result = mountFeign.setAppearance(String.valueOf(roleId), requestBody);
                    break;

                case OP_PIFU_UP:
                    result = mountFeign.upgradePifu(String.valueOf(roleId), requestBody);
                    break;

                case OP_SET_PIFU:
                    result = mountFeign.setPifu(String.valueOf(roleId), requestBody);
                    break;

                case OP_WEAR:
                    result = mountFeign.wearHarness(String.valueOf(roleId), requestBody);
                    break;

                case OP_DECOMPOSE:
                    result = mountFeign.decomposeHarness(String.valueOf(roleId), requestBody);
                    break;

                case OP_UNLOCK:
                    result = mountFeign.unlockHarnessSlot(String.valueOf(roleId), requestBody);
                    break;

                case OP_ENTRY_REFRESH:
                    result = mountFeign.refreshShop(String.valueOf(roleId), requestBody);
                    break;

                case OP_BUY:
                    result = mountFeign.buyFromShop(String.valueOf(roleId), requestBody);
                    break;

                case OP_REFRESH_BUY:
                    result = mountFeign.refreshAndBuy(String.valueOf(roleId), requestBody);
                    break;

                case OP_OPEN_BUY:
                    result = mountFeign.openShop(String.valueOf(roleId));
                    break;

                default:
                    log.warn("Unknown mount operation: {}", operation);
                    sendOperationResult(session, operation, false, resultParam1, resultParam2);
                    return;
            }

            success = result != null && Boolean.TRUE.equals(result.get("success"));
            sendOperationResult(session, operation, success, resultParam1, resultParam2);

            if (success) {
                publishTaskProgress(roleId, operation);
                // 操作成功后更新客户端Mount信息
                sendMountInfo(session, roleId);
                // 马具操作: 额外发送 2144 单件更新
                if (operation == OP_WEAR || operation == OP_DECOMPOSE || operation == OP_UNLOCK) {
                    sendHarnessOneInfo(session, result);
                }
                if (operation == OP_SET_APPEARANCE || operation == OP_SET_PIFU) {
                    roleServiceHandler.pushRoleState(session).subscribe();
                }
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Failed to parse mount request", e);
        } catch (Exception e) {
            log.error("Error handling mount operation", e);
        }
        });
    }

    /**
     * 发送操作结果
     */
    private void sendOperationResult(PlayerSession session, int opType, boolean success, int param1, int param2) {
        try {
            Msgmount.PB_SCMountOpRet.Builder builder = Msgmount.PB_SCMountOpRet.newBuilder();
            builder.setRetType(success ? 0 : 1);
            builder.setParam1(opType);
            builder.setParam2(param1);

            Emitters.emit(session, 2142, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("Failed to send mount operation result", e);
        }
    }

    /**
     * 发送Mount信息更新
     */
    private void sendMountInfo(PlayerSession session, Long roleId) {
        try {
            sendMountInfo(session, mountFeign.getMountData(String.valueOf(roleId)));
        } catch (Exception e) {
            log.error("Failed to send mount info", e);
        }
    }

    private void sendMountInfo(PlayerSession session, java.util.Map<String, Object> mountData) {
        if (mountData == null || Boolean.FALSE.equals(mountData.get("success"))) {
            return;
        }

        Msgmount.PB_SCMountInfo.Builder builder = Msgmount.PB_SCMountInfo.newBuilder();

        // Parse appearanceId
        Object appId = mountData.get("appearanceId");
        if (appId instanceof Number) builder.setAppearanceId(((Number) appId).intValue());

        // Parse mount_list
        Object mountList = mountData.get("mountList");
        if (mountList instanceof java.util.List<?> list) {
            for (Object item : list) {
                if (item instanceof java.util.Map<?, ?> m) {
                    Msgmount.PB_MountData.Builder md = Msgmount.PB_MountData.newBuilder();
                    Object lv = m.get("level");
                    if (lv instanceof Number) md.setLevel(((Number) lv).intValue());
                    Object gr = m.get("grade");
                    if (gr instanceof Number) md.setGrade(((Number) gr).intValue());
                    Object et = m.get("lastExploreTime");
                    if (et instanceof Number) md.setLastExploreTime(((Number) et).longValue());
                    builder.addMountList(md.build());
                }
            }
        }

        // Parse pifu_list
        Object pifuList = mountData.get("pifuList");
        if (pifuList instanceof java.util.List<?> pl) {
            for (Object p : pl) {
                if (p instanceof Number) builder.addPifuList(((Number) p).intValue());
            }
        }

        // Parse shop/timing fields
        Object freeTime = mountData.get("freeTime");
        if (freeTime instanceof Number) builder.setFreeTime(((Number) freeTime).intValue());
        Object r1 = mountData.get("refresh1Num");
        if (r1 instanceof Number) builder.setRefresh1Num(((Number) r1).intValue());
        Object r2 = mountData.get("refresh2Num");
        if (r2 instanceof Number) builder.setRefresh2Num(((Number) r2).intValue());
        Object bf = mountData.get("buyFlag");
        if (bf instanceof Number) builder.setBuyFlag(((Number) bf).intValue());
        Object bsl = mountData.get("buySeqList");
        if (bsl instanceof java.util.List<?> bslList) {
            for (Object b : bslList) {
                if (b instanceof Number) builder.addBuySeqList(((Number) b).intValue());
            }
        }

        Emitters.emit(session, 2141, builder.build().toByteArray());
    }

    /**
     * 发送Mount列表
     */
    public void sendMountList(PlayerSession session, Long roleId) {
        try {
            sendMountList(session, mountFeign.getMountData(String.valueOf(roleId)));
        } catch (Exception e) {
            log.error("Failed to send mount list", e);
        }
    }

    private void sendMountList(PlayerSession session, java.util.Map<String, Object> mountData) {
        if (mountData == null || Boolean.FALSE.equals(mountData.get("success"))) {
            return;
        }

        Msgmount.PB_SCMountHarnessListInfo.Builder builder = Msgmount.PB_SCMountHarnessListInfo.newBuilder();

        Object harnessList = mountData.get("harnessList");
        if (harnessList instanceof java.util.List<?> hl) {
            for (Object item : hl) {
                if (item instanceof java.util.Map<?, ?> hm) {
                    Msgmount.PB_HarnessData.Builder hd = Msgmount.PB_HarnessData.newBuilder();
                    Object idx = hm.get("index");
                    if (idx instanceof Number) hd.setIndex(((Number) idx).intValue());
                    Object iid = hm.get("itemId");
                    if (iid instanceof Number) hd.setItemId(((Number) iid).intValue());
                    Object wm = hm.get("wearingMark");
                    if (wm instanceof Number) hd.setWearingMark(((Number) wm).intValue());
                    Object an = hm.get("attrNum");
                    if (an instanceof Number) hd.setAttrNum(((Number) an).intValue());
                    Object lf = hm.get("lockFlag");
                    if (lf instanceof Number) hd.setLockFlag(((Number) lf).intValue());
                    builder.addHarnessList(hd.build());
                }
            }
        }

        Emitters.emit(session, 2143, builder.build().toByteArray());
    }

    private boolean hasOwnedMountData(java.util.Map<String, Object> mountData) {
        if (mountData == null || Boolean.FALSE.equals(mountData.get("success"))) {
            return false;
        }
        Object hasData = mountData.get("hasData");
        if (hasData instanceof Boolean b) {
            return b;
        }
        Object mountList = mountData.get("mountList");
        if (mountList instanceof java.util.List<?> list && !list.isEmpty()) {
            return true;
        }
        Object harnessList = mountData.get("harnessList");
        return harnessList instanceof java.util.List<?> list && !list.isEmpty();
    }

    /**
     * 发送单件马具更新 — SC:2144 PB_SCMountHarnessOneInfo
     * Được gọi sau OP_WEAR / OP_DECOMPOSE / OP_UNLOCK để client cập nhật đúng slot.
     */
    private void sendHarnessOneInfo(PlayerSession session, java.util.Map<String, Object> result) {
        if (result == null) return;
        try {
            Object harnessData = result.get("harnessData");
            if (!(harnessData instanceof java.util.Map<?, ?> hm)) return;

            Msgmount.PB_HarnessData.Builder hd = Msgmount.PB_HarnessData.newBuilder();
            Object idx = hm.get("index");      if (idx instanceof Number) hd.setIndex(((Number) idx).intValue());
            Object iid = hm.get("itemId");     if (iid instanceof Number) hd.setItemId(((Number) iid).intValue());
            Object wm  = hm.get("wearingMark");if (wm  instanceof Number) hd.setWearingMark(((Number) wm).intValue());
            Object an  = hm.get("attrNum");    if (an  instanceof Number) hd.setAttrNum(((Number) an).intValue());
            Object lf  = hm.get("lockFlag");   if (lf  instanceof Number) hd.setLockFlag(((Number) lf).intValue());

            Msgmount.PB_SCMountHarnessOneInfo out = Msgmount.PB_SCMountHarnessOneInfo.newBuilder()
                    .setHarnessData(hd)
                    .build();
            Emitters.emit(session, 2144, out.toByteArray());
        } catch (Exception e) {
            log.error("Failed to send harness one info", e);
        }
    }

    private void publishTaskProgress(Long roleId, int operation) {
        String taskKey;
        String source;
        switch (operation) {
            case OP_LEVEL_UP -> {
                taskKey = taskActionConditionMapping.mountLevelUpTaskKey();
                source = "websocket-mount-level-up";
            }
            case OP_GRADE_UP -> {
                taskKey = taskActionConditionMapping.mountGradeUpTaskKey();
                source = "websocket-mount-grade-up";
            }
            case OP_WEAR -> {
                taskKey = taskActionConditionMapping.mountWearTaskKey();
                source = "websocket-mount-wear";
            }
            case OP_DECOMPOSE -> {
                taskKey = taskActionConditionMapping.mountDecomposeTaskKey();
                source = "websocket-mount-decompose";
            }
            default -> {
                return;
            }
        }
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}

