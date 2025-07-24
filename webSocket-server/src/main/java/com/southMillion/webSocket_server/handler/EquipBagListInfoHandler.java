package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.EquipServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.EquipDto;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;


import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class EquipBagListInfoHandler implements GameMessageHandler {
    private final EquipServiceFeignClient equipServiceFeign;
    private final WebsocketEventProducer eventProducer;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        String userId = (String) session.getAttributes().get("userId");
        List<EquipDto> list = equipServiceFeign.getAllEquip(userId);
        Msgequip.PB_SCEquipBagListInfo.Builder builder = Msgequip.PB_SCEquipBagListInfo.newBuilder();
        for (EquipDto d : list) {
            builder.addBagList(Msgequip.PB_EquipBagData.newBuilder()
                    .setIndex(d.getEquipType()) // giả lập index = equipType
                    .setBagData(Msgequip.PB_EquipData.newBuilder()
                            .setEquipType(d.getEquipType())
                            .setItemId(d.getItemId())
                            .setHp(d.getHp())
                            .setAttack(d.getAttack())
                            .setDefend(d.getDefend())
                            .setSpeed(d.getSpeed())
                            .setAttrType1(d.getAttrType1())
                            .setAttrValue1(d.getAttrValue1())
                            .setAttrType2(d.getAttrType2())
                            .setAttrValue2(d.getAttrValue2())
                    ));
        }
        sendResponse(session, 1607, builder.build().toByteArray());

        // Gửi event Kafka cho task-service & report-service
        eventProducer.sendGameEvent(userId, "EQUIP_BAG_LIST", list);
    }

    @Override
    public int getMsgId() {
        return 1607;
    }
}
