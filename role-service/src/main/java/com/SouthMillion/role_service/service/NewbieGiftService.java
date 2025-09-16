package com.SouthMillion.role_service.service;


import com.SouthMillion.role_service.config.RoleConfigCache;
import com.SouthMillion.role_service.service.producer.BagEventProducer;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.event.BagGrantEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewbieGiftService {

    private final RoleConfigCache cfg;
    private final BagEventProducer producer;

    @Value("${app.newbie.bind-default:true}")
    private boolean bindDefault; // vì OtherCfg hiện chỉ có boxId/boxNum

    /** Gọi khi tạo role hoặc login lần đầu */
    public void onFirstLogin(String userId, String roleId) {
        var other = cfg.other(); // có boxId / boxNum
        if (other == null) return;
        int itemId = other.getBoxId();
        int num    = (int)Math.max(0, other.getBoxNum());
        if (itemId <= 0 || num <= 0) return;

        var item = BagGrantEvent.Item.builder()
                .itemId(itemId)
                .num(num)
                .bind(bindDefault) // cấu hình mặc định
                .expireAt(null)    // nếu cần hạn, thêm field vào OtherCfg & parse
                .build();

        producer.publishGrant(userId, roleId, List.of(item), "first-login");
    }
}