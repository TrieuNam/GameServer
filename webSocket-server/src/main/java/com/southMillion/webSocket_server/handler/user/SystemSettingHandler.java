package com.southMillion.webSocket_server.handler.user;

import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.user.UserGameFeignClient;
import com.southMillion.webSocket_server.utils.SessionUtils;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class SystemSettingHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userService;

    @Override
    public int getMsgId() {
        return 1460;
    }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgrole.PB_CSRoleSystemSetReq req;
        try {
            req = Msgrole.PB_CSRoleSystemSetReq.parseFrom(payload);
        } catch (Exception e) {
            return;
        }
        int roleId = SessionUtils.getRoleId(session);
        List<Msgrole.PB_system_set> settingList = req.getSystemSetListList();
        userService.saveSystemSetting(roleId, settingList);

        // Trả lại info
        Msgrole.PB_SCRoleSystemSetInfo resp = Msgrole.PB_SCRoleSystemSetInfo.newBuilder()
                .addAllSystemSetList(settingList)
                .build();
        sendResponse(session, 1461, resp.toByteArray());
    }
}
