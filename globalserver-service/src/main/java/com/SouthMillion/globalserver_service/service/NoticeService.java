package com.SouthMillion.globalserver_service.service;

import com.SouthMillion.globalserver_service.entity.NoticeUser;
import com.SouthMillion.globalserver_service.repository.NoticeRepository;
import com.SouthMillion.globalserver_service.repository.NoticeUserRepository;
import com.SouthMillion.globalserver_service.service.cache.NoticeRedisService;
import org.SouthMillion.dto.globalserver.NoticeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NoticeService {
    @Autowired
    private NoticeRepository noticeRepo;
    @Autowired
    private NoticeUserRepository noticeUserRepo;
    @Autowired
    private NoticeRedisService noticeRedisService;

    @Autowired private StringRedisTemplate redis;
    @Autowired private KafkaTemplate<String, NoticeEvent> kafka;

    private static final String NOTICE_TIME_KEY = "notice:time";

    public long getLatest() {
        String val = redis.opsForValue().get(NOTICE_TIME_KEY);
        return val != null ? Long.parseLong(val) : 0;
    }

    public void setNoticeTime(long time) {
        redis.opsForValue().set(NOTICE_TIME_KEY, String.valueOf(time));
        kafka.send("notice-event", new NoticeEvent(time));
    }

    // Lấy số lượng notice chưa đọc của user
    public int getUnreadNoticeNum(String userKey) {
        Integer cached = noticeRedisService.getUnreadNum(userKey);
        if (cached != null) return cached;

        int count = noticeUserRepo.countUnread(userKey);
        noticeRedisService.setUnreadNum(userKey, count);
        return count;
    }

    // Khi có notice mới gửi đến user
    public void pushNoticeToUser(String userKey, Long noticeId) {
        NoticeUser nu = new NoticeUser();
        nu.setUserKey(userKey);
        nu.setNoticeId(noticeId);
        nu.setIsRead(false);
        nu.setReadTime(null);
        noticeUserRepo.save(nu);
        // Update cache
        int unread = getUnreadNoticeNum(userKey);
        noticeRedisService.setUnreadNum(userKey, unread + 1);
    }

    // Khi user đọc notice
    public void readNotice(String userKey, Long noticeId) {
        NoticeUser nu = noticeUserRepo.findByUserKeyAndNoticeId(userKey, noticeId);
        if (nu != null && !nu.getIsRead()) {
            nu.setIsRead(true);
            nu.setReadTime(LocalDateTime.now());
            noticeUserRepo.save(nu);
            int unread = getUnreadNoticeNum(userKey);
            noticeRedisService.setUnreadNum(userKey, Math.max(0, unread - 1));
        }
    }
}
