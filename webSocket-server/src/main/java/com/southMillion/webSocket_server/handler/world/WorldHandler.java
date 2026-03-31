package com.SouthMillion.webSocket_server.handler.world;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.PacketCodec;
import com.SouthMillion.webSocket_server.service.PlayerSessionRegistry;
import com.SouthMillion.webSocket_server.service.client.WorldFeign;
import com.SouthMillion.webSocket_server.service.grpc.GameWorldGrpcClient;
import com.SouthMillion.webSocket_server.utils.FeignTokenHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.grpc.gameworld.*;
import org.SouthMillion.proto.Msgworld.Msgworld;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;

/**
 * World Handler - Handles world/scene management operations
 * MIGRATED TO gRPC from Feign for 50-60% performance improvement
 * <p>
 * Message IDs: 2000-2099 (WORLD category)
 * Proto file: msgworld.proto
 * <p>
 * Operations:
 * - Enter/leave scenes (zones)
 * - Movement sync (&lt;25ms target)
 * - Object visibility (roles, NPCs, monsters, items)
 * - Pickup items
 * - Interact with NPCs
 * <p>
 * Key msgIds:
 * - Enter scene request/ack
 * - Leave scene request/ack
 * - Move request/ack
 * - Scene info push
 * - Object enter/leave view
 * - Pickup item request/ack
 * - Interact NPC request/ack
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorldHandler implements MessageHandler {

    private final GameWorldGrpcClient gameWorldGrpcClient;
    private final WorldFeign worldFeign;
    private final PlayerSessionRegistry sessionRegistry;

    /**
     * Virtual-thread scheduler cho Feign/gRPC blocking calls.
     * Dùng @Autowired trên field non-final để tránh xung đột với @RequiredArgsConstructor + @Qualifier.
     */
    @Autowired
    @Qualifier("feignVtScheduler")
    private Scheduler feignScheduler;

    // Message ID constants (based on msgworld.proto structure)
    private static final int MSGID_ENTER_SCENE_REQ = 2001;
    private static final int MSGID_ENTER_SCENE_ACK = 2002;
    private static final int MSGID_LEAVE_SCENE_REQ = 2003;
    private static final int MSGID_LEAVE_SCENE_ACK = 2004;
    private static final int MSGID_MOVE_REQ = 2010;
    private static final int MSGID_MOVE_ACK = 2011;
    private static final int MSGID_SCENE_INFO = 2020;
    // 2021-2022: role enter/leave view – reserved for future push broadcasts
    private static final int MSGID_OBJECT_MOVE = 2023;
    // 2024: object status change – reserved for future push broadcasts
    private static final int MSGID_PICKUP_ITEM_REQ = 2030;
    private static final int MSGID_PICKUP_ITEM_ACK = 2031;
    private static final int MSGID_INTERACT_NPC_REQ = 2040;
    private static final int MSGID_INTERACT_NPC_ACK = 2041;
    private static final float MOVE_BROADCAST_RADIUS = 80.0f;
    private static final int MOVE_BROADCAST_MAX_PLAYERS = 120;

    @Override
    public int[] interests() {
        return new int[]{
            MSGID_ENTER_SCENE_REQ,
            MSGID_LEAVE_SCENE_REQ,
            MSGID_MOVE_REQ,
            MSGID_PICKUP_ITEM_REQ,
            MSGID_INTERACT_NPC_REQ
        };
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        // ⚠️ QUAN TRỌNG: phải .subscribeOn(feignScheduler) để các blocking call (gRPC + Feign)
        // chạy trên virtual-thread pool, KHÔNG chạy trên Netty event-loop thread.
        // Nếu thiếu dòng này: 1 service chết → block Netty thread → toàn bộ WebSocket server đứng.
        Mono<Void> work = Mono.fromRunnable(() -> {
            if (session.getRoleId() == null) {
                log.warn("[World] User not logged in, sessionId: {}", session.getSessionId());
                return;
            }

            try {
                switch (msgId) {
                    case MSGID_ENTER_SCENE_REQ:
                        handleEnterScene(session, payload);
                        break;
                    case MSGID_LEAVE_SCENE_REQ:
                        handleLeaveScene(session, payload);
                        break;
                    case MSGID_MOVE_REQ:
                        handleMove(session, payload);
                        break;
                    case MSGID_PICKUP_ITEM_REQ:
                        handlePickupItem(session, payload);
                        break;
                    case MSGID_INTERACT_NPC_REQ:
                        handleInteractNpc(session, payload);
                        break;
                    default:
                        log.warn("[World] Unknown msgId: {}", msgId);
                }
            } catch (Exception e) {
                log.error("[World] Error handling msgId {}: {}", msgId, e.getMessage(), e);
            }
        });
        // Chạy trên virtual-thread pool → các service khác down sẽ không ảnh hưởng Netty threads.
        // Dùng typed variable 'work' để Java compiler giữ nguyên Mono<Void> type context.
        return work
                .subscribeOn(feignScheduler)
                .timeout(Duration.ofSeconds(8))
                .onErrorResume(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.error("[World] TIMEOUT msgId={} roleId={} — world-service có thể đang down",
                                msgId, session.getRoleId());
                    } else {
                        log.error("[World] Error msgId={}: {}", msgId, ex.getMessage());
                    }
                    return Mono.empty();
                });
    }

    /**
     * Handle enter scene request
     */
    private void handleEnterScene(PlayerSession session, byte[] payload) {
        try {
            Msgworld.PB_CSEnterSceneReq req = Msgworld.PB_CSEnterSceneReq.parseFrom(payload);
            int sceneId = req.getSceneId();
            int enterType = req.getEnterType();

            log.info("[World] Enter scene - roleId: {}, sceneId: {}, enterType: {}", 
                    session.getRoleId(), sceneId, enterType);

            // Call world service via gRPC to enter zone
            gameWorldGrpcClient.enterZone(
                session.getRoleId(), 
                sceneId,
                req.hasTargetPos() ? req.getTargetPos().getX() : 100.0f,
                req.hasTargetPos() ? req.getTargetPos().getY() : 0.0f,
                req.hasTargetPos() ? req.getTargetPos().getZ() : 100.0f
            );

            // Send enter scene acknowledgment
            Msgworld.PB_SCEnterSceneAck.Builder ackBuilder = Msgworld.PB_SCEnterSceneAck.newBuilder();
            ackBuilder.setRetCode(0);
            ackBuilder.setRetMsg("Success");
            ackBuilder.setSceneId(sceneId);
            
            // Set spawn position (from service or default)
            Msgworld.PB_Position.Builder posBuilder = Msgworld.PB_Position.newBuilder();
            posBuilder.setX(100.0f);  // Default spawn position
            posBuilder.setY(0.0f);
            posBuilder.setZ(100.0f);
            ackBuilder.setSpawnPos(posBuilder.build());
            
            ackBuilder.setServerTime(System.currentTimeMillis());

            Emitters.emit(session, MSGID_ENTER_SCENE_ACK, ackBuilder.build().toByteArray());

            // Track current scene in session so movement calls know the zone
            session.setCurrentSceneId(sceneId);

            log.info("[World] Enter scene success - roleId: {}, sceneId: {}", session.getRoleId(), sceneId);

            // Send scene info and visible objects to the entering player
            sendSceneInfo(session, sceneId);

        } catch (Exception e) {
            log.error("[World] Error handling enter scene: {}", e.getMessage(), e);
            sendEnterSceneError(session);
        }
    }

    /**
     * Handle leave scene request
     */
    private void handleLeaveScene(PlayerSession session, byte[] payload) {
        try {
            Msgworld.PB_CSLeaveSceneReq req = Msgworld.PB_CSLeaveSceneReq.parseFrom(payload);
            int sceneId = req.getSceneId();
            int leaveType = req.getLeaveType();

            log.info("[World] Leave scene - roleId: {}, sceneId: {}, leaveType: {}", 
                    session.getRoleId(), sceneId, leaveType);

            // Call world service via gRPC to leave zone
            gameWorldGrpcClient.leaveZone(session.getRoleId(), sceneId);

            // Send leave scene acknowledgment
            Msgworld.PB_SCLeaveSceneAck.Builder ackBuilder = Msgworld.PB_SCLeaveSceneAck.newBuilder();
            ackBuilder.setRetCode(0);
            ackBuilder.setRetMsg("Success");

            Emitters.emit(session, MSGID_LEAVE_SCENE_ACK, ackBuilder.build().toByteArray());

            session.setCurrentSceneId(0);   // Clear zone tracking
            log.info("[World] Leave scene success - roleId: {}, sceneId: {}", session.getRoleId(), sceneId);

        } catch (Exception e) {
            log.error("[World] Error handling leave scene: {}", e.getMessage(), e);
            sendLeaveSceneError(session);
        }
    }

    /**
     * Handle movement request
     */
    private void handleMove(PlayerSession session, byte[] payload) {
        try {
            Msgworld.PB_CSMoveReq req = Msgworld.PB_CSMoveReq.parseFrom(payload);

            log.debug("[World] Move - roleId: {}, from: ({},{},{}), to: ({},{},{})", 
                    session.getRoleId(),
                    req.getStartPos().getX(), req.getStartPos().getY(), req.getStartPos().getZ(),
                    req.getEndPos().getX(), req.getEndPos().getY(), req.getEndPos().getZ());

            // Call world service via gRPC to update position (<25ms target)
            gameWorldGrpcClient.updatePosition(
                session.getRoleId(),
                session.getCurrentSceneId(),   // zone tracked since handleEnterScene
                req.getEndPos().getX(),
                req.getEndPos().getY(),
                req.getEndPos().getZ(),
                req.hasDirection() ? req.getDirection().getX() : 0.0f,
                "WALKING" // Movement state
            );

            // Send move acknowledgment
            Msgworld.PB_SCMoveAck.Builder ackBuilder = Msgworld.PB_SCMoveAck.newBuilder();
            ackBuilder.setRetCode(0);
            ackBuilder.setPosition(req.getEndPos());
            ackBuilder.setTimestamp(System.currentTimeMillis());

            Emitters.emit(session, MSGID_MOVE_ACK, ackBuilder.build().toByteArray());

            // Broadcast movement to other players in view
            broadcastObjectMove(session, req);

        } catch (Exception e) {
            log.error("[World] Error handling move: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle pickup item request
     */
    private void handlePickupItem(PlayerSession session, byte[] payload) {
        try {
            Msgworld.PB_CSPickupItemReq req = Msgworld.PB_CSPickupItemReq.parseFrom(payload);
            long itemUid = req.getItemUid();

            log.info("[World] Pickup item - roleId: {}, itemUid: {}", session.getRoleId(), itemUid);

            // ⚠️ FIX: set auth token trước khi gọi Feign (FeignAuthInterceptor đọc từ ThreadLocal này)
            // Thiếu bước này → 401 Unauthorized → world-service từ chối request
            FeignTokenHolder.set(session.getSessionId());
            String result;
            try {
                result = worldFeign.pickupItem(session.getRoleId(), itemUid);
            } finally {
                FeignTokenHolder.clear();
            }

            // Parse result (format: "itemId:count" or "error")
            Msgworld.PB_SCPickupItemAck.Builder ackBuilder = Msgworld.PB_SCPickupItemAck.newBuilder();

            if (result != null && result.contains(":")) {
                String[] parts = result.split(":");
                int itemId = Integer.parseInt(parts[0]);
                int count = Integer.parseInt(parts[1]);

                ackBuilder.setRetCode(0);
                ackBuilder.setRetMsg("Success");
                ackBuilder.setItemUid(itemUid);
                ackBuilder.setItemId(itemId);
                ackBuilder.setItemCount(count);

                log.info("[World] Pickup item success - roleId: {}, itemId: {}, count: {}",
                        session.getRoleId(), itemId, count);
            } else {
                ackBuilder.setRetCode(1);
                ackBuilder.setRetMsg("Item not found or cannot pickup");
                ackBuilder.setItemUid(itemUid);
            }

            Emitters.emit(session, MSGID_PICKUP_ITEM_ACK, ackBuilder.build().toByteArray());

        } catch (Exception e) {
            log.error("[World] Error handling pickup item: {}", e.getMessage(), e);
            sendPickupItemError(session);
        }
    }

    /**
     * Handle interact with NPC request
     */
    private void handleInteractNpc(PlayerSession session, byte[] payload) {
        try {
            Msgworld.PB_CSInteractNpcReq req = Msgworld.PB_CSInteractNpcReq.parseFrom(payload);
            int npcId = req.getNpcId();
            int interactType = req.getInteractType();

            log.info("[World] Interact NPC - roleId: {}, npcId: {}, type: {}",
                    session.getRoleId(), npcId, interactType);

            // ⚠️ FIX: set auth token trước khi gọi Feign
            FeignTokenHolder.set(session.getSessionId());
            try {
                worldFeign.interactNpc(session.getRoleId(), npcId, interactType);
            } finally {
                FeignTokenHolder.clear();
            }

            // Send interact acknowledgment
            Msgworld.PB_SCInteractNpcAck.Builder ackBuilder = Msgworld.PB_SCInteractNpcAck.newBuilder();
            ackBuilder.setRetCode(0);
            ackBuilder.setRetMsg("Success");
            ackBuilder.setNpcId(npcId);
            ackBuilder.setInteractType(interactType);

            Emitters.emit(session, MSGID_INTERACT_NPC_ACK, ackBuilder.build().toByteArray());

            log.info("[World] Interact NPC success - roleId: {}, npcId: {}", session.getRoleId(), npcId);

        } catch (Exception e) {
            log.error("[World] Error handling interact NPC: {}", e.getMessage(), e);
            sendInteractNpcError(session);
        }
    }

    /**
     * Send scene info to client
     */
    private void sendSceneInfo(PlayerSession session, int sceneId) {
        try {
            // Get zone info via gRPC
            ZoneInfoResponse zoneInfo = gameWorldGrpcClient.getZoneInfo(sceneId, true, true);

            // Build scene info message
            Msgworld.PB_SCSceneInfo.Builder builder = Msgworld.PB_SCSceneInfo.newBuilder();
            builder.setSceneId(sceneId);
            builder.setSceneName("Scene " + sceneId);
            builder.setSceneType(1);  // 1-主城, 2-野外, 3-副本

            // Add players and entities from gRPC response
            if (zoneInfo != null && zoneInfo.getStatus().getSuccess()) {
                builder.setSceneName(zoneInfo.getZoneName());
                log.debug("[World] Zone info: {} players, {} entities", 
                    zoneInfo.getPlayerCount(), zoneInfo.getEntitiesCount());
            }

            Emitters.emit(session, MSGID_SCENE_INFO, builder.build().toByteArray());

            log.debug("[World] Sent scene info - roleId: {}, sceneId: {}", session.getRoleId(), sceneId);
        } catch (Exception e) {
            log.error("[World] Error sending scene info: {}", e.getMessage(), e);
        }
    }

    // Error response methods
    private void sendEnterSceneError(PlayerSession session) {
        try {
            Msgworld.PB_SCEnterSceneAck.Builder ackBuilder = Msgworld.PB_SCEnterSceneAck.newBuilder();
            ackBuilder.setRetCode(1);
            ackBuilder.setRetMsg("Enter scene failed");
            Emitters.emit(session, MSGID_ENTER_SCENE_ACK, ackBuilder.build().toByteArray());
        } catch (Exception e) {
            log.error("[World] Error sending enter scene error: {}", e.getMessage(), e);
        }
    }

    private void sendLeaveSceneError(PlayerSession session) {
        try {
            Msgworld.PB_SCLeaveSceneAck.Builder ackBuilder = Msgworld.PB_SCLeaveSceneAck.newBuilder();
            ackBuilder.setRetCode(1);
            ackBuilder.setRetMsg("Leave scene failed");
            Emitters.emit(session, MSGID_LEAVE_SCENE_ACK, ackBuilder.build().toByteArray());
        } catch (Exception e) {
            log.error("[World] Error sending leave scene error: {}", e.getMessage(), e);
        }
    }

    private void sendPickupItemError(PlayerSession session) {
        try {
            Msgworld.PB_SCPickupItemAck.Builder ackBuilder = Msgworld.PB_SCPickupItemAck.newBuilder();
            ackBuilder.setRetCode(2);
            ackBuilder.setRetMsg("Pickup failed");
            ackBuilder.setItemUid(0L);
            Emitters.emit(session, MSGID_PICKUP_ITEM_ACK, ackBuilder.build().toByteArray());
        } catch (Exception e) {
            log.error("[World] Error sending pickup item error: {}", e.getMessage(), e);
        }
    }

    private void sendInteractNpcError(PlayerSession session) {
        try {
            Msgworld.PB_SCInteractNpcAck.Builder ackBuilder = Msgworld.PB_SCInteractNpcAck.newBuilder();
            ackBuilder.setRetCode(1);
            ackBuilder.setRetMsg("Interact failed");
            ackBuilder.setNpcId(0);
            Emitters.emit(session, MSGID_INTERACT_NPC_ACK, ackBuilder.build().toByteArray());
        } catch (Exception e) {
            log.error("[World] Error sending interact NPC error: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast object movement to other players in view
     * This would typically use a pub-sub pattern or direct WebSocket push
     */
    private void broadcastObjectMove(PlayerSession session, Msgworld.PB_CSMoveReq req) {
        try {
            // Build broadcast message
            Msgworld.PB_SCObjectMove.Builder builder = Msgworld.PB_SCObjectMove.newBuilder();
            builder.setObjType(1);  // 1=Player, 2=Monster
            builder.setObjId(0L);   // roleId is ULID String — proto uses long, skip for now
            builder.setStartPos(req.getStartPos());
            builder.setEndPos(req.getEndPos());
            builder.setDirection(req.getDirection());
            builder.setSpeed(1.0f);  // Default speed
            builder.setTimestamp(System.currentTimeMillis());

            byte[] broadcastData = builder.build().toByteArray();

            int sceneId = session.getCurrentSceneId();
            if (sceneId <= 0) return;
            try {
                NearbyPlayersResponse nearbyPlayers = gameWorldGrpcClient.getNearbyPlayers(
                        session.getRoleId(),
                        sceneId,
                        req.getEndPos().getX(),
                        req.getEndPos().getY(),
                        req.getEndPos().getZ(),
                        MOVE_BROADCAST_RADIUS,
                        MOVE_BROADCAST_MAX_PLAYERS
                );
                if (nearbyPlayers != null && nearbyPlayers.getStatus().getSuccess()) {
                    long selfRoleId = session.getRoleId();
                    nearbyPlayers.getPlayersList().forEach(playerInfo -> {
                        long nearbyRoleId = playerInfo.getRoleId();
                        if (nearbyRoleId == selfRoleId) return;
                        sessionRegistry.getByRoleId(nearbyRoleId).ifPresent(nearby ->
                                nearby.sendBinary(PacketCodec.encode(MSGID_OBJECT_MOVE, broadcastData)));
                    });
                }
            } catch (Exception grpcEx) {
                // Broadcast failure is non-fatal; the client will re-sync on next move.
                log.debug("[World] Broadcast skipped (nearby query unavailable): {}", grpcEx.getMessage());
            }

        } catch (Exception e) {
            log.error("[World] Error broadcasting object move: {}", e.getMessage(), e);
        }
    }
}
