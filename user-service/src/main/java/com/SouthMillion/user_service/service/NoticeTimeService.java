package com.SouthMillion.user_service.service;

import com.SouthMillion.user_service.model.NoticeTimeEntity;
import com.SouthMillion.user_service.model.NoticeTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NoticeTimeService {
    @Autowired
    private NoticeTimeRepository repo;

    @Autowired
    private NoticeTimeRedisService redisService;

    public Long getNoticeTime(Long userId) {
        // 1. Check Redis cache trước
        Long time = redisService.getNoticeTime(userId);
        if (time != null) return time;

        // 2. Nếu cache miss, lấy từ DB
        Optional<NoticeTimeEntity> entity = repo.findById(userId);
        if (entity.isPresent()) {
            redisService.setNoticeTime(userId, entity.get().getNoticeTime());
            return entity.get().getNoticeTime();
        }
        return 0L;
    }

    public Long setNoticeTime(Long userId, Long noticeTime) {
        // Update DB
        NoticeTimeEntity entity = new NoticeTimeEntity();
        entity.setUserId(userId);
        entity.setNoticeTime(noticeTime);
        repo.save(entity);

        // Update cache
        redisService.setNoticeTime(userId, noticeTime);
        return noticeTime;
    }
}
