package com.SouthMillion.webSocket_server.handler.pet;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskCondition;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.PetFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msgpet.Msgpet;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PetHandler implements MessageHandler {

    private final PetFeign petFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    @Override
    public int[] interests() {
        return new int[]{2110, 2105}; // PB_CSRolePetReq + PB_CSPetOneKeyUpLevelGemReq
    }

    /** Gọi sau login: chỉ đẩy pet snapshot khi DB thực sự có pet/module data. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> pushLoginPetSnapshot(session, roleId));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Long roleId = session.getRoleId();
                if (roleId == null) {
                    sendErrorResponse(session);
                    return;
                }

                if (msgId == 2105) {
                    handleOneKeyUpLevelGem(session, roleId, payload);
                    return;
                }

                Msgpet.PB_CSRolePetReq req = Msgpet.PB_CSRolePetReq.parseFrom(payload);
                int operation = req.hasReqType() ? req.getReqType() : 1;
                log.debug("[Pet] Handler op={}, roleId={}", operation, roleId);

                switch (operation) {
                    case 1 -> handleGetPets(session, roleId);
                    case 2 -> handleActivatePet(session, roleId, req);
                    case 3 -> handleUpgradePet(session, roleId, req);
                    case 4 -> handleEvolvePet(session, roleId, req);
                    case 5 -> handleSetActivePet(session, roleId, req);
                    default -> {
                        log.warn("[Pet] Unknown operation: {}", operation);
                        sendErrorResponse(session);
                    }
                }
            } catch (Exception e) {
                log.error("[Pet] Error handling request for roleId={}", session.getRoleId(), e);
                sendErrorResponse(session);
            }
        });
    }

    /** OP1: Lấy toàn bộ thú */
    private void handleGetPets(PlayerSession session, Long roleId) {
        try {
            Map<String, Object> result = petFeign.getRolePets(String.valueOf(roleId));
            sendPetListSnapshot(session, result);
        } catch (Exception e) {
            log.error("[Pet] Error in handleGetPets", e);
            sendErrorResponse(session);
        }
    }

    private void pushLoginPetSnapshot(PlayerSession session, Long roleId) {
        try {
            Map<String, Object> result = petFeign.getRolePets(String.valueOf(roleId));
            if (!hasAnyPetData(result)) {
                log.debug("[Pet] skip pushAll because no pet rows for roleId={}", roleId);
                return;
            }
            sendPetListSnapshot(session, result);
        } catch (Exception e) {
            log.error("[Pet] Error in pushLoginPetSnapshot roleId={}", roleId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void sendPetListSnapshot(PlayerSession session, Map<String, Object> result) {
        Msgpet.PB_SCRolePetAllInfo.Builder response = Msgpet.PB_SCRolePetAllInfo.newBuilder();
        Object petList = result != null ? result.get("pets") : null;
        if (!(petList instanceof List<?>)) {
            petList = result != null ? result.get("petList") : null;
        }
        if (petList instanceof List<?> pets) {
            for (Object pet : pets) {
                if (pet instanceof Map<?, ?> petMap) {
                    response.addPetList(buildPetData((Map<String, Object>) petMap));
                }
            }
        }
        sendResponse(session, response.build());
    }

    /**
     * OP2: Mở khoá / thêm thú mới.
     * param_1 = petTemplateId
     * PetController mới: POST /api/pet/{roleId}/add  (body: {petTemplateId})
     */
    private void handleActivatePet(PlayerSession session, Long roleId, Msgpet.PB_CSRolePetReq req) {
        try {
            int petTemplateId = req.hasParam1() ? req.getParam1() : 0;
            Map<String, Object> body = Map.of("petTemplateId", petTemplateId);
            Map<String, Object> result = petFeign.activatePet(String.valueOf(roleId), body);
            publishTaskProgress(roleId, result, taskActionConditionMapping.petActivateTaskKey(), "websocket-pet-activate");
            sendPetResponse(session, result);
        } catch (Exception e) {
            log.error("[Pet] Error in handleActivatePet", e);
            sendErrorResponse(session);
        }
    }

    /**
     * OP3: Nâng cấp cấp độ thú.
     * param_1 = petId, param_list = materialIds[]
     * PetController mới: POST /api/pet/{roleId}/levelup  (body: {petId, materialIds})
     */
    private void handleUpgradePet(PlayerSession session, Long roleId, Msgpet.PB_CSRolePetReq req) {
        try {
            long petId = req.hasParam1() ? req.getParam1() : 0L;
            List<Long> materialIds = req.getParamListList().stream()
                    .map(Integer::longValue).collect(Collectors.toList());
            Map<String, Object> body = new HashMap<>();
            body.put("petId", petId);
            body.put("materialIds", materialIds);
            Map<String, Object> result = petFeign.upgradePet(String.valueOf(roleId), body);
            publishTaskProgress(roleId, result, taskActionConditionMapping.petUpgradeTaskKey(), "websocket-pet-upgrade");
            publishDirectConditionProgress(roleId, result, TaskCondition.PET_LEVEL_UP.taskKey(), "websocket-pet-levelup");
            sendPetResponse(session, result);
        } catch (Exception e) {
            log.error("[Pet] Error in handleUpgradePet", e);
            sendErrorResponse(session);
        }
    }

    /**
     * OP4: Tiến hoá thú (tăng sao).
     * param_1 = petId
     * PetController mới: POST /api/pet/{roleId}/evolve  (body: {petId})
     */
    private void handleEvolvePet(PlayerSession session, Long roleId, Msgpet.PB_CSRolePetReq req) {
        try {
            long petId = req.hasParam1() ? req.getParam1() : 0L;
            Map<String, Object> body = Map.of("petId", petId);
            Map<String, Object> result = petFeign.evolvePet(String.valueOf(roleId), body);
            publishTaskProgress(roleId, result, taskActionConditionMapping.petEvolveTaskKey(), "websocket-pet-evolve");
            sendPetResponse(session, result);
        } catch (Exception e) {
            log.error("[Pet] Error in handleEvolvePet", e);
            sendErrorResponse(session);
        }
    }

    /**
     * OP5: Đặt thú chiến đấu (set active / fight).
     * param_1 = petId
     * PetController mới: POST /api/pet/{roleId}/fight  (body: {petId})
     */
    private void handleSetActivePet(PlayerSession session, Long roleId, Msgpet.PB_CSRolePetReq req) {
        try {
            long petId = req.hasParam1() ? req.getParam1() : 0L;
            Map<String, Object> body = Map.of("petId", petId);
            Map<String, Object> result = petFeign.setActivePet(String.valueOf(roleId), body);
            sendPetResponse(session, result);
        } catch (Exception e) {
            log.error("[Pet] Error in handleSetActivePet", e);
            sendErrorResponse(session);
        }
    }

    // ---- helpers ----

    @SuppressWarnings("unchecked")
    private void sendPetResponse(PlayerSession session, Map<String, Object> result) {
        Msgpet.PB_SCRolePetAllInfo.Builder response = Msgpet.PB_SCRolePetAllInfo.newBuilder();
        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
            Map<String, Object> petData = (Map<String, Object>) result.get("petData");
            if (petData != null) response.addPetList(buildPetData(petData));
        }
        sendResponse(session, response.build());
    }

    private Msgpet.PB_SCRolePetData buildPetData(Map<String, Object> pet) {
        return Msgpet.PB_SCRolePetData.newBuilder()
                .setPetId(readInt(pet, "petId", null, 0))
                .setPetIndex(readInt(pet, "petIndex", null, 0))
                .setPetLevel(readInt(pet, "level", "petLevel", 1))
                .setPetExp(readInt(pet, "petExp", null, 0))
                .setPetOrder(readInt(pet, "petStar", "petOrder", 1))
                .build();
    }

    private int readInt(Map<String, Object> pet, String key, String fallbackKey, int defaultValue) {
        Object value = pet.get(key);
        if (!(value instanceof Number) && fallbackKey != null) {
            value = pet.get(fallbackKey);
        }
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private boolean hasAnyPetData(Map<String, Object> result) {
        if (result == null) {
            return false;
        }
        Object hasData = result.get("hasData");
        if (hasData instanceof Boolean b) {
            return b;
        }
        Object pets = result.get("pets");
        if (!(pets instanceof List<?>)) {
            pets = result.get("petList");
        }
        return pets instanceof List<?> list && !list.isEmpty();
    }

    private void sendResponse(PlayerSession session, Msgpet.PB_SCRolePetAllInfo response) {
        try {
            Emitters.emit(session, 2101, response.toByteArray());
        } catch (Exception e) {
            log.error("[Pet] Failed to send response", e);
        }
    }

    private void sendErrorResponse(PlayerSession session) {
        sendResponse(session, Msgpet.PB_SCRolePetAllInfo.newBuilder().build());
    }

    private void handleOneKeyUpLevelGem(PlayerSession session, Long roleId, byte[] payload) {
        try {
            Msgpet.PB_CSPetOneKeyUpLevelGemReq req = Msgpet.PB_CSPetOneKeyUpLevelGemReq.parseFrom(payload);
            int successCount = 0;
            for (Msgpet.PB_OneKeyPetGemInfo item : req.getItemsList()) {
                try {
                    if (item.getIsTsGem()) {
                        int gemIndex = item.hasTsGemIndex() ? item.getTsGemIndex() : 0;
                        if (gemIndex > 0) {
                            petFeign.oneKeyTSGemLevelUp(String.valueOf(roleId), gemIndex);
                            successCount++;
                        }
                    } else {
                        int petIndex = item.hasPetIndex() ? item.getPetIndex() : 0;
                        int slotIndex = item.hasSlotIndex() ? item.getSlotIndex() : 0;
                        if (petIndex > 0) {
                            petFeign.oneKeyGemLevelUp(String.valueOf(roleId), petIndex, Math.max(0, slotIndex));
                            successCount++;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("[Pet] one-key gem item failed roleId={}, item={}, error={}", roleId, item, ex.getMessage());
                }
            }
            if (successCount > 0) {
                taskProgressPublisher.publish(roleId, TaskCondition.PET_GEM_UPGRADE.taskKey(), successCount, "websocket-pet-gem-upgrade");
            }
            handleGetPets(session, roleId);
        } catch (Exception e) {
            log.error("[Pet] Error in handleOneKeyUpLevelGem", e);
            sendErrorResponse(session);
        }
    }

    private void publishTaskProgress(Long roleId, Map<String, Object> result, String taskKey, String source) {
        if (roleId == null || taskKey == null || taskKey.isBlank()) {
            return;
        }
        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
            taskProgressPublisher.publish(roleId, taskKey, 1, source);
        }
    }

    private void publishDirectConditionProgress(Long roleId, Map<String, Object> result, String conditionKey, String source) {
        if (roleId == null || conditionKey == null || conditionKey.isBlank()) {
            return;
        }
        if (result != null && Boolean.TRUE.equals(result.get("success"))) {
            taskProgressPublisher.publish(roleId, conditionKey, 1, source);
        }
    }
}
