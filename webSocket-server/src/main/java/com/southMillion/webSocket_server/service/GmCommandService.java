package com.southMillion.webSocket_server.service;

import com.southMillion.webSocket_server.service.client.ItemServiceFeignClient;
import com.southMillion.webSocket_server.service.client.TaskServiceFeignClient;
import com.southMillion.webSocket_server.service.client.UserServiceFeignClient;
import com.southMillion.webSocket_server.service.producer.WebsocketEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GmCommandService {
    @Autowired private UserServiceFeignClient userServiceFeignClient;
    @Autowired
    private ItemServiceFeignClient itemServiceFeignClient;
    @Autowired private TaskServiceFeignClient taskServiceFeignClient;
    @Autowired private WebsocketEventProducer kafkaEventProducer;

    public String executeCommand(String type, String command, String userKey) {
        try {
            switch (type) {
                case "user":
                    return userCommand(command, userKey);
                case "item":
                    return itemCommand(command, userKey);
                case "task":
                    return taskCommand(command, userKey);
                case "global":
                    return globalCommand(command, userKey);
                default:
                    return "Lệnh GM không hợp lệ!";
            }
        } catch (Exception e) {
            return "Lỗi GM: " + e.getMessage();
        }
    }

    private String userCommand(String command, String userKey) {
        // ví dụ: ban:12345, unban:12345, reset:12345
        if (command.startsWith("ban:")) {
            String targetUser = command.substring(4);
            userServiceFeignClient.banUser(targetUser);
            return "Đã ban user " + targetUser;
        } else if (command.startsWith("unban:")) {
            String targetUser = command.substring(6);
            userServiceFeignClient.unbanUser(targetUser);
            return "Đã unban user " + targetUser;
        } else if (command.startsWith("reset:")) {
            String targetUser = command.substring(6);
            userServiceFeignClient.resetUser(targetUser);
            return "Đã reset user " + targetUser;
        }
        return "Cú pháp user GM không hợp lệ";
    }

    private String itemCommand(String command, String userKey) {
        // ví dụ: add:12345,100,10 (user, itemId, số lượng)
        if (command.startsWith("add:")) {
            String[] parts = command.substring(4).split(",");
            String targetUser = parts[0];
            int itemId = Integer.parseInt(parts[1]);
            int count = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
            itemServiceFeignClient.addItem(targetUser, itemId, count);
            return "Đã cộng " + count + " item " + itemId + " cho user " + targetUser;
        } else if (command.startsWith("remove:")) {
            String[] parts = command.substring(7).split(",");
            String targetUser = parts[0];
            int itemId = Integer.parseInt(parts[1]);
            int count = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
            itemServiceFeignClient.removeItem(targetUser, itemId, count);
            return "Đã trừ " + count + " item " + itemId + " cho user " + targetUser;
        }
        return "Cú pháp item GM không hợp lệ";
    }

    private String taskCommand(String command, String userKey) {
        // ví dụ: finish:12345,567 (user, taskId)
        if (command.startsWith("finish:")) {
            String[] parts = command.substring(7).split(",");
            String targetUser = parts[0];
            int taskId = Integer.parseInt(parts[1]);
            taskServiceFeignClient.finishTask(targetUser, taskId);
            return "Đã hoàn thành task " + taskId + " cho user " + targetUser;
        }
        return "Cú pháp task GM không hợp lệ";
    }

    private String globalCommand(String command, String userKey) {
        // ví dụ: kickall, maintenance:on/off
        if ("kickall".equals(command)) {
            kafkaEventProducer.sendGlobalEvent("GM_KICK_ALL", null);
            return "Đã gửi lệnh kick all user!";
        } else if ("maintenance:on".equals(command)) {
            kafkaEventProducer.sendGlobalEvent("GM_MAINTENANCE_ON", null);
            return "Đã bật chế độ bảo trì!";
        } else if ("maintenance:off".equals(command)) {
            kafkaEventProducer.sendGlobalEvent("GM_MAINTENANCE_OFF", null);
            return "Đã tắt chế độ bảo trì!";
        }
        return "Cú pháp global GM không hợp lệ";
    }
}
