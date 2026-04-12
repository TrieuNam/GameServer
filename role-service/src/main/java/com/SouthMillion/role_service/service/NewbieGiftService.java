package com.SouthMillion.role_service.service;


import com.SouthMillion.role_service.config.RoleConfigCache;
import com.SouthMillion.role_service.service.client.BagFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewbieGiftService {

    private final RoleConfigCache cfg;
    private final BagFeign bagFeign;

    @Value("${app.newbie.bind-default:true}")
    private boolean bindDefault;

    /** Gọi khi tạo role hoặc login lần đầu */
    public void onFirstLogin(String userId, String roleId) {
        var other = cfg.other();
        if (other == null) return;
        int itemId = other.getBoxId();
        int num    = (int) Math.max(0, other.getBoxNum());
        if (itemId <= 0 || num <= 0) return;

        try {
            BagAddItemReq req = BagAddItemReq.builder()
                    .userId(0L) // userId is UUID string; bag-service uses it as audit field only
                    .roleId(Long.parseLong(roleId))
                    .items(List.of(BagAddItemReq.Item.builder()
                            .itemId(itemId)
                            .amount(num)
                            .bound(bindDefault)
                            .build()))
                    .source("first-login")
                    .idemKey("newbie-" + roleId) // idempotent: safe to retry on startup
                    .build();
            bagFeign.add(req);
            log.info("[NewbieGift] Granted boxId={} x{} to roleId={}", itemId, num, roleId);
        } catch (Exception e) {
            log.warn("[NewbieGift] Failed to grant newbie box to roleId={}: {}", roleId, e.getMessage());
        }
    }
}