package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.EquipServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.EquipDto;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class EquipListInfoHandler implements GameMessageHandler {
    private final EquipServiceFeignClient equipServiceFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        String userId = (String) session.getAttributes().get("userId");
        List<EquipDto> list = equipServiceFeign.getAllEquip(userId);
        Msgequip.PB_SCEquipListInfo.Builder builder = Msgequip.PB_SCEquipListInfo.newBuilder();
        for (EquipDto d : list) {
            builder.addEquipList(Msgequip.PB_EquipData.newBuilder()
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

            );
        }
        sendResponse(session, 1605, builder.build().toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1605;
    }
}
