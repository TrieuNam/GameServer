package com.southMillion.webSocket_server.handler.items;

import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.Emitters;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.service.client.BagInternalFeign;
import com.southMillion.webSocket_server.service.client.BagPublicHttpClient;
import com.southMillion.webSocket_server.service.client.GiftFeign;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgknapsack.Msgknapsack;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KnapsackHandler implements MessageHandler {
    private final BagPublicHttpClient bagPublic;
    private final BagInternalFeign bagInternal;
    private final GiftFeign giftFeign;

    private static final byte BAG_COMMON = 0;

    @Override public int[] interests() { return new int[]{ MsgIds.CS_KNAPSACK_REQ }; }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();

        Msgknapsack.PB_CSKnapsackReq req;
        try { req = Msgknapsack.PB_CSKnapsackReq.parseFrom(payload); }
        catch (Exception e) { return Mono.empty(); }

        int op = req.hasReqType() ? req.getReqType() : 0;

        return switch (op) {
            case 1 -> onUse(ps, req);   // ví dụ: USE
            default -> Mono.empty();
        };
    }

    private Mono<Void> onUse(PlayerSession ps, Msgknapsack.PB_CSKnapsackReq req) {
        String tk = ps.getSessionId();
        String rid = ps.getRoleId();

        // ví dụ param[0]=itemId, param[1]=num
        int itemId = req.getParamCount() >= 1 ? req.getParam(0) : 0;
        int num    = req.getParamCount() >= 2 ? req.getParam(1) : 1;

        // ưu tiên gift-service
        return FeignCall.withToken(tk, "gift.use", () -> giftFeign.use(rid, itemId, num, null))
                .onErrorResume(ex -> Mono.empty())
                .then( Mono.defer(() -> FeignCall.withToken(tk, "bag.get", () -> bagPublic.get(rid, BAG_COMMON)))
                        .doOnNext(bag -> {
                            Emitters.sendKnapsackAll(ps, bag);
                            Emitters.sendKnapsackSingle(ps, bag, itemId);
                        })
                        .then()
                );
    }
}