package com.southMillion.webSocket_server.handler.shop;

import com.southMillion.webSocket_server.dto.PlayerSession;
import com.southMillion.webSocket_server.net.MessageHandler;
import com.southMillion.webSocket_server.net.MsgIds;
import com.southMillion.webSocket_server.service.client.ShopFeign;
import com.southMillion.webSocket_server.utils.FeignCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.shop.ShopDTOs;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShopHandler implements MessageHandler {
    private final ShopFeign shopFeign;

    private static final byte BAG_COMMON = 0;

    @Override public int[] interests() { return new int[]{ MsgIds.CS_SHOP_BUY_REQ }; }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        if (!StringUtils.hasText(ps.getRoleId())) return Mono.empty();

        Msgother.PB_CSShopBuyReq req;
        try { req = Msgother.PB_CSShopBuyReq.parseFrom(payload); }
        catch (Exception e) { return Mono.empty(); }

        String tk = ps.getSessionId(); String rid = ps.getRoleId();
        int index = req.hasIndex() ? req.getIndex() : 0;
        int num   = req.hasNum()   ? req.getNum()   : 1;

        // Map sang shop-service: giả định COMMON shop
        var buyReq = new ShopDTOs.BuyReq(
                ps.getRoleId(),
                ShopDTOs.Kind.COMMON,
                index,
                Math.max(1, num),
                BAG_COMMON, // nhận item vào bag thường
                BAG_COMMON  // dùng ví trong bag thường (nếu ví là item ảo)
        );

        return FeignCall.withToken(tk, "shop.buy", () -> shopFeign.buy(buyReq))
                .doOnNext(info -> {
                    // TODO: emit PB_SCShopInfo
                })
                .onErrorResume(ex -> Mono.empty())
                .then();
    }
}