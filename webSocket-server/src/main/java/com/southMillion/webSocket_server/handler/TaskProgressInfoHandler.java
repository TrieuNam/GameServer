package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.TaskServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.task.TaskProgressDto;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class TaskProgressInfoHandler implements GameMessageHandler {
    @Autowired
    private TaskServiceFeignClient taskService;

    @Autowired
    private WebsocketEventProducer eventProducer; // Thêm vào

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Long userId = (Long) session.getAttributes().get("userId");

            // Lấy toàn bộ tiến độ hoặc từng taskId tuỳ request
            List<TaskProgressDto> list = taskService.getTaskProgress(userId);

            for (TaskProgressDto dto : list) {
                Msgrole.PB_SCTaskProgressInfo resp = Msgrole.PB_SCTaskProgressInfo.newBuilder()
                        .setId(dto.getTaskDefId().intValue())
                        .setProgress(dto.getProgress())
                        .build();
                sendResponse(session, 1452, resp.toByteArray());
            }

            // Gửi event Kafka cho task-service & report-service
            eventProducer.sendGameEvent(
                    String.valueOf(userId),
                    "TASK_PROGRESS_INFO",
                    list
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override public int getMsgId() { return 1452; }
}
