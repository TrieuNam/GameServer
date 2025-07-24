package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.EquipServiceFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.EquipDto;
import org.SouthMillion.proto.Msgequip.Msgequip;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
@RequiredArgsConstructor
public class EquipOneInfoHandler implements GameMessageHandler {
    private final EquipServiceFeignClient equipServiceFeign;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            // Parse từ Protobuf, bỏ qua 4 bytes đầu là msgId
            Msgequip.PB_CSEquipReq req = Msgequip.PB_CSEquipReq.parseFrom(payload);
            int equipType = req.getParam1(); // <-- lấy equipType client gửi lên!
            String userId = (String) session.getAttributes().get("userId");

            EquipDto d = equipServiceFeign.getEquip(userId, equipType);

            Msgequip.PB_SCEquipOneInfo resp = Msgequip.PB_SCEquipOneInfo.newBuilder()
                    .setEquipData(Msgequip.PB_EquipData.newBuilder()
                            .setEquipType(d.getEquipType())
                            .setItemId(d.getItemId())
                            .setHp(d.getHp())
                            .setAttack(d.getAttack())
                            .setDefend(d.getDefend())
                            .setSpeed(d.getSpeed())
                            .setAttrType1(d.getAttrType1())
                            .setAttrValue1(d.getAttrValue1())
                            .setAttrType2(d.getAttrType2())
                            .setAttrValue2(d.getAttrValue2()))
                    .build();
            sendResponse(session, 1606, resp.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() {
        return 1606;
    }
}
