package com.SouthMillion.task_service.service.consumer;

import com.SouthMillion.task_service.service.TaskProgressService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.KafkaEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskEventConsumer {
    private final TaskProgressService taskProgressService;

    @KafkaListener(topics = "game-events", groupId = "task-service-group")
    public void onTaskEvent(KafkaEventDto event) {
        if (event == null || event.getType() == null) {
            System.err.println("[TASK-SERVICE] Event null hoặc không có type!");
            return;
        }

        switch (event.getType()) {
            case "ITEM_PURCHASED":
                handleItemPurchased(event);
                break;
            case "EQUIP_CHANGED":
                handleEquipChanged(event);
                break;
            case "SHIZHUANG_UNLOCKED":
                handleShiZhuangUnlocked(event);
                break;
            // Thêm các loại nhiệm vụ khác tại đây
            default:
                System.out.println("[TASK-SERVICE] Bỏ qua event type: " + event.getType());
        }
    }

    private void handleItemPurchased(KafkaEventDto event) {
        try {
            String userId = event.getUserId();
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            int itemId = Integer.parseInt(payload.get("itemId").toString());
            int amount = Integer.parseInt(payload.get("amount").toString());

            if (amount <= 0) {
                System.err.printf("[TASK-SERVICE] Số lượng không hợp lệ cho ITEM_PURCHASED: %d\n", amount);
                return;
            }

            taskProgressService.increaseProgress(userId, "BUY_ITEM", itemId, amount);

            System.out.printf("[TASK-SERVICE] update: user=%s mua item=%d số lượng=%d\n", userId, itemId, amount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEquipChanged(KafkaEventDto event) {
        try {
            String userId = event.getUserId();
            // Payload có thể là Map hoặc EquipChangedPayload, ở đây giả sử Map cho dễ
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();

            // Ví dụ payload có equipType, itemId, level v.v.
            int equipType = Integer.parseInt(payload.get("equipType").toString());
            int itemId = Integer.parseInt(payload.get("itemId").toString());
            int level = payload.get("level") != null ? Integer.parseInt(payload.get("level").toString()) : 1;

            // Validate cơ bản
            if (equipType <= 0 || itemId <= 0) {
                System.err.println("[TASK-SERVICE] EQUIP_CHANGED dữ liệu không hợp lệ: " + payload);
                return;
            }

            // Cập nhật tiến trình nhiệm vụ "Trang bị mới", hoặc "Nâng cấp trang bị"
            // Có thể chia taskCode theo loại nhiệm vụ của bạn
            String taskCode = "EQUIP_TYPE_" + equipType; // Ví dụ: EQUIP_TYPE_1, EQUIP_TYPE_2...

            // Tăng tiến độ, hoặc đặt logic nâng cấp nếu cần
            taskProgressService.increaseProgress(userId, taskCode, itemId, 1);

            System.out.printf("[TASK-SERVICE] update: user=%s thay đổi equipType=%d (item=%d, level=%d)\n",
                    userId, equipType, itemId, level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleShiZhuangUnlocked(KafkaEventDto event) {
        try {
            String userId = event.getUserId();
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();

            // Ví dụ payload có shizhuangId, level
            int shizhuangId = Integer.parseInt(payload.get("shizhuangId").toString());
            int level = payload.get("level") != null ? Integer.parseInt(payload.get("level").toString()) : 1;

            if (shizhuangId <= 0) {
                System.err.println("[TASK-SERVICE] SHIZHUANG_UNLOCKED dữ liệu không hợp lệ: " + payload);
                return;
            }

            // Cập nhật tiến độ nhiệm vụ "Mở khóa thời trang"
            String taskCode = "UNLOCK_SHIZHUANG";
            taskProgressService.increaseProgress(userId, taskCode, shizhuangId, 1);

            System.out.printf("[TASK-SERVICE] update: user=%s mở khóa thời trang id=%d, level=%d\n",
                    userId, shizhuangId, level);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
