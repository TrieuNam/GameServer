package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.UserServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.user.RoleAttrDto;
import org.SouthMillion.dto.user.RoleAttrListEvent;
import org.SouthMillion.dto.user.RoleDto;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class RoleAttrListHandler implements GameMessageHandler {
    @Autowired
    private UserServiceFeignClient userService;

    @Autowired
    private WebsocketEventProducer eventProducer;

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            String roleId = (String) session.getAttributes().get("roleId");
            List<RoleAttrDto> attrs = userService.getRoleAttrs(roleId);
            RoleDto role = userService.getRoleInfo(roleId);

            Msgrole.PB_SCRoleAttrList.Builder builder = Msgrole.PB_SCRoleAttrList.newBuilder()
                    .setNotifyReason(1)
                    .setCapability(role.getCap() == null ? 0L : role.getCap());

            for (RoleAttrDto attr : attrs) {
                builder.addAttrList(
                        Msgrole.PB_AttrPair.newBuilder()
                                .setAttrType(attr.getAttrType())
                                .setAttrValue(attr.getAttrValue())
                );
            }
            sendResponse(session, 1401, builder.build().toByteArray());
            eventProducer.sendGameEvent(
                    roleId,
                    "ROLE_ATTR_LIST",
                    new RoleAttrListEvent(role, attrs)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getMsgId() { return 1401; }
}
