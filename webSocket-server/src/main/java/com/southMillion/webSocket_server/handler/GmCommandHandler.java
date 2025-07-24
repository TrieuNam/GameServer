package com.southMillion.webSocket_server.handler;

import com.google.protobuf.ByteString;
import com.southMillion.webSocket_server.service.GmCommandService;
import org.SouthMillion.proto.Msggm.Msggm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;

@Component
public class GmCommandHandler implements GameMessageHandler {

    // Ví dụ: inject service xử lý GM command, hoặc event producer, tùy thực tế
    @Autowired
    private GmCommandService gmCommandService;

    // Có thể inject thêm eventProducer nếu muốn gửi sự kiện Kafka

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            // Parse protobuf từ client gửi lên
            Msggm.PB_CSGMCommand reqPb = Msggm.PB_CSGMCommand.parseFrom(payload);

            String type = reqPb.getType().toStringUtf8();
            String command = reqPb.getCommand().toStringUtf8();

            // Lấy thông tin user từ session nếu cần
            String userKey = (String) session.getAttributes().get("userKey");

            // Xử lý logic GM command qua service
            String result = gmCommandService.executeCommand(type, command, userKey);

            // Có thể gửi event qua Kafka, ví dụ:
            // eventProducer.sendGameEvent(userKey, "GM_COMMAND", command);

            // Trả về kết quả cho client (nếu client cần response)
            Msggm.PB_SCGMCommand respPb = Msggm.PB_SCGMCommand.newBuilder()
                    .setType(reqPb.getType())
                    .setResult(ByteString.copyFromUtf8(result))
                    .build();

            // Đóng gói thành binary: 4 byte msgId + protobuf
            byte[] respBody = respPb.toByteArray();
            ByteBuffer buffer = ByteBuffer.allocate(4 + respBody.length);
            buffer.putInt(2000);
            buffer.put(respBody);
            buffer.flip();

            session.sendMessage(new BinaryMessage(buffer));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Nếu bạn có registry handler qua getMsgId() thì thêm hàm này:
    public int getMsgId() {
        return 2001;
    }
}