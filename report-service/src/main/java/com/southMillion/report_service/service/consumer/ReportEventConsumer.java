package com.southMillion.report_service.service.consumer;

import com.southMillion.report_service.service.ReportLogService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.KafkaEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportEventConsumer {
    private final ReportLogService reportLogService;
    // private final AlertService alertService; // Nếu bạn có service cảnh báo nội bộ
    // private final AnalyticsService analyticsService; // Nếu muốn gửi analytics

    @KafkaListener(topics = "game-events", groupId = "report-service-group")
    public void onReportEvent(KafkaEventDto event) {
        try {
            if (event == null || event.getType() == null) {
                System.err.println("[REPORT-SERVICE] Nhận event null hoặc thiếu type!");
                return;
            }

            // Lưu toàn bộ event vào database
            reportLogService.save(event);

            switch (event.getType()) {
                case "ITEM_PURCHASED":
                    logItemPurchased(event);
                    break;
                case "ITEM_NOT_ENOUGH":
                    alertNotEnough(event);
                    break;
                case "USER_LOGIN":
                    logLogin(event);
                    break;
                // Thêm các loại event khác tại đây
                default:
                    // Log mặc định
                    System.out.println("[REPORT-SERVICE] Event nhận: " + event);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logItemPurchased(KafkaEventDto event) {
        try {
            String userId = event.getUserId();
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            int itemId = Integer.parseInt(payload.get("itemId").toString());
            int amount = Integer.parseInt(payload.get("amount").toString());

            System.out.printf("[REPORT-SERVICE] User %s mua vật phẩm itemId=%d, số lượng=%d\n",
                    userId, itemId, amount);

            // Nếu tích hợp analyticsService:
            // analyticsService.sendItemPurchase(userId, itemId, amount, event.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void alertNotEnough(KafkaEventDto event) {
        try {
            String userId = event.getUserId();
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            int itemId = Integer.parseInt(payload.get("itemId").toString());
            int needAmount = Integer.parseInt(payload.get("needAmount").toString());

            System.out.printf("[REPORT-SERVICE][ALERT] User %s không đủ vật phẩm itemId=%d, cần=%d\n",
                    userId, itemId, needAmount);

            // Nếu tích hợp alertService gửi cảnh báo nội bộ (Slack, Email...):
            // alertService.sendAlert("User " + userId + " không đủ vật phẩm " + itemId + " cần " + needAmount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logLogin(KafkaEventDto event) {
        try {
            String userId = event.getUserId();
            Map<String, Object> payload = (Map<String, Object>) event.getPayload();
            String loginIp = payload.get("ip") != null ? payload.get("ip").toString() : "unknown";
            long time = event.getTimestamp();

            System.out.printf("[REPORT-SERVICE] User %s login từ IP %s tại %d\n", userId, loginIp, time);

            // Nếu muốn lưu vào DB hoặc gửi đi analytics thì gọi thêm ở đây
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}