package com.southMillion.webSocket_server.handler.user;

import com.google.protobuf.InvalidProtocolBufferException;
import com.southMillion.webSocket_server.handler.GameMessageHandler;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class CmdToClientHandler implements GameMessageHandler {
    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        Msgother.PB_SCCmdToClientCmd cmd = null;
        try {
            cmd = Msgother.PB_SCCmdToClientCmd.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        // Thường phía server gọi, client nhận, ít khi client gửi tới server.
        // Nếu cần thì forward về client luôn.
        sendResponse(session, 1466, cmd.toByteArray());
    }

    @Override
    public int getMsgId() {
        return 1466;
    }
}