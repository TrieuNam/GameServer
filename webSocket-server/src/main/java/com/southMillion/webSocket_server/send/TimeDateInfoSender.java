package com.southMillion.webSocket_server.send;

import org.SouthMillion.proto.Msgserver.Msgserver;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class TimeDateInfoSender {
    public void send(WebSocketSession session) {
        LocalDateTime now = LocalDateTime.now();
        Msgserver.PB_SCTimeDateInfo info = Msgserver.PB_SCTimeDateInfo.newBuilder()
                .setTime((int) (System.currentTimeMillis() / 1000L))
                .setYear(now.getYear())
                .setMon(now.getMonthValue())
                .setDay(now.getDayOfMonth())
                .setHour(now.getHour())
                .setMinute(now.getMinute())
                .setSecond(now.getSecond())
                .build();
        sendResponse(session, 9002, info.toByteArray());
    }
}
