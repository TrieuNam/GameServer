package com.SouthMillion.webSocket_server.handler.bag;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.handler.role.RoleServiceHandler;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.ShiZhuangFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Xử lý PB_CSKnapsackReq (MsgId: 1500)
 * KNAPSACK_REQ_TYPE:
 *  0 = USE  (param: [itemId, num, param?])
 *  1 = SELL (param: [itemId, num, unitPrice?])
 *  2 = SHI_ZHUANG_LEVEL_UP  -> route sang handler khác
 *  3 = SHI_ZHUANG_USE       -> route sang handler khác
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BagHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BagHandler.class);

    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;
    private final ShiZhuangFeign shiZhuangFeign;
    private final RoleServiceHandler roleServiceHandler;
    private final ActivityFeign activityFeign;
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final int REQ_USE                  = 0;
    private static final int REQ_SELL                 = 1;
    private static final int REQ_SHI_ZHUANG_LEVEL_UP = 2;
    private static final int REQ_SHI_ZHUANG_USE      = 3;

    @Override
    public int[] interests() { return new int[]{ MsgIds.CS_KNAPSACK_REQ, MsgIds.CS_BUY_CMD_REQ }; }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (msgId == MsgIds.CS_BUY_CMD_REQ) {
            return handleBuyCmd(ps, payload);
        }
        if (msgId != MsgIds.CS_KNAPSACK_REQ) return Mono.empty();

        final Msgknapsack.PB_CSKnapsackReq req;
        try {
            req = Msgknapsack.PB_CSKnapsackReq.parseFrom(payload);
        } catch (Exception e) {
            log.warn("[bag] parse 1500 error: {}", e.toString());
            return Mono.empty();
        }

        Long roleId = ps.getRoleId();
        if (roleId == null) {
            log.warn("[bag] missing roleId");
            return Mono.empty();
        }

        int type = req.getReqType();
        List<Integer> p = req.getParamList();

        return switch (type) {
            case REQ_USE                  -> handleUse(ps, roleId, p);
            case REQ_SELL                 -> handleSell(ps, roleId, p);
            case REQ_SHI_ZHUANG_LEVEL_UP -> handleShiZhuangLevelUp(ps, roleId, p);
            case REQ_SHI_ZHUANG_USE      -> handleShiZhuangUse(ps, roleId, p);
            default -> { log.info("[bag] unsupported req_type={}", type); yield Mono.empty(); }
        };
    }

    /** Gọi sau login nếu muốn đẩy toàn bộ túi. */
    public Mono<Void> pushAll(PlayerSession ps) {
        Long roleId = ps.getRoleId();
        if (roleId == null) return Mono.empty();
        return FeignCall.withToken(ps.getSessionId(), "bag.list",
                        () -> bagFeign.list(String.valueOf(roleId)))
                .doOnNext(list -> Emitters.sendKnapsackAllInfo(ps, list))
                .then();
    }

    private Mono<Void> handleUse(PlayerSession ps, Long roleId, List<Integer> p) {
        if (p.size() < 2) return Mono.empty();
        int itemId = safe(p, 0);
        int num    = safe(p, 1);
        Integer param = p.size() >= 3 ? p.get(2) : 0;

        var body = BagDTOs.UseItemReq.builder()
                .itemId(itemId).num(num).param(param).build();

        return FeignCall.withToken(ps.getSessionId(), "bag.use",
                        () -> { bagFeign.use(String.valueOf(roleId), body); return null; })
                .onErrorResume(ex -> onNotEnough(ps, ex, itemId))
                .then(FeignCall.withToken(ps.getSessionId(), "bag.list",
                        () -> bagFeign.list(String.valueOf(roleId))))
                .doOnNext(list -> Emitters.sendKnapsackSingleInfo(ps, itemId, currentNum(list, itemId)))
                .then();
    }

    private Mono<Void> handleSell(PlayerSession ps, Long roleId, List<Integer> p) {
        if (p.size() < 2) return Mono.empty();
        int itemId     = safe(p, 0);
        int num        = safe(p, 1);
        long unitPrice = (p.size() >= 3) ? p.get(2).longValue() : 0L;

        var body = BagDTOs.SellItemReq.builder()
                .itemId(itemId).num(num).unitPrice(unitPrice).build();

        return FeignCall.withToken(ps.getSessionId(), "bag.sell",
                        () -> bagFeign.sell(String.valueOf(roleId), body))
                .onErrorResume(ex -> onNotEnough(ps, ex, itemId).then(Mono.empty()))
                .then(FeignCall.withToken(ps.getSessionId(), "bag.list",
                        () -> bagFeign.list(String.valueOf(roleId))))
                .doOnNext(list -> Emitters.sendKnapsackSingleInfo(ps, itemId, currentNum(list, itemId)))
                .then(Mono.fromRunnable(() -> pushWalletBalance(ps, roleId)));
    }

    private Mono<Void> handleShiZhuangLevelUp(PlayerSession ps, Long roleId, List<Integer> p) {
        if (p.isEmpty()) return Mono.empty();
        int clothesId = safe(p, 0);
        int consumeMode = p.size() >= 2 ? safe(p, 1) : 0;
        if (clothesId <= 0) return Mono.empty();

        return FeignCall.withToken(ps.getSessionId(), "shizhuang.levelup",
                        () -> {
                            shiZhuangFeign.levelUpFashion(String.valueOf(roleId), clothesId, consumeMode);
                            return null;
                        })
                .doOnSuccess(ignored -> log.debug("[bag] shizhuang levelUp clothesId={}, consumeMode={}, roleId={}", clothesId, consumeMode, roleId))
                .then(refreshShiZhuangState(ps, roleId, clothesId))
                .onErrorResume(ex -> {
                    log.warn("[bag] shizhuang levelUp failed roleId={}, clothesId={}: {}", roleId, clothesId, ex.getMessage());
                    sendFashionNotEnoughNotice(ps, ex);
                    pushWalletBalance(ps, roleId);
                    return refreshShiZhuangState(ps, roleId, clothesId);
                });
    }

    private Mono<Void> handleShiZhuangUse(PlayerSession ps, Long roleId, List<Integer> p) {
        if (p.isEmpty()) return Mono.empty();
        int clothesId = safe(p, 0);
        if (clothesId <= 0) return Mono.empty();

        return FeignCall.withToken(ps.getSessionId(), "shizhuang.appearance",
                        () -> shiZhuangFeign.getCurrentAppearance(String.valueOf(roleId)))
                .onErrorReturn(Map.of())
                .flatMap(appearance -> {
                    boolean currentlyWorn = isClothesCurrentlyWorn(appearance, clothesId);
                    String action = currentlyWorn ? "shizhuang.unwear" : "shizhuang.wear";
                    return FeignCall.withToken(ps.getSessionId(), action,
                            () -> {
                                if (currentlyWorn) {
                                    shiZhuangFeign.unwearFashion(String.valueOf(roleId), clothesId);
                                } else {
                                    shiZhuangFeign.wearFashion(String.valueOf(roleId), clothesId);
                                }
                                return null;
                            });
                })
                .then(refreshShiZhuangState(ps, roleId, clothesId))
                .onErrorResume(ex -> {
                    log.warn("[bag] shizhuang use failed roleId={}, clothesId={}: {}", roleId, clothesId, ex.getMessage());
                    sendFashionNotEnoughNotice(ps, ex);
                    return refreshShiZhuangState(ps, roleId, clothesId);
                });
    }

    private Mono<Void> refreshShiZhuangState(PlayerSession ps, Long roleId, int clothesId) {
        return FeignCall.withToken(ps.getSessionId(), "shizhuang.list",
                        () -> shiZhuangFeign.getRoleFashions(String.valueOf(roleId)))
                .doOnNext(fashions -> {
                    sendShiZhuangList(ps, fashions);
                    sendShiZhuangOne(ps, fashions, clothesId);
                })
                .then(roleServiceHandler.pushRoleState(ps)
                        .onErrorResume(ex -> {
                            log.warn("[bag] role-state refresh after shizhuang change failed roleId={}: {}", roleId, ex.getMessage());
                            return Mono.empty();
                        }))
                .onErrorResume(ex -> {
                    log.warn("[bag] shizhuang refresh failed roleId={}, clothesId={}: {}", roleId, clothesId, ex.getMessage());
                    sendShiZhuangList(ps, List.of());
                    sendShiZhuangOne(ps, List.of(), clothesId);
                    return roleServiceHandler.pushRoleState(ps)
                            .onErrorResume(roleEx -> Mono.empty());
                });
    }

    private void sendShiZhuangList(PlayerSession ps, List<Map<String, Object>> fashions) {
        var builder = Msgknapsack.PB_SCAllShiZhuangInfo.newBuilder();
        if (fashions != null) {
            for (Map<String, Object> fashion : fashions) {
                Msgknapsack.PB_ShiZhuangData data = toShiZhuangData(fashion);
                if (data != null) {
                    builder.addShizhuangList(data);
                }
            }
        }
        Emitters.emit(ps, MsgIds.SC_ALL_SHIZHUANG_INFO, builder.build().toByteArray());
    }

    private void sendShiZhuangOne(PlayerSession ps, List<Map<String, Object>> fashions, int clothesId) {
        if (clothesId <= 0) return;
        Msgknapsack.PB_ShiZhuangData data = null;
        if (fashions != null) {
            for (Map<String, Object> fashion : fashions) {
                int currentClothesId = getInt(fashion, "clothesId", 0);
                if (currentClothesId == clothesId) {
                    data = Msgknapsack.PB_ShiZhuangData.newBuilder()
                            .setId(clothesId)
                            .setLevel(Math.max(getInt(fashion, "level", 0), 0))
                            .build();
                    break;
                }
            }
        }

        if (data == null) {
            return;
        }

        Emitters.emit(ps, MsgIds.SC_SHIZHUANG_INFO,
                Msgknapsack.PB_SCShiZhuangInfo.newBuilder().setShizhuang(data).build().toByteArray());
    }

    private Msgknapsack.PB_ShiZhuangData toShiZhuangData(Map<String, Object> fashion) {
        int clothesId = getInt(fashion, "clothesId", getInt(fashion, "id", 0));
        if (clothesId <= 0) {
            return null;
        }
        return Msgknapsack.PB_ShiZhuangData.newBuilder()
                .setId(clothesId)
                .setLevel(Math.max(getInt(fashion, "level", 0), 0))
                .build();
    }

    private boolean isClothesCurrentlyWorn(Map<String, Object> appearance, int clothesId) {
        if (appearance == null || appearance.isEmpty() || clothesId <= 0) {
            return false;
        }
        return getInt(appearance, "surfaceWeapon", 0) == clothesId
                || getInt(appearance, "surfaceShield", 0) == clothesId
                || getInt(appearance, "surfaceHead", 0) == clothesId
                || getInt(appearance, "surfaceBody", 0) == clothesId;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) return defaultValue;
        Object value = map.get(key);
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Mono<Void> handleBuyCmd(PlayerSession ps, byte[] payload) {
        return Mono.fromSupplier(() -> {
            try {
                Msgknapsack.PB_CSBuyCmdReq req = Msgknapsack.PB_CSBuyCmdReq.parseFrom(payload);
                int buyType = req.hasBuyType() ? req.getBuyType() : 0;
                int itemId  = req.hasBuyParam1() ? req.getBuyParam1() : 0;
                int num     = req.hasNum() ? req.getNum() : 1;
                return BagDTOs.BuyCmdReq.builder()
                        .buyType(buyType).itemId(itemId).num(num)
                        .currency(0).price(0).build();
            } catch (Exception e) {
                log.warn("[bag] parse 1501 error: {}", e.toString());
                return null;
            }
        }).flatMap(buyCmdReq -> {
            if (buyCmdReq == null) return Mono.empty();
            Long roleId = Long.valueOf(ps.getRoleId());
            int itemId = buyCmdReq.getItemId();
            return FeignCall.withToken(ps.getSessionId(), "bag.buyCmd",
                            () -> bagFeign.buyCmd(String.valueOf(roleId), buyCmdReq))
                    .doOnNext(ignored -> notifyActivityBuy(roleId, buyCmdReq.getBuyType(), buyCmdReq.getItemId()))
                    .onErrorResume(ex -> {
                        if (itemId > 0) Emitters.sendItemNotEnoughNotice(ps, itemId);
                        return Mono.empty();
                    })
                    .then(FeignCall.withToken(ps.getSessionId(), "bag.list",
                            () -> bagFeign.list(String.valueOf(roleId))))
                    .doOnNext(list -> Emitters.sendKnapsackAllInfo(ps, list))
                    .then(Mono.fromRunnable(() -> pushWalletBalance(ps, roleId)));
        });
    }

    /**
     * After a successful BuyCmd for an activity-type purchase (TianXuanZhiLi/ShouChongDingZhi),
     * notify activity-service so it can update the buy/gift state for that activity.
     * buyType = client ACTIVITY_TYPE value (2075 or 2078).
     * param1  = gift seq for TianXuan; 0 for ShouChong.
     */
    private void notifyActivityBuy(Long roleId, int buyType, int param1) {
        int dispatchType;
        if (buyType == 2078) dispatchType = 41;       // ShouChongDingZhi
        else if (buyType == 2075) dispatchType = 38;  // TianXuanZhiLi
        else return;
        try {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("activityType", dispatchType);
            body.put("operaType", 2); // BUY op in server convention
            body.put("param1", param1);
            body.put("param2", 0);
            body.put("param3", 0);
            activityFeign.randActivity(String.valueOf(roleId), body);
        } catch (Exception e) {
            log.warn("[bag] activity bridge failed for buyType={} roleId={}: {}", buyType, roleId, e.getMessage());
        }
    }

    /**
     * After a sell operation, fetch the latest wallet balances and push each
     * currency update to the client so it can refresh its gold/diamond counters.
     */
    private void pushWalletBalance(PlayerSession ps, Long roleId) {
        try {
            WalletDTOs.BalancesResp walletResp = walletHttpClient.info(String.valueOf(roleId));
            if (walletResp != null && walletResp.balances() != null) {
                Emitters.sendWalletBalances(ps, walletResp.balances());
            }
        } catch (Exception e) {
            // Non-critical — sell succeeded; log and continue
            log.warn("[bag] Failed to push wallet balance for roleId={}: {}", roleId, e.getMessage());
        }
    }

    private Mono<Void> onNotEnough(PlayerSession ps, Throwable ex, int itemId) {
        if (ex instanceof feign.FeignException.FeignClientException fce
                && fce.status() >= 400 && fce.status() < 500) {
            Emitters.sendItemNotEnoughNotice(ps, itemId);
            return Mono.empty();
        }
        return Mono.error(ex);
    }

    private static long currentNum(List<BagDTOs.ItemView> list, int itemId) {
        if (list == null || list.isEmpty()) return 0L;
        return list.stream()
                .filter(iv -> iv.getItemId() != null && iv.getItemId() == itemId)
                .map(iv -> iv.getNum() == null ? 0L : iv.getNum())
                .mapToLong(Long::longValue)   // hoặc .mapToLong(n -> n)
                .sum();
    }

    private static int safe(List<Integer> p, int i) {
        return (i < p.size() && p.get(i) != null) ? p.get(i) : 0;
    }

    private void sendFashionNotEnoughNotice(PlayerSession ps, Throwable ex) {
        extractFashionErrorItemId(ex)
                .filter(itemId -> itemId > 0)
                .ifPresent(itemId -> Emitters.sendItemNotEnoughNotice(ps, itemId));
    }

    private Optional<Integer> extractFashionErrorItemId(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof feign.FeignException fe) {
                if (fe.status() >= 400 && fe.status() < 500) {
                    Integer fromBody = parseItemIdFromErrorBody(fe.contentUTF8());
                    if (fromBody != null && fromBody > 0) {
                        return Optional.of(fromBody);
                    }
                }
                break;
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    private Integer parseItemIdFromErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode node = JSON.readTree(body);
            JsonNode itemIdNode = node.get("itemId");
            if (itemIdNode != null && itemIdNode.canConvertToInt()) {
                return itemIdNode.intValue();
            }
            JsonNode dataNode = node.get("data");
            if (dataNode != null && dataNode.isObject()) {
                JsonNode nestedItemIdNode = dataNode.get("itemId");
                if (nestedItemIdNode != null && nestedItemIdNode.canConvertToInt()) {
                    return nestedItemIdNode.intValue();
                }
            }
        } catch (Exception ignored) {
            // Ignore malformed upstream error payloads.
        }
        return null;
    }
}

