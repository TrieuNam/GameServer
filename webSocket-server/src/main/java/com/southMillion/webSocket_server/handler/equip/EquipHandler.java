package com.SouthMillion.webSocket_server.handler.equip;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.TaskCondition;
import com.SouthMillion.webSocket_server.service.TaskProgressPublisher;
import com.SouthMillion.webSocket_server.service.client.EquipHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.equip.EquipDTOs;
import org.SouthMillion.dto.equip.EquipFumoDTOs;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class EquipHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EquipHandler.class);

    private final EquipHttpClient equipHttpClient;
    private final TaskProgressPublisher taskProgressPublisher;
    private final reactor.core.scheduler.Scheduler feignVtScheduler;

    // Legacy C++ req_type parity (other/equip/equip.cpp)
    private static final int OP_BAG_WEARING   = 1;
    private static final int OP_BAG_SELL      = 2;
    private static final int OP_FU_MO         = 3;
    private static final int OP_CANCEL_FU_MO  = 4;
    private static final int OP_TRANSFORM      = 5;

    private static final int MSG_SC_FUMO_LIST = 1603;
    private static final int MSG_SC_FUMO_ONE  = 1604;
    private static final int MSG_SC_EQUIP_LIST = 1605;
    private static final int MSG_SC_EQUIP_ONE  = 1606;
    private static final int MSG_SC_BAG_LIST   = 1607;
    private static final int MSG_SC_BAG_ONE    = 1608;
    private static final long BAG_SLOT_CACHE_TTL_MS = 250L;

    private final ConcurrentHashMap<Long, CachedBagSlots> bagSlotCache = new ConcurrentHashMap<>();

    @Override
    public int[] interests() {
        return new int[]{1600}; // PB_CSEquipReq
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.parseFrom(payload);
                int reqType = req.hasReqType() ? req.getReqType() : OP_BAG_WEARING;
                int param1  = req.hasParam1()  ? req.getParam1()  : 0;
                int param2  = req.hasParam2()  ? req.getParam2()  : 0;
                int param3  = req.hasParam3()  ? req.getParam3()  : 0;
                Long roleId = session.getRoleId();
                if (roleId == null) {
                    return;
                }

                log.debug("[Equip] reqType={}, p1={}, p2={}, p3={}, roleId={}", reqType, param1, param2, param3, roleId);

                switch (reqType) {
                    case OP_BAG_WEARING  -> handleBagWearing(session, roleId, param1);
                    case OP_BAG_SELL     -> handleBagSell(session, roleId, param1);
                    case OP_FU_MO        -> handleFuMo(session, roleId, param1);
                    case OP_CANCEL_FU_MO -> handleCancelFuMo(session, roleId, param1);
                    case OP_TRANSFORM    -> handleTransform(session, roleId, param1, param2, param3);
                    default -> {
                        log.warn("[Equip] Unknown reqType={}", reqType);
                        pushAll(session).subscribe();
                    }
                }
            } catch (Exception e) {
                log.error("[Equip] Error for roleId={}", session.getRoleId(), e);
                pushAll(session).subscribe();
            }
        });
    }

    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();

        // Parallel execution: fetch all equip data concurrently instead of sequentially
        // Old: sendEquipList() → sendFuMoList() → sendBagList() (sequential, ~1200ms total)
        // New: Mono.zip all 3 calls (parallel, ~500ms total - 60% faster)
        // Phase 5: Use virtual thread scheduler (feignVtScheduler) for better concurrency
        return Mono.zip(
                Mono.fromCallable(() -> equipHttpClient.list(String.valueOf(roleId)))
                        .subscribeOn(feignVtScheduler),
                Mono.fromCallable(() -> equipHttpClient.fumoList(String.valueOf(roleId)))
                        .subscribeOn(feignVtScheduler),
                Mono.fromCallable(() -> fetchBagSlots(roleId))
                        .subscribeOn(feignVtScheduler)
        ).flatMap(tuple -> {
            // Emit all responses
            sendEquipListFromResp(session, tuple.getT1());
            sendFuMoListFromResp(session, tuple.getT2());
            sendBagList(session, tuple.getT3());
            return Mono.<Void>empty();
        }).onErrorResume(e -> {
            log.warn("[Equip] pushAll parallel fetch error roleId={}: {}", roleId, e.getMessage());
            // Fallback: send empty responses
            sendEquipListFromResp(session, null);
            sendFuMoListFromResp(session, null);
            sendBagList(session, List.of());
            return Mono.<Void>empty();
        });
    }

    private void sendEquipListFromResp(PlayerSession session, EquipDTOs.ListResp resp) {
        try {
            Msgequip.PB_SCEquipListInfo.Builder b = Msgequip.PB_SCEquipListInfo.newBuilder();
            if (resp != null && resp.getItems() != null) {
                for (EquipDTOs.EquipItem item : resp.getItems()) {
                    b.addEquipList(toProto(item));
                }
            }
            Emitters.emit(session, MSG_SC_EQUIP_LIST, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Equip] sendEquipListFromResp error", e);
            Emitters.emit(session, MSG_SC_EQUIP_LIST, Msgequip.PB_SCEquipListInfo.newBuilder().build().toByteArray());
        }
    }

    private void sendFuMoListFromResp(PlayerSession session, EquipFumoDTOs.FumoListResp resp) {
        try {
            Msgequip.PB_SCEquipFuMoListInfo.Builder b = Msgequip.PB_SCEquipFuMoListInfo.newBuilder();
            if (resp != null && resp.fumoList() != null) {
                for (EquipFumoDTOs.FumoData fd : resp.fumoList()) {
                    if (fd != null) {
                        b.addFumoList(toProto(fd));
                    }
                }
            }
            Emitters.emit(session, MSG_SC_FUMO_LIST, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Equip] sendFuMoListFromResp error", e);
            Emitters.emit(session, MSG_SC_FUMO_LIST, Msgequip.PB_SCEquipFuMoListInfo.newBuilder().build().toByteArray());
        }
    }

    private void handleBagWearing(PlayerSession session, Long roleId, int indexOrItemId) {
        List<BagSlot> slots = fetchBagSlots(roleId);
        BagSlot target = findBagSlot(slots, indexOrItemId);
        if (target == null) {
            log.warn("[Equip] bag_wearing target not found, p1={}, roleId={}", indexOrItemId, roleId);
            sendBagList(session, slots);
            return;
        }

        try {
            equipHttpClient.wear(String.valueOf(roleId), target.itemId());
            reportTaskProgress(roleId, TaskCondition.GET_EQUIP.taskKey(), 1);
            reportQualityConditionProgress(roleId, target.quality());
            invalidateBagSlotsCache(roleId);
        } catch (Exception e) {
            log.error("[Equip] bag_wearing failed roleId={}, itemId={}", roleId, target.itemId(), e);
        }

        syncEquipAfterMutation(session, roleId, target.itemId(), target.equipType());
        sendBagOneEmpty(session, target.index(), target.equipType());
        sendBagList(session, fetchBagSlots(roleId));
    }

    private void handleBagSell(PlayerSession session, Long roleId, int indexOrItemId) {
        List<BagSlot> slots = fetchBagSlots(roleId);
        BagSlot target = findBagSlot(slots, indexOrItemId);
        if (target == null) {
            log.warn("[Equip] bag_sell target not found, p1={}, roleId={}", indexOrItemId, roleId);
            sendBagList(session, slots);
            return;
        }

        try {
            equipHttpClient.bagSell(Map.of(
                    "roleId", String.valueOf(roleId),
                    "itemId", target.itemId(),
                    "equipType", target.equipType(),
                    "index", target.index()
            ));
            reportTaskProgress(roleId, TaskCondition.SELL_EQUIP_NUM.taskKey(), 1);
            invalidateBagSlotsCache(roleId);
        } catch (Exception e) {
            log.warn("[Equip] bagSell failed roleId={}: {}", roleId, e.getMessage());
        }
        sendBagOneEmpty(session, target.index(), target.equipType());
        sendBagList(session, fetchBagSlots(roleId));
    }

    private void reportTaskProgress(Long roleId, String taskKey, int delta) {
        taskProgressPublisher.publish(roleId, taskKey, delta, "websocket-equip");
    }

    private void reportQualityConditionProgress(Long roleId, int quality) {
        for (TaskCondition condition : TaskCondition.equipQualityConditions(quality)) {
            reportTaskProgress(roleId, condition.taskKey(), 1);
        }
    }

    private void handleFuMo(PlayerSession session, Long roleId, int equipType) {
        if (equipType < 0) {
            log.warn("[Equip] fumo invalid equipType={}, roleId={}", equipType, roleId);
            return;
        }
        try {
            EquipFumoDTOs.FumoOneResp one = equipHttpClient.fumoAddExp(
                    new EquipFumoDTOs.AddExpReq(String.valueOf(roleId), equipType, 20, Map.of())
            );
            sendFuMoOne(session, equipType, one);
        } catch (Exception e) {
            log.error("[Equip] fumo add-exp failed roleId={}, equipType={}", roleId, equipType, e);
            sendFuMoOne(session, roleId, equipType);
        }
    }

    private void handleCancelFuMo(PlayerSession session, Long roleId, int equipType) {
        if (equipType < 0) {
            log.warn("[Equip] cancel_fumo invalid equipType={}, roleId={}", equipType, roleId);
            return;
        }
        try {
            EquipFumoDTOs.FumoOneResp one = equipHttpClient.fumoActivate(
                    new EquipFumoDTOs.ActivateReq(String.valueOf(roleId), equipType, 0)
            );
            sendFuMoOne(session, equipType, one);
        } catch (Exception e) {
            log.error("[Equip] cancel_fumo failed roleId={}, equipType={}", roleId, equipType, e);
            sendFuMoOne(session, roleId, equipType);
        }
    }

    private void handleTransform(PlayerSession session, Long roleId, int num1, int num2, int num3) {
        try {
            equipHttpClient.fumoTransform(Map.of(
                    "roleId", String.valueOf(roleId),
                    "count1", Math.max(0, num1),
                    "count2", Math.max(0, num2),
                    "count3", Math.max(0, num3)
            ));
            invalidateBagSlotsCache(roleId);
        } catch (Exception e) {
            log.warn("[Equip] transform failed roleId={}: {}", roleId, e.getMessage());
        }
        sendFuMoList(session, roleId);
        sendBagList(session, fetchBagSlots(roleId));
    }

    private void sendEquipList(PlayerSession session, Long roleId) {
        try {
            EquipDTOs.ListResp resp = equipHttpClient.list(String.valueOf(roleId));
            Msgequip.PB_SCEquipListInfo.Builder b = Msgequip.PB_SCEquipListInfo.newBuilder();
            if (resp != null && resp.getItems() != null) {
                for (EquipDTOs.EquipItem item : resp.getItems()) {
                    b.addEquipList(toProto(item));
                }
            }
            Emitters.emit(session, MSG_SC_EQUIP_LIST, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Equip] sendEquipList error roleId={}", roleId, e);
            Emitters.emit(session, MSG_SC_EQUIP_LIST, Msgequip.PB_SCEquipListInfo.newBuilder().build().toByteArray());
        }
    }

    private void sendFuMoList(PlayerSession session, Long roleId) {
        try {
            EquipFumoDTOs.FumoListResp resp = equipHttpClient.fumoList(String.valueOf(roleId));
            Msgequip.PB_SCEquipFuMoListInfo.Builder b = Msgequip.PB_SCEquipFuMoListInfo.newBuilder();
            if (resp != null && resp.fumoList() != null) {
                for (EquipFumoDTOs.FumoData fd : resp.fumoList()) {
                    if (fd != null) {
                        b.addFumoList(toProto(fd));
                    }
                }
            }
            Emitters.emit(session, MSG_SC_FUMO_LIST, b.build().toByteArray());
        } catch (Exception e) {
            log.error("[Equip] sendFuMoList error roleId={}", roleId, e);
            Emitters.emit(session, MSG_SC_FUMO_LIST, Msgequip.PB_SCEquipFuMoListInfo.newBuilder().build().toByteArray());
        }
    }

    private void sendFuMoOne(PlayerSession session, Long roleId, int equipType) {
        try {
            EquipFumoDTOs.FumoOneResp one = equipHttpClient.fumoOne(String.valueOf(roleId), equipType);
            sendFuMoOne(session, equipType, one);
        } catch (Exception e) {
            log.error("[Equip] sendFuMoOne fallback failed roleId={}, equipType={}", roleId, equipType, e);
            sendFuMoOne(session, equipType, null);
        }
    }

    private void sendFuMoOne(PlayerSession session, int equipType, EquipFumoDTOs.FumoOneResp one) {
        Msgequip.PB_SCEquipFuMoOneInfo.Builder b = Msgequip.PB_SCEquipFuMoOneInfo.newBuilder()
                .setEquipType(Math.max(equipType, 0));
        if (one != null && one.fumoData() != null) {
            b.setEquipType(Math.max(one.equipType(), 0));
            b.setFumoData(toProto(one.fumoData()));
        }
        Emitters.emit(session, MSG_SC_FUMO_ONE, b.build().toByteArray());
    }

    private void sendBagList(PlayerSession session, List<BagSlot> slots) {
        Msgequip.PB_SCEquipBagListInfo.Builder b = Msgequip.PB_SCEquipBagListInfo.newBuilder();
        for (BagSlot slot : slots) {
            b.addBagList(toProtoBag(slot));
        }
        Emitters.emit(session, MSG_SC_BAG_LIST, b.build().toByteArray());
    }

    private void sendBagOneEmpty(PlayerSession session, int index, int equipType) {
        Msgequip.PB_EquipData equipData = Msgequip.PB_EquipData.newBuilder()
                .setEquipType(Math.max(equipType, 0))
                .setItemId(0)
                .build();
        Msgequip.PB_EquipBagData bagData = Msgequip.PB_EquipBagData.newBuilder()
                .setIndex(Math.max(index, 0))
                .setBagData(equipData)
                .build();
        Emitters.emit(
                session,
                MSG_SC_BAG_ONE,
                Msgequip.PB_SCEquipBagOneInfo.newBuilder().setBagData(bagData).build().toByteArray()
        );
    }

    private void syncEquipAfterMutation(PlayerSession session, Long roleId, Integer itemIdHint, Integer equipTypeHint) {
        try {
            EquipDTOs.ListResp resp = equipHttpClient.list(String.valueOf(roleId));
            Msgequip.PB_SCEquipListInfo.Builder listBuilder = Msgequip.PB_SCEquipListInfo.newBuilder();

            EquipDTOs.EquipItem target = null;
            if (resp != null && resp.getItems() != null) {
                for (EquipDTOs.EquipItem item : resp.getItems()) {
                    listBuilder.addEquipList(toProto(item));
                    if (itemIdHint != null && item.getItemId() == itemIdHint) {
                        target = item;
                    }
                    if (target == null && equipTypeHint != null && item.getEquipType() == equipTypeHint) {
                        target = item;
                    }
                }
            }

            Emitters.emit(session, MSG_SC_EQUIP_LIST, listBuilder.build().toByteArray());
            if (target != null) {
                Emitters.emit(
                        session,
                        MSG_SC_EQUIP_ONE,
                        Msgequip.PB_SCEquipOneInfo.newBuilder().setEquipData(toProto(target)).build().toByteArray()
                );
            } else if (equipTypeHint != null && equipTypeHint > 0) {
                Emitters.emit(
                        session,
                        MSG_SC_EQUIP_ONE,
                        Msgequip.PB_SCEquipOneInfo.newBuilder()
                                .setEquipData(Msgequip.PB_EquipData.newBuilder().setEquipType(equipTypeHint).setItemId(0).build())
                                .build()
                                .toByteArray()
                );
            }
        } catch (Exception e) {
            log.error("[Equip] syncEquipAfterMutation failed roleId={}", roleId, e);
            Emitters.emit(session, MSG_SC_EQUIP_LIST, Msgequip.PB_SCEquipListInfo.newBuilder().build().toByteArray());
        }
    }

    private Msgequip.PB_EquipFuMoData toProto(EquipFumoDTOs.FumoData fd) {
        return Msgequip.PB_EquipFuMoData.newBuilder()
                .setLevel(Math.max(fd.level(), 0))
                .setExp(Math.max(fd.exp(), 0))
                .setEndTime((int) Math.max(fd.endTimeEpochSec(), 0L))
                .build();
    }

    private Msgequip.PB_EquipData toProto(EquipDTOs.EquipItem item) {
        return Msgequip.PB_EquipData.newBuilder()
                .setEquipType(item.getEquipType())
                .setItemId(item.getItemId())
                .setHp(item.getHp())
                .setAttack(item.getAttack())
                .setDefend(item.getDefend())
                .setSpeed(item.getSpeed())
                .setAttrType1(item.getAttrType1())
                .setAttrValue1(item.getAttrValue1())
                .setAttrType2(item.getAttrType2())
                .setAttrValue2(item.getAttrValue2())
                .build();
    }

    private Msgequip.PB_EquipBagData toProtoBag(BagSlot slot) {
        Msgequip.PB_EquipData data = Msgequip.PB_EquipData.newBuilder()
                .setEquipType(Math.max(slot.equipType(), 0))
                .setItemId(Math.max(slot.itemId(), 0))
                .build();
        return Msgequip.PB_EquipBagData.newBuilder()
                .setIndex(Math.max(slot.index(), 0))
                .setBagData(data)
                .build();
    }

    private List<BagSlot> fetchBagSlots(Long roleId) {
        long now = System.currentTimeMillis();
        CachedBagSlots cached = bagSlotCache.get(roleId);
        if (cached != null && (now - cached.cachedAtMs()) <= BAG_SLOT_CACHE_TTL_MS) {
            return cached.slots();
        }

        try {
            Map<String, Object> resp = equipHttpClient.wearableItems(String.valueOf(roleId));
            Object rawItems = resp != null ? resp.get("items") : null;
            if (!(rawItems instanceof List<?> list) || list.isEmpty()) {
                bagSlotCache.put(roleId, new CachedBagSlots(List.of(), now));
                return List.of();
            }

            List<BagSlot> parsed = new ArrayList<>();
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> m)) {
                    continue;
                }
                int itemId = asInt(m.get("itemId"), asInt(m.get("item_id"), 0));
                int equipType = asInt(m.get("equipType"), asInt(m.get("equip_type"), 0));
                int quality = asInt(m.get("quality"), asInt(m.get("color"), asInt(m.get("q"), 1)));
                int num = asInt(m.get("num"), 1);
                if (itemId <= 0) {
                    continue;
                }

                // Equip bag protobuf has no quantity field, so expand stacked entries into logical slots.
                int copies = Math.max(num, 1);
                for (int i = 0; i < copies; i++) {
                    parsed.add(new BagSlot(parsed.size(), itemId, equipType, quality));
                }
            }

            parsed.sort(Comparator
                    .comparingInt(BagSlot::equipType)
                    .thenComparingInt(BagSlot::itemId)
                    .thenComparing(Comparator.comparingInt(BagSlot::quality).reversed())
                    .thenComparingInt(BagSlot::index));
            List<BagSlot> indexed = new ArrayList<>(parsed.size());
            for (int i = 0; i < parsed.size(); i++) {
                BagSlot s = parsed.get(i);
                indexed.add(new BagSlot(i, s.itemId(), s.equipType(), s.quality()));
            }
            List<BagSlot> frozen = List.copyOf(indexed);
            bagSlotCache.put(roleId, new CachedBagSlots(frozen, now));
            return frozen;
        } catch (Exception e) {
            log.error("[Equip] fetchBagSlots failed roleId={}", roleId, e);
            CachedBagSlots stale = bagSlotCache.get(roleId);
            if (stale != null) {
                return stale.slots();
            }
            return List.of();
        }
    }

    private void invalidateBagSlotsCache(Long roleId) {
        if (roleId != null) {
            bagSlotCache.remove(roleId);
        }
    }

    private BagSlot findBagSlot(List<BagSlot> slots, int indexOrItemId) {
        if (indexOrItemId < 0) {
            return null;
        }
        for (BagSlot slot : slots) {
            if (slot.index() == indexOrItemId) {
                return slot;
            }
        }
        for (BagSlot slot : slots) {
            if (slot.itemId() == indexOrItemId) {
                return slot;
            }
        }
        return null;
    }

    private int asInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private record CachedBagSlots(List<BagSlot> slots, long cachedAtMs) {}
    private record BagSlot(int index, int itemId, int equipType, int quality) {}
}
