package com.SouthMillion.webSocket_server.handler.equip;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.net.MsgIds;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.time.Duration;

@Slf4j
@Component
public class EnchantHandler implements MessageHandler {
    @Override
    public int[] interests() { return new int[]{1640}; }

    @Override
    public Mono<Void> handle(PlayerSession ps, int msgId, byte[] payload) {
        return Mono.fromCallable(() -> {
            Msgequip.PB_CSEnChantReq req = Msgequip.PB_CSEnChantReq.parseFrom(payload);
            // TODO: Call enchantFeign.enchant and build response
            Msgequip.PB_SCEnChantInfo proto = Msgequip.PB_SCEnChantInfo.newBuilder()
                .setEquipUid(req.getEquipUid())
                .setResultCode(0)
                .build();
            Emitters.emit(ps, MsgIds.SC_ENCHANT_INFO, proto);
            return null;
        })
        .timeout(Duration.ofSeconds(8))
        .onErrorResume(ex -> { log.warn("[Enchant] error: {}", ex.getMessage()); return Mono.empty(); })
        .then();
    }

    public Mono<Void> pushAll(PlayerSession ps) {
        // TODO: Fetch and push all enchant info for equipped items
        return Mono.empty();
    }
}
