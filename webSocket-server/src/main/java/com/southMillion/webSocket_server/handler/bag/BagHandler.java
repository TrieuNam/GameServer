package com.SouthMillion.webSocket_server.handler.bag;


import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
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

    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;

    private static final int REQ_USE  = 0;
    private static final int REQ_SELL = 1;

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
            case REQ_USE  -> handleUse(ps, roleId, p);
            case REQ_SELL -> handleSell(ps, roleId, p);
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
}

