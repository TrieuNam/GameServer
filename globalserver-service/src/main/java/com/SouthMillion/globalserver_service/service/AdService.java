package com.SouthMillion.globalserver_service.service;

import com.SouthMillion.globalserver_service.entity.AdEntity;
import com.SouthMillion.globalserver_service.repository.AdRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.globalserver.AdDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Calendar;

@Service
public class AdService {
    @Autowired
    private AdRepository repo;
    @Autowired private StringRedisTemplate redis;
    private static final String AD_KEY = "ad:info:%d";
    private static final int MAX_COUNT = 5;

    // Lấy thông tin quảng cáo user (có cache)
    public AdDTO getAdInfo(Long userId) {
        String key = String.format(AD_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parse(cache);

        AdEntity e = repo.findById(userId).orElseGet(() -> {
            AdEntity entity = new AdEntity();
            entity.setUserId(userId);
            entity.setTodayCount(0);
            entity.setNextFetchTime(0L);
            entity.setLastUpdate(System.currentTimeMillis());
            repo.save(entity);
            return entity;
        });
        AdDTO dto = toDto(e);
        redis.opsForValue().set(key, toJson(dto), Duration.ofMinutes(10));
        return dto;
    }

    // Xem quảng cáo, nhận thưởng
    public void fetchAd(Long userId) {
        AdEntity e = repo.findById(userId).orElseThrow();
        // Reset lượt mới mỗi ngày (giả sử 00:00)
        long now = System.currentTimeMillis();
        if (!sameDay(e.getLastUpdate(), now)) {
            e.setTodayCount(0);
        }
        if (e.getTodayCount() >= MAX_COUNT)
            throw new RuntimeException("Hết lượt xem hôm nay");

        if (e.getNextFetchTime() > now)
            throw new RuntimeException("Chưa đến thời gian xem tiếp");

        e.setTodayCount(e.getTodayCount() + 1);
        e.setLastUpdate(now);
        // Đặt cooldown, ví dụ 5 phút/lần
        e.setNextFetchTime(now + 5*60*1000L);
        repo.save(e);
        redis.delete(String.format(AD_KEY, userId));
        // TODO: Thưởng cho user ở đây (gọi service khác)
    }

    // Helper
    private boolean sameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(); c1.setTimeInMillis(t1);
        Calendar c2 = Calendar.getInstance(); c2.setTimeInMillis(t2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private AdDTO toDto(AdEntity e) {
        AdDTO dto = new AdDTO();
        dto.setTodayCount(e.getTodayCount());
        dto.setNextFetchTime(e.getNextFetchTime());
        dto.setCanFetch(e.getTodayCount() < MAX_COUNT && e.getNextFetchTime() <= System.currentTimeMillis());
        return dto;
    }

    private String toJson(Object o) {
        try { return new ObjectMapper().writeValueAsString(o); }
        catch (Exception ex) { return "{}"; }
    }
    private AdDTO parse(String json) {
        try { return new ObjectMapper().readValue(json, AdDTO.class); }
        catch (Exception ex) { return new AdDTO(); }
    }
}