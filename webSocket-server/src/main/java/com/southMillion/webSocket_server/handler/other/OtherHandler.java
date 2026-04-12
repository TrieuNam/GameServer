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
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OtherHandler.class);

    private static final int LIMIT_CORE_TYPES = 6;
    private static final int GET_TYPE_CORECRISIS_BOX = 93;

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
                // Read all 6 core levels
                if (result.get("coreLevels") instanceof List<?> levels) {
                    for (Object lv : levels) {
                        builder.addCoreLevel(lv instanceof Number n ? n.intValue() : 0);
                    }
                }
                // DRAW (type=1): also emit 1507 PB_SCGetItemNotice with drawn chips
                if (req.getType() == 1 && result.get("drawnItems") instanceof List<?> items && !items.isEmpty()) {
                        Msgknapsack.PB_SCGetItemNotice.Builder notice = Msgknapsack.PB_SCGetItemNotice.newBuilder()
                            .setGetType(GET_TYPE_CORECRISIS_BOX); // PUT_REASON_CORECRISIS_BOX (client BagEnum.GET_TYPE)
                    for (Object obj : items) {
                        if (obj instanceof Map<?, ?> m) {
                            int itemId = m.get("itemId") instanceof Number n ? n.intValue() : 0;
                            int num    = m.get("num")    instanceof Number n ? n.intValue() : 0;
                            if (itemId > 0 && num > 0) {
                                notice.addItemList(Msgknapsack.PB_ItemData.newBuilder()
                                        .setItemId(itemId).setNum(num).build());
                            }
                        }
                    }
                    Emitters.emit(session, MsgIds.SC_GET_ITEM_NOTICE, notice.build().toByteArray());
                }
            }
        } catch (Exception e) {
            log.error("[LimitCore] Error calling backend", e);
        }
        ensureLimitCoreLevels(builder);
        Emitters.emit(session, MsgIds.SC_LIMIT_CORE_INFO, builder.build().toByteArray());
    }

    /**
     * Push 1468 PB_SCLimitCoreInfo to player — called at login bootstrap.
     */
    public Mono<Void> pushLimitCoreInfo(PlayerSession session) {
        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
            Msgother.PB_SCLimitCoreInfo.Builder builder = Msgother.PB_SCLimitCoreInfo.newBuilder();
            try {
                Map<String, Object> result = roleFeign.getLimitCoreInfo(String.valueOf(roleId));
                if (result != null && result.get("coreLevels") instanceof List<?> levels) {
                    for (Object lv : levels) {
                        builder.addCoreLevel(lv instanceof Number n ? n.intValue() : 0);
                    }
                }
            } catch (Exception e) {
                log.warn("[LimitCore] pushLimitCoreInfo failed roleId={}: {}", roleId, e.getMessage());
            }
            ensureLimitCoreLevels(builder);
            Emitters.emit(session, MsgIds.SC_LIMIT_CORE_INFO, builder.build().toByteArray());
        });
    }

    private void ensureLimitCoreLevels(Msgother.PB_SCLimitCoreInfo.Builder builder) {
        while (builder.getCoreLevelCount() < LIMIT_CORE_TYPES) {
            builder.addCoreLevel(0);
        }
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

    // 1685 → 1686 + 1687 + 1688: Item recycle level-up
    private void handleItemRecycle(PlayerSession session, byte[] payload) throws Exception {
        Msgother.PB_CSItemRecycleLevelUpReq req = Msgother.PB_CSItemRecycleLevelUpReq.parseFrom(payload);
        Long roleId = session.getRoleId();
        List<Integer> itemIds = req.getItemIdsList();

        log.info("[ItemRecycle] items={} roleId={}", itemIds, roleId);

        // 1686: level + exp sau khi recycle
        Msgother.PB_SCItemRecycleInfo.Builder infoBuilder = Msgother.PB_SCItemRecycleInfo.newBuilder();
        try {
            Map<String, Object> result = bagFeign.recycleItems(String.valueOf(roleId), itemIds);
            if (result != null) {
                if (result.get("level") instanceof Number n) infoBuilder.setLevel(n.intValue());
                if (result.get("exp") instanceof Number n)   infoBuilder.setExp(n.longValue());
            }
        } catch (Exception e) {
            log.error("[ItemRecycle] Error calling backend", e);
        }
        Emitters.emit(session, 1686, infoBuilder.build().toByteArray());

        // 1688: thông báo từng item đã bị consume (num = 0 = đã dùng hết 1 unit)
        // + 1687: danh sách bag hiện tại sau recycle
        try {
            List<org.SouthMillion.dto.bag.BagDTOs.ItemView> bagList =
                    bagFeign.list(String.valueOf(roleId));

            // Build map itemId -> remaining num để gửi 1688 chính xác
            Map<Integer, Long> remainingMap = new java.util.HashMap<>();
            if (bagList != null) {
                for (org.SouthMillion.dto.bag.BagDTOs.ItemView iv : bagList) {
                    if (iv.getItemId() != null) {
                        remainingMap.merge(iv.getItemId(),
                                iv.getNum() != null ? iv.getNum().longValue() : 0L, Long::sum);
                    }
                }
            }

            // 1688 — một message cho mỗi item đã recycle
            for (Integer itemId : itemIds) {
                long remaining = remainingMap.getOrDefault(itemId, 0L);
                Msgother.PB_SCItemRecycleOneInfo oneInfo = Msgother.PB_SCItemRecycleOneInfo.newBuilder()
                        .setItemData(org.SouthMillion.proto.Msgknapsack.Msgknapsack.PB_ItemData.newBuilder()
                                .setItemId(itemId)
                                .setNum(remaining)
                                .build())
                        .build();
                Emitters.emit(session, 1688, oneInfo.toByteArray());
            }

            // 1687 — full list sau recycle
            Msgother.PB_SCItemRecycleListInfo.Builder listBuilder =
                    Msgother.PB_SCItemRecycleListInfo.newBuilder();
            if (bagList != null) {
                for (org.SouthMillion.dto.bag.BagDTOs.ItemView iv : bagList) {
                    if (iv.getItemId() != null) {
                        listBuilder.addItemList(
                                org.SouthMillion.proto.Msgknapsack.Msgknapsack.PB_ItemData.newBuilder()
                                        .setItemId(iv.getItemId())
                                        .setNum(iv.getNum() != null ? iv.getNum().longValue() : 0L)
                                        .build());
                    }
                }
            }
            Emitters.emit(session, 1687, listBuilder.build().toByteArray());
        } catch (Exception e) {
            log.warn("[ItemRecycle] Error pushing 1687/1688 for roleId={}: {}", roleId, e.getMessage());
        }
    }

    /**
     * Push 1686 PB_SCItemRecycleInfo (level + exp) — gọi khi login bootstrap.
     */
    public Mono<Void> pushRecycleInfo(PlayerSession session) {
        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
            Msgother.PB_SCItemRecycleInfo.Builder builder = Msgother.PB_SCItemRecycleInfo.newBuilder();
            try {
                Map<String, Object> result = bagFeign.getRecycleProgress(String.valueOf(roleId));
                if (result != null) {
                    if (result.get("level") instanceof Number n) builder.setLevel(n.intValue());
                    if (result.get("exp") instanceof Number n)   builder.setExp(n.longValue());
                }
            } catch (Exception e) {
                log.warn("[ItemRecycle] pushRecycleInfo failed roleId={}: {}", roleId, e.getMessage());
                builder.setLevel(1).setExp(0);
            }
            Emitters.emit(session, 1686, builder.build().toByteArray());
        });
    }

    /**
     * Push 1687 PB_SCItemRecycleListInfo (bag items list) — gọi khi login bootstrap.
     * Client dùng CfgItemRetrieve để lọc những item nào có thể recycle.
     */
    public Mono<Void> pushRecycleListInfo(PlayerSession session) {
        return Mono.fromRunnable(() -> {
            Long roleId = session.getRoleId();
            Msgother.PB_SCItemRecycleListInfo.Builder builder =
                    Msgother.PB_SCItemRecycleListInfo.newBuilder();
            try {
                List<org.SouthMillion.dto.bag.BagDTOs.ItemView> bagList =
                        bagFeign.list(String.valueOf(roleId));
                if (bagList != null) {
                    for (org.SouthMillion.dto.bag.BagDTOs.ItemView iv : bagList) {
                        if (iv.getItemId() != null) {
                            builder.addItemList(
                                    org.SouthMillion.proto.Msgknapsack.Msgknapsack.PB_ItemData.newBuilder()
                                            .setItemId(iv.getItemId())
                                            .setNum(iv.getNum() != null ? iv.getNum().longValue() : 0L)
                                            .build());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[ItemRecycle] pushRecycleListInfo failed roleId={}: {}", roleId, e.getMessage());
            }
            Emitters.emit(session, 1687, builder.build().toByteArray());
        });
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
