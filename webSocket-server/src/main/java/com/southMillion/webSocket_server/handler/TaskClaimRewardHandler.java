package com.southMillion.webSocket_server.handler;

import com.southMillion.webSocket_server.service.client.TaskServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.SouthMillion.dto.task.TaskClaimRequest;
import org.SouthMillion.dto.task.TaskProgressDto;
import org.SouthMillion.proto.Msgrole.Msgrole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.southMillion.webSocket_server.utils.websocketSendResponse.sendResponse;

@Component
public class TaskClaimRewardHandler implements GameMessageHandler {
    @Autowired
    private TaskServiceFeignClient taskService;

    @Autowired
    private WebsocketEventProducer eventProducer; // Thêm vào

    @Override
    public void handle(WebSocketSession session, byte[] payload) {
        try {
            Long userId = (Long) session.getAttributes().get("userId");

            // Gọi task-service: "claim reward nhiệm vụ hợp lệ đầu tiên"
            TaskClaimRequest req = new TaskClaimRequest();
            req.setUserId(userId);
            req.setTaskDefId(null); // hoặc không set

            TaskProgressDto dto = taskService.claimReward(req);

            // Trả về tiến độ của task vừa claim (PB_SCTaskProgressInfo)
            Msgrole.PB_SCTaskProgressInfo resp = Msgrole.PB_SCTaskProgressInfo.newBuilder()
                    .setId(dto.getTaskDefId().intValue())
                    .setProgress(dto.getProgress())
                    .build();

            sendResponse(session, 1452, resp.toByteArray());

            // Gửi event Kafka cho task-service & report-service
            eventProducer.sendGameEvent(
                    String.valueOf(userId),
                    "TASK_REWARD_CLAIMED",
                    dto
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override public int getMsgId() { return 1451; }
}
