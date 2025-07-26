package com.SouthMillion.globalserver_service.service;

import com.SouthMillion.globalserver_service.entity.AdvertisementEntity;
import com.SouthMillion.globalserver_service.repository.AdvertisementRepository;
import com.SouthMillion.globalserver_service.service.impl.AdvertisementService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.globalserver.AdvertisementDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Service
public class AdvertisementServiceImpl implements AdvertisementService {
    @Autowired
    private AdvertisementRepository repo;
    @Autowired
    private StringRedisTemplate redis;

    private static final String AD_KEY = "ad:info:%d";
    private static final int MAX_COUNT = 5;
    private static final int COOLDOWN = 5 * 60; // giây

    // Lấy danh sách quảng cáo user (có cache)
    @Override
    public List<AdvertisementDTO> getAdvertisementList(Long userId) {
        String key = String.format(AD_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parseList(cache);

        List<AdvertisementEntity> ads = repo.findAllByUserId(userId);
        // Nếu chưa có quảng cáo nào, khởi tạo mặc định 1 quảng cáo seq=1
        if (ads.isEmpty()) {
            AdvertisementEntity entity = new AdvertisementEntity();
            entity.setUserId(userId);
            entity.setSeq(1);
            entity.setTodayCount(0);
            entity.setNextFetchTime(0);
            repo.save(entity);
            ads = List.of(entity);
        }
        List<AdvertisementDTO> result = ads.stream()
                .map(AdvertisementEntity::fromEntity)
                .collect(Collectors.toList());
        redis.opsForValue().set(key, toJson(result), Duration.ofMinutes(10));
        return result;
    }

    @Override
    // User xem quảng cáo và nhận thưởng (theo từng seq)
    public void fetchAd(Long userId, Integer seq) {
        AdvertisementEntity e = repo.findByUserIdAndSeq(userId, seq)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy quảng cáo"));
        long now = System.currentTimeMillis() / 1000; // giây
        // Reset lượt nếu sang ngày mới
        if (!sameDay(e.getNextFetchTime(), now)) {
            e.setTodayCount(0);
        }
        if (e.getTodayCount() >= MAX_COUNT)
            throw new RuntimeException("Hết lượt xem hôm nay");
        if (e.getNextFetchTime() > now)
            throw new RuntimeException("Chưa đến thời gian xem tiếp");

        e.setTodayCount(e.getTodayCount() + 1);
        e.setNextFetchTime((int) (now + COOLDOWN));
        repo.save(e);
        redis.delete(String.format(AD_KEY, userId));
        // TODO: Thưởng cho user ở đây (gọi service khác)
    }

    // Helper: reset lượt mỗi ngày (so sánh theo UTC ngày)
    private boolean sameDay(long t1, long t2) {
        Calendar c1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar c2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c1.setTimeInMillis(t1 * 1000);
        c2.setTimeInMillis(t2 * 1000);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    // Chuyển List<AdvertisementDTO> sang JSON và ngược lại
    private String toJson(List<AdvertisementDTO> list) {
        try { return new ObjectMapper().writeValueAsString(list); }
        catch (Exception ex) { return "[]"; }
    }
    private List<AdvertisementDTO> parseList(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<List<AdvertisementDTO>>() {});
        }
        catch (Exception ex) { return Collections.emptyList(); }
    }
}