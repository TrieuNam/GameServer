package com.SouthMillion.globalserver_service.service;

import com.SouthMillion.globalserver_service.entity.MailEntity;
import com.SouthMillion.globalserver_service.repository.MailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.ItemDTO;
import org.SouthMillion.dto.globalserver.MailDTO;
import org.SouthMillion.dto.globalserver.MailEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MailService {
    @Autowired
    private MailRepository mailRepo;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private KafkaTemplate<String, MailEvent> kafka;

    private static final String MAIL_LIST_KEY = "mail:list:%d";

    // Lấy danh sách mail (có cache)
    public List<MailDTO> getUserMailList(Long userId) {
        String key = String.format(MAIL_LIST_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parseJsonList(cache);

        List<MailEntity> entities = mailRepo.findByUserId(userId);
        List<MailDTO> result = entities.stream().map(this::entityToDto).collect(Collectors.toList());
        redis.opsForValue().set(key, toJson(result), Duration.ofMinutes(5));
        return result;
    }

    // Lấy chi tiết mail
    public MailDTO getMailDetail(Integer mailId, Long userId) {
        MailEntity entity = mailRepo.findById(mailId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Mail not found or not belong to user"));
        return entityToDto(entity);
    }

    // Đánh dấu đã đọc
    public void readMail(Integer mailId, Long userId) {
        MailEntity entity = mailRepo.findById(mailId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Mail not found or not belong to user"));
        entity.setIsRead(true);
        mailRepo.save(entity);
        invalidateCache(userId);
        kafka.send("mail-event", new MailEvent().builder().userId(userId).mailId(mailId).action("READ").build());
    }

    // Nhận thưởng mail
    public void fetchMail(Integer mailId, Long userId) {
        MailEntity entity = mailRepo.findById(mailId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Mail not found or not belong to user"));
        if (entity.getIsFetched() != null && entity.getIsFetched())
            throw new RuntimeException("Mail already fetched");
        entity.setIsFetched(true);
        mailRepo.save(entity);
        invalidateCache(userId);
        kafka.send("mail-event", new MailEvent().builder().userId(userId).mailId(mailId).action("FETCH").build());
        // TODO: Cộng thưởng cho user (gọi service khác)
    }

    // Xoá mail
    public void deleteMail(Integer mailId, Long userId) {
        MailEntity entity = mailRepo.findById(mailId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Mail not found or not belong to user"));
        mailRepo.delete(entity);
        invalidateCache(userId);
        kafka.send("mail-event", new MailEvent().builder().userId(userId).mailId(mailId).action("DELETE").build());
    }

    // ===== Helper =====
    private void invalidateCache(Long userId) {
        redis.delete(String.format(MAIL_LIST_KEY, userId));
    }

    private MailDTO entityToDto(MailEntity e) {
        MailDTO dto = new MailDTO();
        dto.setId(e.getId());
        dto.setMailType(e.getMailType());
        dto.setSubject(e.getSubject());
        dto.setContent(e.getContent());
        dto.setIsRead(Boolean.TRUE.equals(e.getIsRead()));
        dto.setIsFetched(Boolean.TRUE.equals(e.getIsFetched()));
        dto.setRecvTime(e.getRecvTime());
        // Parse itemDataJson
        if (e.getItemDataJson() != null && !e.getItemDataJson().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                dto.setItemList(Arrays.asList(mapper.readValue(e.getItemDataJson(), ItemDTO[].class)));
            } catch (Exception ex) {
                dto.setItemList(Collections.emptyList());
            }
        } else dto.setItemList(Collections.emptyList());
        return dto;
    }

    private List<MailDTO> parseJsonList(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return Arrays.asList(mapper.readValue(json, MailDTO[].class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJson(Object o) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }
}