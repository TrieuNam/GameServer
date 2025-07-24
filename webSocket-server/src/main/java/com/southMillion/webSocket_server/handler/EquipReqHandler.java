package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.EquipServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.EquipDto;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;

@Component
@RequiredArgsConstructor
public class EquipReqHandler implements GameMessageHandler {
    private final EquipServiceFeignClient equipServiceFeign;

    private final WebsocketEventProducer eventProducer; // Thêm dòng này!

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.parseFrom(payload);
            String userId = (String) session.getAttributes().get("userId");
            int equipType = req.getParam1();
            EquipDto equip = equipServiceFeign.getEquip(userId, equipType);

            Msgequip.PB_SCEquipOneInfo resp = Msgequip.PB_SCEquipOneInfo.newBuilder()
                    .setEquipData(Msgequip.PB_EquipData.newBuilder()
                            .setEquipType(equip.getEquipType())
                            .setItemId(equip.getItemId())
                            .setHp(equip.getHp())
                            .setAttack(equip.getAttack())
                            .setDefend(equip.getDefend())
                            .setSpeed(equip.getSpeed())
                            .setAttrType1(equip.getAttrType1())
                            .setAttrValue1(equip.getAttrValue1())
                            .setAttrType2(equip.getAttrType2())
                            .setAttrValue2(equip.getAttrValue2())

                    )
                    .build();
            sendResponse(session, 1606, resp.toByteArray());

            eventProducer.sendGameEvent(userId, "EQUIP_REQ", equip);
        } catch (Exception e) { e.printStackTrace(); }
    }
    @Override public int getMsgId() { return 1600; }

}