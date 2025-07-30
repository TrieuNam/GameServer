package com.SouthMillion.item_service.service;

import org.SouthMillion.dto.item.Box.BoxSetDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class BoxAutoOpenService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "box:auto:";

    // Lưu setting auto open
    public void saveBoxSetting(Long userId, BoxSetDTO setting) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, setting);
    }

    // Lấy setting auto open
    public BoxSetDTO getBoxSetting(Long userId) {
        Object obj = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (obj instanceof BoxSetDTO) {
            return (BoxSetDTO) obj;
        }
        return null; // hoặc trả default nếu muốn
    }
}
