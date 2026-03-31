package com.SouthMillion.webSocket_server.handler.recharge;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.ActivityFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.proto.Msgrandactivity.Msgrandactivity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Handles recharge package config request.
 *
 *   CS 3004 PB_CSChongZhiConfigReq  → SC 3005 PB_SCChongZhiConfigInfo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RechargeConfigHandler implements MessageHandler {

    private final ActivityFeign activityFeign;

    @Override
    public int[] interests() {
        return new int[]{3004};
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgrandactivity.PB_CSChongZhiConfigReq req =
                        Msgrandactivity.PB_CSChongZhiConfigReq.parseFrom(payload);
                int currency = req.hasCurrency() ? req.getCurrency() : 0;
                String spid = req.hasSpidStr() ? req.getSpidStr().toStringUtf8() : null;

                Map<String, Object> data = activityFeign.getRechargeConfig(currency, spid);

                Msgrandactivity.PB_SCChongZhiConfigInfo.Builder resp =
                        Msgrandactivity.PB_SCChongZhiConfigInfo.newBuilder();
                resp.setCurrencyType(((Number) data.getOrDefault("currencyType", currency)).intValue());
                resp.setInfoCount(((Number) data.getOrDefault("infoCount", 0)).intValue());

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> infoList =
                        (List<Map<String, Object>>) data.getOrDefault("infoList", List.of());
                for (Map<String, Object> item : infoList) {
                    Msgrandactivity.PB_ChongzhiInfo.Builder info =
                            Msgrandactivity.PB_ChongzhiInfo.newBuilder();
                    info.setSeq(((Number) item.getOrDefault("seq", 0)).intValue());
                    info.setMoneyShow(((Number) item.getOrDefault("moneyShow", 0)).intValue());
                    info.setAddGold(((Number) item.getOrDefault("addGold", 0)).intValue());
                    info.setExtraRewardType(((Number) item.getOrDefault("extraRewardType", 0)).intValue());
                    info.setExtraReward(((Number) item.getOrDefault("extraReward", 0)).intValue());
                    resp.addInfoList(info.build());
                }

                Emitters.emit(session, 3005, resp.build().toByteArray());
                log.debug("[RechargeConfig] roleId={} currency={} sent {} packages",
                        session.getRoleId(), currency, infoList.size());
            } catch (Exception e) {
                log.error("[RechargeConfig] Error for roleId={}", session.getRoleId(), e);
            }
        });
    }
}
