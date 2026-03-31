package com.SouthMillion.webSocket_server.handler.other;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.PetFeign;
import com.SouthMillion.webSocket_server.service.client.RoleFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.advertisment.AdvertisementDTOs;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for msgother.proto systems.
 *
 * Handled CS msgIds:
 *   1467 PB_CSLimitCoreReq           → Limit core / breakthrough, responds 1468 PB_SCLimitCoreInfo
 *   1655 PB_CSDuoBaoReq              → DuoBao (夺宝) ops, responds 1656 PB_SCDuoBaoInfo
 *   1663 PB_CSAdvertisementFetch     → Ad reward fetch, responds 1662 PB_SCAdvertisementInfo
 *   1685 PB_CSItemRecycleLevelUpReq  → Item recycle, responds 1686 PB_SCItemRecycleInfo
 *   1690 PB_CSPetFbReq               → Pet dungeon (宠物副本), responds 1691 PB_SCPetFbInfo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtherHandler implements MessageHandler {

    private final ActivityFeign activityFeign;
    private final BagFeign bagFeign;
    private final PetFeign petFeign;
    private final RoleFeign roleFeign;

    @Override
    public int[] interests() {
        return new int[]{MsgIds.CS_LIMIT_CORE_REQ, 1655, 1663, 1685, 1690};
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                switch (msgId) {
                    case MsgIds.CS_LIMIT_CORE_REQ -> handleLimitCore(session, payload);
                    case 1655 -> handleDuoBao(session, payload);
                    case 1663 -> handleAdvertisement(session, payload);
                    case 1685 -> handleItemRecycle(session, payload);
                    case 1690 -> handlePetFb(session, payload);
                    default   -> log.warn("[Other] Unhandled msgId={}", msgId);
                }
            } catch (Exception e) {
                log.error("[Other] Error handling msgId={} for roleId={}", msgId, session.getRoleId(), e);
            }
        });
    }

    // 1467 → 1468: Limit Core / 限界突破
    private void handleLimitCore(PlayerSession session, byte[] payload) throws Exception {
        Msgother.PB_CSLimitCoreReq req = Msgother.PB_CSLimitCoreReq.parseFrom(payload);
        Long roleId = session.getRoleId();

        log.info("[LimitCore] type={} p1={} roleId={}", req.getType(), req.getP1(), roleId);

        Msgother.PB_SCLimitCoreInfo.Builder builder = Msgother.PB_SCLimitCoreInfo.newBuilder();
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("type", req.getType());
            request.put("p1",   req.getP1());
            Map<String, Object> result = roleFeign.limitCore(String.valueOf(roleId), request);
            if (result != null) {
                if (result.get("level") instanceof Number n) builder.addCoreLevel(n.intValue());
            }
        } catch (Exception e) {
            log.error("[LimitCore] Error calling backend", e);
        }
        Emitters.emit(session, MsgIds.SC_LIMIT_CORE_INFO, builder.build().toByteArray());
    }

    // 1655 → 1656: DuoBao (夺宝)
    private void handleDuoBao(PlayerSession session, byte[] payload) throws Exception {
        Msgother.PB_CSDuoBaoReq req = Msgother.PB_CSDuoBaoReq.parseFrom(payload);
        Long roleId = session.getRoleId();

        log.info("[DuoBao] op={} p1={} p2={} roleId={}",
                req.getOpType(), req.getParam1(), req.getParam2(), roleId);

        Msgother.PB_SCDuoBaoInfo.Builder builder = Msgother.PB_SCDuoBaoInfo.newBuilder();
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("opType", req.getOpType());
            request.put("param1", req.getParam1());
            request.put("param2", req.getParam2());

            Map<String, Object> result = activityFeign.duobaoOperation(String.valueOf(roleId), request);

            if (result != null && result.get("dataList") instanceof List<?> dataList) {
                for (Object item : dataList) {
                    if (item instanceof Map<?, ?> m) {
                        Msgother.PB_DuoBaoData.Builder data = Msgother.PB_DuoBaoData.newBuilder();
                        if (m.get("integral") instanceof Number n)        data.setIntegral(n.intValue());
                        if (m.get("fetchFlag") instanceof Number n)       data.setFetchFlag(n.intValue());
                        if (m.get("freeRefreshNum") instanceof Number n)  data.setFreeRefreshNum(n.intValue());
                        if (m.get("freeRefreshTime") instanceof Number n) data.setFreeRefreshTime(n.intValue());
                        builder.addDataList(data);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[DuoBao] Error calling backend", e);
        }
        Emitters.emit(session, 1656, builder.build().toByteArray());
    }

    // 1663 → 1662: Advertisement fetch — delegates to role-service
    private void handleAdvertisement(PlayerSession session, byte[] payload) throws Exception {
        Msgother.PB_CSAdvertisementFetch req = Msgother.PB_CSAdvertisementFetch.parseFrom(payload);
        Long roleId = session.getRoleId();

        log.info("[Ad] seq={} is_dia={} roleId={}", req.getSeq(), req.getIsDia(), roleId);

        Msgother.PB_SCAdvertisementInfo.Builder builder = Msgother.PB_SCAdvertisementInfo.newBuilder();
        try {
            AdvertisementDTOs.AdFetchReq adReq = new AdvertisementDTOs.AdFetchReq(
                    String.valueOf(roleId), String.valueOf(roleId),
                    req.getSeq(), req.getIsDia(), req.getParam());

            AdvertisementDTOs.AdInfo adInfo = roleFeign.claimAd(adReq);

            if (adInfo != null) {
                Msgother.PB_SCAdvertisement.Builder ad = Msgother.PB_SCAdvertisement.newBuilder();
                ad.setSeq(adInfo.seq());
                ad.setTodayCount(adInfo.claimedToday() ? 1 : 0);
                if (adInfo.nextClaimAt() != null) {
                    ad.setNextFetchTime((int) adInfo.nextClaimAt().getEpochSecond());
                }
                builder.addAdList(ad);
            }
            builder.setIsInit(0);
        } catch (Exception e) {
            log.error("[Ad] Error calling backend", e);
            builder.setIsInit(1);
        }
        Emitters.emit(session, 1662, builder.build().toByteArray());
    }

    // 1685 → 1686: Item recycle level-up
    private void handleItemRecycle(PlayerSession session, byte[] payload) throws Exception {
        Msgother.PB_CSItemRecycleLevelUpReq req = Msgother.PB_CSItemRecycleLevelUpReq.parseFrom(payload);
        Long roleId = session.getRoleId();

        log.info("[ItemRecycle] items={} roleId={}", req.getItemIdsList(), roleId);

        Msgother.PB_SCItemRecycleInfo.Builder builder = Msgother.PB_SCItemRecycleInfo.newBuilder();
        try {
            Map<String, Object> result = bagFeign.recycleItems(String.valueOf(roleId), req.getItemIdsList());
            if (result != null) {
                if (result.get("level") instanceof Number n) builder.setLevel(n.intValue());
                if (result.get("exp") instanceof Number n)   builder.setExp(n.longValue());
            }
        } catch (Exception e) {
            log.error("[ItemRecycle] Error calling backend", e);
        }
        Emitters.emit(session, 1686, builder.build().toByteArray());
    }

    // 1690 → 1691: Pet dungeon (宠物副本)
    private void handlePetFb(PlayerSession session, byte[] payload) throws Exception {
        Msgother.PB_CSPetFbReq req = Msgother.PB_CSPetFbReq.parseFrom(payload);
        Long roleId = session.getRoleId();

        log.info("[PetFb] type={} p1={} roleId={}", req.getType(), req.getP1(), roleId);

        Msgother.PB_SCPetFbInfo.Builder builder = Msgother.PB_SCPetFbInfo.newBuilder();
        try {
            Map<String, Object> result;
            switch (req.getType()) {
                case 1 -> result = petFeign.getPetDungeonInfo(String.valueOf(roleId));
                case 2 -> result = petFeign.startPetDungeon(String.valueOf(roleId), req.getP1());
                case 3 -> result = petFeign.claimPetDungeonReward(String.valueOf(roleId), req.getP1());
                default -> {
                    log.warn("[PetFb] Unknown type: {}", req.getType());
                    result = Map.of("success", false);
                }
            }
            if (result != null) {
                if (result.get("passLevel") instanceof Number n)  builder.setPassLevel(n.intValue());
                if (result.get("fetchFlag") instanceof Number n)  builder.setFetchFlag(n.longValue());
            }
        } catch (Exception e) {
            log.error("[PetFb] Error calling backend", e);
        }
        Emitters.emit(session, 1691, builder.build().toByteArray());
    }
}
