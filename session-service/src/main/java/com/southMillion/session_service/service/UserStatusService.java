package com.SouthMillion.session_service.service;

import com.SouthMillion.session_service.service.client.UserFeignClient;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.user.ActiveResp;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserFeignClient userFeign;

    /**
     * Cache 60s theo cấu hình CacheConfig để giảm tải gọi user-service.
     */
    @Cacheable(cacheNames = "userActive", key = "#userId", unless = "#result == null")
    public Boolean isActive(String userId) {
        ActiveResp resp = userFeign.isActive(userId);
        return resp != null && resp.isActive();
    }
}