package com.SouthMillion.webSocket_server.handler.shenqi;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskActionConditionMapping;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.ArtifactFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.Msgshenqi.Msgshenqi;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShenQiHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShenQiHandler.class);

    private final ArtifactFeign artifactFeign;
    private final TaskProgressPublisher taskProgressPublisher;
    private final TaskActionConditionMapping taskActionConditionMapping;

    private static final int OP_GET_LIST      = 1;
    private static final int OP_ACTIVATE      = 2;
    private static final int OP_UPGRADE       = 3;
    private static final int OP_UPGRADE_SKILL = 4;
    private static final int OP_SET_ACTIVE    = 5;
    private static final int OP_EVOLVE        = 6;
    private static final int OP_REFINE        = 7;
    private static final int OP_DRAW          = 8;
    private static final int OP_GET_RECORDS   = 9;

    @Override
    public int[] interests() {
        return new int[]{1675}; // PB_CSShenQiReq
    }

    /** Goi sau login: day danh sach than khi (1676) ve client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> handleGetList(session, roleId));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgshenqi.PB_CSShenQiReq req = Msgshenqi.PB_CSShenQiReq.parseFrom(payload);
                int reqType = req.hasReqType() ? req.getReqType() : 0;
                int param1  = req.hasParam1()  ? req.getParam1()  : 0;
                int param2  = req.hasParam2()  ? req.getParam2()  : 0;
                Long roleId = session.getRoleId();
                if (roleId == null) {
                    log.warn("[ShenQi] Missing roleId, skip request");
                    return;
                }

                log.debug("[ShenQi] op={}, param1={}, param2={}, roleId={}", reqType, param1, param2, roleId);

                switch (reqType) {
                    case OP_GET_LIST      -> handleGetList(session, roleId);
                    case OP_ACTIVATE      -> handleActivate(session, roleId, param1);
                    case OP_UPGRADE       -> handleUpgrade(session, roleId, param1, param2);
                    case OP_UPGRADE_SKILL -> handleUpgradeSkill(session, roleId, param1, param2);
                    case OP_SET_ACTIVE    -> handleSetActive(session, roleId, param1);
                    case OP_EVOLVE        -> handleEvolve(session, roleId, param1, param2);
                    case OP_REFINE        -> handleRefine(session, roleId, param1);
                    case OP_DRAW          -> handleDraw(session, roleId, param1);
                    case OP_GET_RECORDS   -> handleGetRecords(session, roleId);
                    default -> log.warn("[ShenQi] Unknown op={}", reqType);
                }
            } catch (Exception e) {
                log.error("[ShenQi] Error for roleId={}", session.getRoleId(), e);
                sendErrorList(session);
            }
        });
    }

    // op=1: Get all artifacts → 1676 PB_SCShenQiListInfo
    private void handleGetList(PlayerSession session, Long roleId) {
        try {
            List<Map<String, Object>> artifacts = artifactFeign.getRoleArtifacts(roleId);
            Msgshenqi.PB_SCShenQiListInfo.Builder builder = Msgshenqi.PB_SCShenQiListInfo.newBuilder();
            if (artifacts != null) {
                for (Map<String, Object> artifact : artifacts) {
                    Msgshenqi.PB_ShenQiData.Builder data = Msgshenqi.PB_ShenQiData.newBuilder();
                    // Artifact entity serializes as camelCase (Lombok @Data)
                    int num = getInt(artifact, "artifactIndex", "artifact_index", "num", "id");
                    int level = getInt(artifact, "level");
                    data.setNum(num > 0 ? num : 1);
                    data.setLevel(level > 0 ? level : 1);
                    builder.addShenqiList(data);
                }
            }
            Emitters.emit(session, 1676, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[ShenQi] handleGetList error", e);
            sendErrorList(session);
        }
    }

    // op=2: Activate/unlock artifact → 1677 PB_SCShenQiOneInfo
    private void handleActivate(PlayerSession session, Long roleId, int artifactId) {
        try {
            Map<String, Object> result = artifactFeign.activateArtifact(roleId, artifactId);
            publishTaskProgress(roleId, result, taskActionConditionMapping.shenqiActivateTaskKey(), "websocket-shenqi-activate");
            sendOneInfo(session, artifactId, result);
        } catch (Exception e) {
            log.error("[ShenQi] handleActivate error", e);
            sendOneInfo(session, artifactId, null);
        }
    }

    // op=3: Upgrade artifact level → 1677 PB_SCShenQiOneInfo
    private void handleUpgrade(PlayerSession session, Long roleId, int artifactId, int targetLevel) {
        try {
            Map<String, Object> result = artifactFeign.upgradeArtifact(roleId, artifactId);
            publishTaskProgress(roleId, result, taskActionConditionMapping.shenqiUpgradeTaskKey(), "websocket-shenqi-upgrade");
            sendOneInfo(session, artifactId, result);
        } catch (Exception e) {
            log.error("[ShenQi] handleUpgrade error", e);
            sendOneInfo(session, artifactId, null);
        }
    }

    // op=4: Upgrade artifact skill
    private void handleUpgradeSkill(PlayerSession session, Long roleId, int artifactId, int skillIndex) {
        try {
            Map<String, Object> result = artifactFeign.upgradeSkill(roleId, artifactId, skillIndex);
            sendOneInfo(session, artifactId, result);
        } catch (Exception e) {
            log.error("[ShenQi] handleUpgradeSkill error", e);
            sendOneInfo(session, artifactId, null);
        }
    }

    // op=5: Set active (equip) artifact → 1678 PB_SCShenQiOtherInfo
    private void handleSetActive(PlayerSession session, Long roleId, int artifactId) {
        try {
            artifactFeign.setActiveArtifact(roleId, artifactId);
            // Controller returns void; send wearingId back
            Emitters.emit(session, 1678, Msgshenqi.PB_SCShenQiOtherInfo.newBuilder()
                            .setWearingId(artifactId)
                            .build().toByteArray());
        } catch (Exception e) {
            log.error("[ShenQi] handleSetActive error", e);
            Emitters.emit(session, 1678, Msgshenqi.PB_SCShenQiOtherInfo.newBuilder().build().toByteArray());
        }
    }

    // op=6: Evolve artifact (grade up) → 1677 PB_SCShenQiOneInfo
    private void handleEvolve(PlayerSession session, Long roleId, int artifactId, int evolutionPath) {
        try {
            Map<String, Object> result = artifactFeign.evolveArtifact(roleId, artifactId);
            publishTaskProgress(roleId, result, taskActionConditionMapping.shenqiEvolveTaskKey(), "websocket-shenqi-evolve");
            sendOneInfo(session, artifactId, result);
        } catch (Exception e) {
            log.error("[ShenQi] handleEvolve error", e);
            sendOneInfo(session, artifactId, null);
        }
    }

    // op=7: Refine artifact (reroll attributes) → 1677 PB_SCShenQiOneInfo
    private void handleRefine(PlayerSession session, Long roleId, int artifactId) {
        try {
            Map<String, Object> result = artifactFeign.refineArtifact(roleId, artifactId);
            publishTaskProgress(roleId, result, taskActionConditionMapping.shenqiRefineTaskKey(), "websocket-shenqi-refine");
            sendOneInfo(session, artifactId, result);
        } catch (Exception e) {
            log.error("[ShenQi] handleRefine error", e);
            sendOneInfo(session, artifactId, null);
        }
    }

    // op=8: Draw artifact (gacha) → 1679 PB_SCShenQiDrawInfo
    private void handleDraw(PlayerSession session, Long roleId, int drawType) {
        try {
            List<Map<String, Object>> drawResults = artifactFeign.drawArtifacts(roleId, drawType);
            
            Msgshenqi.PB_SCShenQiDrawInfo.Builder builder = Msgshenqi.PB_SCShenQiDrawInfo.newBuilder();
            if (drawResults != null) {
                for (Map<String, Object> result : drawResults) {
                    int artifactId = getInt(result, "artifactId", "artifact_id");
                    if (artifactId <= 0) continue;
                    builder.addCellList(artifactId);
                }
            }
            
            Emitters.emit(session, 1679, builder.build().toByteArray());
            if (drawResults != null && !drawResults.isEmpty()) {
                taskProgressPublisher.publish(roleId, taskActionConditionMapping.shenqiDrawTaskKey(), 1, "websocket-shenqi-draw");
            }
            log.info("[ShenQi] Draw success, roleId={}, drawType={}, count={}", roleId, drawType, drawResults != null ? drawResults.size() : 0);
        } catch (Exception e) {
            log.error("[ShenQi] Draw failed, roleId={}, drawType={}, error: {}", roleId, drawType, e.getMessage());
            Emitters.emit(session, 1679, Msgshenqi.PB_SCShenQiDrawInfo.newBuilder().build().toByteArray());
        }
    }

    // op=9: Get draw history → 1680 PB_SCShenQiRecordInfo
    private void handleGetRecords(PlayerSession session, Long roleId) {
        try {
            List<Map<String, Object>> records = artifactFeign.getDrawRecords(roleId);
            
            Msgshenqi.PB_SCShenQiRecordInfo.Builder builder = Msgshenqi.PB_SCShenQiRecordInfo.newBuilder();
            if (records != null) {
                for (Map<String, Object> record : records) {
                    Msgshenqi.PB_ShenQiRecordData.Builder recordBuilder = Msgshenqi.PB_ShenQiRecordData.newBuilder();

                    long drawTsSec = parseTimestampSeconds(record != null ? record.get("drawTimestamp") : null);
                    if (drawTsSec <= 0) drawTsSec = parseTimestampSeconds(record != null ? record.get("draw_timestamp") : null);
                    recordBuilder.setTime((int) Math.max(0L, Math.min(Integer.MAX_VALUE, drawTsSec)));

                    int artifactId = getInt(record, "artifactId", "artifact_id");
                    if (artifactId > 0) {
                        recordBuilder.addCellList(artifactId);
                    }

                    builder.addRecordList(recordBuilder.build());
                }
            }

            Emitters.emit(session, 1680, builder.build().toByteArray());
            log.info("[ShenQi] GetRecords success, roleId={}, count={}", roleId, records != null ? records.size() : 0);
        } catch (Exception e) {
            log.error("[ShenQi] GetRecords failed, roleId={}, error: {}", roleId, e.getMessage());
            Emitters.emit(session, 1680, Msgshenqi.PB_SCShenQiRecordInfo.newBuilder().build().toByteArray());
        }
    }

    private void sendOneInfo(PlayerSession session, int artifactId, Map<String, Object> result) {
        try {
            Msgshenqi.PB_SCShenQiOneInfo.Builder builder = Msgshenqi.PB_SCShenQiOneInfo.newBuilder();
            builder.setId(artifactId);
            if (result != null) {
                Msgshenqi.PB_ShenQiData.Builder data = Msgshenqi.PB_ShenQiData.newBuilder();
                // Artifact entity camelCase: artifactIndex → num, level
                data.setNum(getInt(result, "artifactIndex", "artifact_index", "num", "id"));
                data.setLevel(getInt(result, "level"));
                if (data.getNum() <= 0) data.setNum(artifactId);
                if (data.getLevel() <= 0) data.setLevel(1);
                builder.setShenqiData(data);
            }
            Emitters.emit(session, 1677, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[ShenQi] sendOneInfo error", e);
        }
    }

    private void sendErrorList(PlayerSession session) {
        try {
            Emitters.emit(session, 1676, Msgshenqi.PB_SCShenQiListInfo.newBuilder().build().toByteArray());
        } catch (Exception e) {
            log.error("[ShenQi] sendErrorList failed", e);
        }
    }

    private int getInt(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) return 0;
        for (String key : keys) {
            Object v = map.get(key);
            if (v instanceof Number n) return n.intValue();
            if (v != null) {
                try {
                    return Integer.parseInt(String.valueOf(v));
                } catch (Exception ignored) {}
            }
        }
        return 0;
    }

    private long parseTimestampSeconds(Object raw) {
        if (raw == null) return 0L;
        if (raw instanceof Number n) {
            long v = n.longValue();
            return (v > 3_000_000_000L) ? (v / 1000L) : v;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) return 0L;
        try {
            long v = Long.parseLong(s);
            return (v > 3_000_000_000L) ? (v / 1000L) : v;
        } catch (Exception ignored) {}
        try {
            LocalDateTime dt = LocalDateTime.parse(s);
            return dt.atZone(ZoneId.systemDefault()).toEpochSecond();
        } catch (Exception ignored) {}
        return 0L;
    }

    private void publishTaskProgress(Long roleId, Map<String, Object> result, String taskKey, String source) {
        if (roleId == null || taskKey == null || taskKey.isBlank()) {
            return;
        }
        if (isOperationSuccess(result)) {
            taskProgressPublisher.publish(roleId, taskKey, 1, source);
        }
    }

    private boolean isOperationSuccess(Map<String, Object> result) {
        if (result == null) {
            return false;
        }
        Object success = result.get("success");
        if (success == null) {
            return true;
        }
        return Boolean.TRUE.equals(success);
    }
}
