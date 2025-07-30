package com.southMillion.webSocket_server.handler.user;

import com.google.protobuf.ByteString;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import com.southMillion.webSocket_server.service.client.user.UserGameFeignClient;
import com.southMillion.webSocket_server.utils.SessionUtils;
import org.SouthMillion.proto.Msggm.Msggm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class GMCommandHandler implements GameMessageHandler {
    @Autowired
    private UserGameFeignClient userService;

    @Override
    public int getMsgId() {
        return 2001;
    }

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msggm.PB_CSGMCommand req;
        try {
            req = Msggm.PB_CSGMCommand.parseFrom(payload);
        } catch (Exception e) {
            return;
        }
        int roleId = SessionUtils.getRoleId(session);
        String commandType = req.getType().toStringUtf8();
        String command = req.getCommand().toStringUtf8();
        String result = "OK"; // TODO: xử lý thật, ví dụ thêm vàng, v.v.
        userService.gmCommand(roleId, commandType, command, result);
        Msggm.PB_SCGMCommand resp = Msggm.PB_SCGMCommand.newBuilder()
                .setType(req.getType())
                .setResult(ByteString.copyFromUtf8(result))
                .build();
        sendResponse(session, 2000, resp.toByteArray());
    }
}