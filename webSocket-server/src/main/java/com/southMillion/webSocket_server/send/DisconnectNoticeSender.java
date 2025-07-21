package com.southMillion.webSocket_server.send;

import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class DisconnectNoticeSender {
    public void send(WebSocketSession session, int reason, int roleId, String userName) {
        try {
            Msgserver.PB_SCDisconnectNotice notice = Msgserver.PB_SCDisconnectNotice.newBuilder()
                    .setReason(reason)
                    .setRoleId(roleId)
                    .setUserName(userName != null ? userName : "")
                    .build();
            int msgId = 9001;
            byte[] payload = notice.toByteArray();
            int length = 4 + 4 + payload.length;
            ByteBuffer buffer = ByteBuffer.allocate(length).order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(length);
            buffer.putInt(msgId);
            buffer.put(payload);
            buffer.flip();
            session.sendMessage(new org.springframework.web.socket.BinaryMessage(buffer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}