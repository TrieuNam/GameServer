package com.SouthMillion.webSocket_server.handler.shizhuang;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.EquipFumoFeign;
import com.SouthMillion.webSocket_server.service.client.ShiZhuangFeign;
import com.SouthMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * ShiZhuang (Fashion/Equipment) Handler - P1a Priority
 * Handles equipment, fumo (enchantment), and fashion operations
 * 
 * MsgIds: 2135 (request), 1509 (list response), 1510 (single response)
 * Operations: equip, unequip, wear fashion, fumo operations
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShiZhuangHandler implements MessageHandler {

    private final ShiZhuangFeign shiZhuangFeign;
    private final EquipFumoFeign equipFumoFeign;
    private final RoleServiceHandler roleServiceHandler;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    // Operation types (1-based as per spec)
    private static final int OP_GET_EQUIPS = 1;       // Get all equipped items
    private static final int OP_EQUIP_ITEM = 2;       // Equip an item
    private static final int OP_UNEQUIP_ITEM = 3;     // Unequip an item
    private static final int OP_WEAR_FASHION = 4;     // Wear fashion
    private static final int OP_GET_FUMO = 5;         // Get fumo list
    private static final int OP_FUMO_ADD_EXP = 6;     // Add fumo experience
    private static final int OP_FUMO_ACTIVATE = 7;    // Activate fumo
    private static final int OP_FUMO_RESET = 8;       // Reset fumo

    @Override
    public int[] interests() {
        return new int[]{2135}; // PB_CSShiZhuangReq
    }

    /** Gọi sau login: đẩy danh sách thời trang (1509) và fumo (1509) về client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        // Login bootstrap should only push fashion list; fumo list uses same packet id and can overwrite fashion data.
        return Mono.fromRunnable(() -> handleGetEquipsList(session, roleId));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                // Parse proto request (assuming fields: op_type, param1, param2)
                // Note: Using msgshizhuang proto structure with req_type mapped to op_type
                var req = org.SouthMillion.proto.Msgshizhuang.Msgshizhuang.PB_CSShiZhuangReq.parseFrom(payload);
                int opType = req.getReqType();
                int param1 = req.getShizhuangId();  // Maps to param1 (itemId, fashionId, equipType, etc.)
                int param2 = req.getSlot();          // Maps to param2 (exp, slots, etc.)

                Long roleId = session.getRoleId();
                if (roleId == null) {
                    log.warn("[ShiZhuang] Missing roleId");
                    return;
                }

                log.info("[ShiZhuang] op={}, param1={}, param2={}, roleId={}", opType, param1, param2, roleId);

                switch (opType) {
                    case OP_GET_EQUIPS -> handleGetEquipsList(session, roleId);
                    case OP_EQUIP_ITEM -> handleEquipItem(session, roleId, param1);
                    case OP_UNEQUIP_ITEM -> handleUnequipItem(session, roleId, param1);
                    case OP_WEAR_FASHION -> handleWearFashion(session, roleId, param1);
                    case OP_GET_FUMO -> handleGetFumoList(session, roleId);
                    case OP_FUMO_ADD_EXP -> handleFumoAddExp(session, roleId, param1, param2);
                    case OP_FUMO_ACTIVATE -> handleFumoActivate(session, roleId, param1);
                    case OP_FUMO_RESET -> handleFumoReset(session, roleId, param1);
                    default -> log.warn("[ShiZhuang] Unknown op_type={}", opType);
                }
            } catch (Exception e) {
                log.error("[ShiZhuang] Error handling request: {}", e.getMessage(), e);
            }
        });
    }

    // ==================== Equipment Operations ====================

    private void handleGetEquipsList(PlayerSession session, Long roleId) {
        try {
            List<Map<String, Object>> fashions = FeignCall.withToken(
                    session.getSessionId(),
                    "shizhuang.list",
                    () -> shiZhuangFeign.getRoleFashions(String.valueOf(roleId))
            ).block();

            var builder = Msgknapsack.PB_SCAllShiZhuangInfo.newBuilder();
            if (fashions != null) {
                for (Map<String, Object> fashion : fashions) {
                    // Use clothesId (config id like 10001,20001…) NOT the database row id
                    int clothesId = getInt(fashion, "clothesId", 0);
                    int level     = getInt(fashion, "level", 0);
                    if (clothesId <= 0) continue; // skip invalid entries
                    builder.addShizhuangList(
                        Msgknapsack.PB_ShiZhuangData.newBuilder()
                            .setId(clothesId)
                            .setLevel(level)
                            .build()
                    );
                }
            }

            byte[] payload = builder.build().toByteArray();
            Emitters.emit(session, MsgIds.SC_ALL_SHIZHUANG_INFO, payload);
            log.info("[ShiZhuang] Sent equips list, count={}", fashions != null ? fashions.size() : 0);
        } catch (Exception e) {
            log.error("[ShiZhuang] Error getting equips list: {}", e.getMessage(), e);
            // Send empty list — do NOT call sendErrorResponse (that sends msg 1510 with id=0 which crashes the client)
            Emitters.emit(session, MsgIds.SC_ALL_SHIZHUANG_INFO,
                    Msgknapsack.PB_SCAllShiZhuangInfo.newBuilder().build().toByteArray());
        }
    }

    private void handleEquipItem(PlayerSession session, Long roleId, int clothesId) {
        try {
            // "Equip" in this context = wear the fashion item
            FeignCall.withToken(
                    session.getSessionId(),
                    "shizhuang.wear",
                    () -> { shiZhuangFeign.wearFashion(String.valueOf(roleId), clothesId); return null; }
            ).block();

            sendSingleShiZhuangResponse(session, clothesId, 1);
            handleGetEquipsList(session, roleId);
            roleServiceHandler.pushRoleState(session).subscribe();
            publishTaskProgress(roleId, taskActionConditionMapping.shizhuangEquipTaskKey(), "websocket-shizhuang-equip");
            log.info("[ShiZhuang] Equipped/wore clothes {}", clothesId);
        } catch (Exception e) {
            log.error("[ShiZhuang] Error equipping item {} (player may not own this item): {}", clothesId, e.getMessage());
            // Send current list to refresh client state
            handleGetEquipsList(session, roleId);
        }
    }

    private void handleUnequipItem(PlayerSession session, Long roleId, int clothesId) {
        try {
            FeignCall.withToken(
                    session.getSessionId(),
                    "shizhuang.unwear",
                    () -> { shiZhuangFeign.unwearFashion(String.valueOf(roleId), clothesId); return null; }
            ).block();

            sendSingleShiZhuangResponse(session, clothesId, 0);
            handleGetEquipsList(session, roleId);
            roleServiceHandler.pushRoleState(session).subscribe();
            log.info("[ShiZhuang] Unequipped item {}", clothesId);
        } catch (Exception e) {
            log.error("[ShiZhuang] Error unequipping item {}: {}", clothesId, e.getMessage(), e);
        }
    }

    private void handleWearFashion(PlayerSession session, Long roleId, int fashionId) {
        try {
            FeignCall.withToken(
                    session.getSessionId(),
                    "shizhuang.wear",
                    () -> { shiZhuangFeign.wearFashion(String.valueOf(roleId), fashionId); return null; }
            ).block();

            sendSingleShiZhuangResponse(session, fashionId, 1);
            handleGetEquipsList(session, roleId);
            roleServiceHandler.pushRoleState(session).subscribe();
            publishTaskProgress(roleId, taskActionConditionMapping.shizhuangEquipTaskKey(), "websocket-shizhuang-wear");
            log.info("[ShiZhuang] Wore fashion {}", fashionId);
        } catch (Exception e) {
            log.error("[ShiZhuang] Error wearing fashion {} (player may not own this item): {}", fashionId, e.getMessage());
            // Send current list to refresh client state
            handleGetEquipsList(session, roleId);
        }
    }

    // ==================== Fumo (Enchantment) Operations ====================

    private void handleGetFumoList(PlayerSession session, Long roleId) {
        try {
            EquipFumoDTOs.FumoListResp fumoList = FeignCall.withToken(
                    session.getSessionId(),
                    "fumo.list",
                    () -> equipFumoFeign.list(String.valueOf(roleId))
            ).block();

            var builder = Msgknapsack.PB_SCAllShiZhuangInfo.newBuilder();
            if (fumoList != null && fumoList.fumoList() != null) {
                int index = 1;
                for (EquipFumoDTOs.FumoData fumo : fumoList.fumoList()) {
                    builder.addShizhuangList(
                        Msgknapsack.PB_ShiZhuangData.newBuilder()
                            .setId(index++)
                            .setLevel(fumo.level())
                            .build()
                    );
                }
            }

            byte[] payload = builder.build().toByteArray();
            Emitters.emit(session, MsgIds.SC_ALL_SHIZHUANG_INFO, payload);
            log.info("[ShiZhuang] Sent fumo list");
        } catch (Exception e) {
            log.error("[ShiZhuang] Error getting fumo list: {}", e.getMessage(), e);
            // Send empty list on error — do NOT send 1510 with id=0
            Emitters.emit(session, MsgIds.SC_ALL_SHIZHUANG_INFO,
                    Msgknapsack.PB_SCAllShiZhuangInfo.newBuilder().build().toByteArray());
        }
    }

    private void handleFumoAddExp(PlayerSession session, Long roleId, int equipType, int addExp) {
        try {
            var req = new EquipFumoDTOs.AddExpReq(String.valueOf(roleId), equipType, addExp, null);
            
            EquipFumoDTOs.FumoOneResp result = FeignCall.withToken(
                    session.getSessionId(),
                    "fumo.addExp",
                    () -> equipFumoFeign.addExp(req)
            ).block();

            sendFumoResponse(session, result);
            if (result != null) {
                publishTaskProgress(roleId, taskActionConditionMapping.shizhuangFumoTaskKey(), "websocket-shizhuang-fumo-add-exp");
            }
            log.info("[ShiZhuang] Added {} exp to fumo equipType={}", addExp, equipType);
        } catch (Exception e) {
            log.error("[ShiZhuang] Error adding fumo exp: {}", e.getMessage(), e);
        }
    }

    private void handleFumoActivate(PlayerSession session, Long roleId, int equipType) {
        try {
            long endTime = System.currentTimeMillis() / 1000 + 86400; // 24 hours from now
            var req = new EquipFumoDTOs.ActivateReq(String.valueOf(roleId), equipType, endTime);
            
            EquipFumoDTOs.FumoOneResp result = FeignCall.withToken(
                    session.getSessionId(),
                    "fumo.activate",
                    () -> equipFumoFeign.activate(req)
            ).block();

            sendFumoResponse(session, result);
            if (result != null) {
                publishTaskProgress(roleId, taskActionConditionMapping.shizhuangFumoTaskKey(), "websocket-shizhuang-fumo-activate");
            }
            log.info("[ShiZhuang] Activated fumo equipType={}", equipType);
        } catch (Exception e) {
            log.error("[ShiZhuang] Error activating fumo: {}", e.getMessage(), e);
        }
    }

    private void handleFumoReset(PlayerSession session, Long roleId, int equipType) {
        try {
            var req = new EquipFumoDTOs.ResetReq(String.valueOf(roleId), equipType, null);
            
            EquipFumoDTOs.OkResp result = FeignCall.withToken(
                    session.getSessionId(),
                    "fumo.reset",
                    () -> equipFumoFeign.reset(req)
            ).block();

            // equipType here is a fumo-slot index, not a clothesId → send via 1510 with that slot
            sendSingleShiZhuangResponse(session, equipType, 0);
            if (result != null && result.ok()) {
                publishTaskProgress(roleId, taskActionConditionMapping.shizhuangFumoTaskKey(), "websocket-shizhuang-fumo-reset");
            }
            log.info("[ShiZhuang] Reset fumo equipType={}, ok={}", equipType, result != null && result.ok());
        } catch (Exception e) {
            log.error("[ShiZhuang] Error resetting fumo: {}", e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Send PB_SCShiZhuangInfo (msg 1510) for a single fashion update.
     * clothesId must be the config-side id (e.g. 10001, 20001…) NOT the DB row id.
     */
    private void sendSingleShiZhuangResponse(PlayerSession session, int clothesId, int level) {
        if (clothesId <= 0) {
            log.warn("[ShiZhuang] Refusing to send shizhuang response with invalid clothesId={}", clothesId);
            return;
        }
        var data = Msgknapsack.PB_ShiZhuangData.newBuilder()
                .setId(clothesId)
                .setLevel(level)
                .build();

        var response = Msgknapsack.PB_SCShiZhuangInfo.newBuilder()
                .setShizhuang(data)
                .build();

        Emitters.emit(session, MsgIds.SC_SHIZHUANG_INFO, response.toByteArray());
    }

    private void sendFumoResponse(PlayerSession session, EquipFumoDTOs.FumoOneResp result) {
        if (result != null && result.fumoData() != null) {
            var data = Msgknapsack.PB_ShiZhuangData.newBuilder()
                    .setId(result.equipType())
                    .setLevel(result.fumoData().level())
                    .build();
            
            var response = Msgknapsack.PB_SCShiZhuangInfo.newBuilder()
                    .setShizhuang(data)
                    .build();
            
            Emitters.emit(session, MsgIds.SC_SHIZHUANG_INFO, response.toByteArray());
        }
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            try { return Integer.parseInt((String) value); } catch (Exception ignored) {}
        }
        return defaultValue;
    }

    private void publishTaskProgress(Long roleId, String taskKey, String source) {
        if (taskKey == null || taskKey.isBlank()) {
            return;
        }
        taskProgressPublisher.publish(roleId, taskKey, 1, source);
    }
}
