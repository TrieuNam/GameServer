package com.SouthMillion.webSocket_server.handler.starmap;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskCondition;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.grpc.StarMapGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgstarmap.Msgstarmap;
import org.SouthMillion.proto.starmap.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class StarMapHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StarMapHandler.class);

    private final StarMapGrpcClient starMapGrpcClient;
    private final TaskProgressPublisher taskProgressPublisher;

    private static final int OP_GET_INFO    = 0;
    private static final int OP_ACTIVATE    = 1;
    private static final int OP_UPGRADE     = 2;
    private static final int OP_RESET       = 3;
    private static final int OP_APPLY_PRESET = 4;

    private static final int SC_STARMAP_INFO  = 2151;
    private static final int SC_STARMAP_OP    = 2152;

    @Override
    public int[] interests() { return new int[]{2150}; }

    /** Push star map info after login. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> handleGetInfo(session, roleId));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgstarmap.PB_CSStarMapReq req = Msgstarmap.PB_CSStarMapReq.parseFrom(payload);
                Long roleId = session.getRoleId();
                int opType = req.hasReqType() ? req.getReqType() : 0;
                log.debug("[StarMap] opType={} roleId={}", opType, roleId);
                switch (opType) {
                    case OP_GET_INFO    -> handleGetInfo(session, roleId);
                    case OP_ACTIVATE    -> handleActivate(session, roleId, req);
                    case OP_UPGRADE     -> handleLevelUp(session, roleId, req);
                    default             -> sendOpRet(session, opType, 0);
                }
            } catch (Exception e) {
                log.error("[StarMap] Error roleId={}", session.getRoleId(), e);
            }
        });
    }

    // ── handlers ─────────────────────────────────────────────────────────────

    private void handleGetInfo(PlayerSession session, Long roleId) {
        long userId = roleId;
        GetAllStarsResponse starsResp = starMapGrpcClient.getAllStars(userId);
        GetAllConstellationsResponse constResp = starMapGrpcClient.getAllConstellations(userId);

        Msgstarmap.PB_SCStarMapInfo.Builder sc = Msgstarmap.PB_SCStarMapInfo.newBuilder();
        for (StarData s : starsResp.getStarsList()) {
            sc.addStarMapList(Msgstarmap.PB_SCStarMapData.newBuilder()
                    .setId(s.getStarId())
                    .setLevel(s.getLevel())
                    .setGrade(s.getLevel() / 10)   // approximate grade
                    .build());
        }
        // nb_star_list from constellations
        for (ConstellationData c : constResp.getConstellationsList()) {
            sc.addNbStarList(Msgstarmap.PB_NBStarNode.newBuilder()
                    .setBigStarMapLevel(c.getLevel())
                    .setBigStarConsume(0)
                    .build());
        }
        Emitters.emit(session, SC_STARMAP_INFO, sc.build().toByteArray());
        log.debug("[StarMap] pushInfo roleId={} stars={}", roleId, starsResp.getStarsCount());
    }

    private void handleActivate(PlayerSession session, Long roleId, Msgstarmap.PB_CSStarMapReq req) {
        int starId = req.hasParam1() ? req.getParam1() : 0;
        ActivateStarResponse r = starMapGrpcClient.activateStar(roleId, starId);
        if (r.hasStatus() && r.getStatus().getSuccess()) {
            taskProgressPublisher.publish(roleId, TaskCondition.STARMAP_ACTIVATE.taskKey(), 1, "websocket-starmap-activate");
        }
        sendOpRet(session, OP_ACTIVATE, r.getStatus().getCode());
    }

    private void handleLevelUp(PlayerSession session, Long roleId, Msgstarmap.PB_CSStarMapReq req) {
        int starId = req.hasParam1() ? req.getParam1() : 0;
        LevelUpStarResponse r = starMapGrpcClient.levelUpStar(roleId, starId);
        sendOpRet(session, OP_UPGRADE, r.getStatus().getCode());
    }

    private void sendOpRet(PlayerSession session, int retType, int param1) {
        byte[] bytes = Msgstarmap.PB_SCStarMapOpRet.newBuilder()
                .setRetType(retType).setParam1(param1).build().toByteArray();
        Emitters.emit(session, SC_STARMAP_OP, bytes);
    }
}
