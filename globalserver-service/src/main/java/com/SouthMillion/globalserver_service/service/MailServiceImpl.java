package com.SouthMillion.globalserver_service.service;

import com.SouthMillion.globalserver_service.entity.MailEntity;
import com.SouthMillion.globalserver_service.repository.MailRepository;
import com.SouthMillion.globalserver_service.service.impl.MailService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ItemDTO;
import org.SouthMillion.dto.globalserver.MailAckInfo;
import org.SouthMillion.dto.globalserver.MailDTO;
import org.SouthMillion.dto.globalserver.MailEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {
    private final MailRepository mailRepo;
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, MailEvent> kafka;
    private static final String MAIL_LIST_KEY = "mail:list:%d";
    private static final ObjectMapper mapper = new ObjectMapper();

    // ===== Lấy danh sách mail (có cache) =====
    @Override
    public List<MailDTO> getMailList(Long userId) {
        String key = String.format(MAIL_LIST_KEY, userId);
        String cache = redis.opsForValue().get(key);
        if (cache != null) return parseJsonList(cache);

        List<MailEntity> entities = mailRepo.findAllByUserId(userId);
        List<MailDTO> result = entities.stream().map(this::entityToDto).collect(Collectors.toList());
        redis.opsForValue().set(key, toJson(result), Duration.ofMinutes(5));
        return result;
    }

    // ===== Lấy chi tiết mail =====
    @Override
    public MailDTO getMailDetail(Long userId, Integer mailIndex) {
        MailEntity entity = mailRepo.findByUserIdAndMailIndex(userId, mailIndex);
        if (entity == null) throw new RuntimeException("Mail not found or not belong to user");
        return entityToDto(entity);
    }

    // ===== Đánh dấu đã đọc =====
    public MailAckInfo readMail(Long userId, Integer mailIndex) {
        MailEntity entity = mailRepo.findByUserIdAndMailIndex(userId, mailIndex);
        MailAckInfo ack = new MailAckInfo();
        ack.setMailType(entity != null ? entity.getMailType() : 0);
        ack.setMailIndex(mailIndex);
        if (entity == null) {
            ack.setRet(1);
            return ack;
        }
        if (entity.getIsRead() != null && entity.getIsRead() == 1) {
            ack.setRet(2); // đã đọc
            return ack;
        }
        entity.setIsRead(1);
        mailRepo.save(entity);
        invalidateCache(userId);
        kafka.send("mail-event", MailEvent.builder().userId(userId).mailIndex(mailIndex).action("READ").build());
        ack.setRet(0);
        return ack;
    }

    // ===== Nhận thưởng mail =====
    @Override
    public List<MailAckInfo> fetchMails(Long userId, List<Integer> mailIndexes) {
        List<MailAckInfo> ackList = new ArrayList<>();
        for (Integer mailIndex : mailIndexes) {
            MailEntity entity = mailRepo.findByUserIdAndMailIndex(userId, mailIndex);
            MailAckInfo ack = new MailAckInfo();
            ack.setMailType(entity != null ? entity.getMailType() : 0);
            ack.setMailIndex(mailIndex);
            if (entity == null) {
                ack.setRet(1); // không tồn tại
            } else if (entity.getIsFetch() != null && entity.getIsFetch() == 1) {
                ack.setRet(2); // đã nhận
            } else {
                entity.setIsFetch(1);
                mailRepo.save(entity);
                invalidateCache(userId);
                kafka.send("mail-event", MailEvent.builder().userId(userId).mailIndex(mailIndex).action("FETCH").build());
                ack.setRet(0);
                // TODO: Thực hiện cộng quà cho user (gọi service khác)
            }
            ackList.add(ack);
        }
        return ackList;
    }

    // ===== Xoá mail =====
    @Override
    public List<MailAckInfo> deleteMails(Long userId, List<Integer> mailIndexes) {
        List<MailAckInfo> ackList = new ArrayList<>();
        for (Integer mailIndex : mailIndexes) {
            MailEntity entity = mailRepo.findByUserIdAndMailIndex(userId, mailIndex);
            MailAckInfo ack = new MailAckInfo();
            ack.setMailType(entity != null ? entity.getMailType() : 0);
            ack.setMailIndex(mailIndex);
            if (entity == null) {
                ack.setRet(1); // không tồn tại
            } else {
                mailRepo.delete(entity);
                invalidateCache(userId);
                kafka.send("mail-event", MailEvent.builder().userId(userId).mailIndex(mailIndex).action("DELETE").build());
                ack.setRet(0);
            }
            ackList.add(ack);
        }
        return ackList;
    }

    // ===== Thao tác tổng hợp (type: 1 = delete, 2 = read, 3 = fetch) =====
    @Override
    public MailAckInfo mailOperation(Long userId, Integer type, Integer p1, Integer p2) {
        // type = 1: xóa, 2: đọc, 3: nhận thưởng
        if (type == 1) { // delete
            List<MailAckInfo> list = deleteMails(userId, List.of(p1));
            return list.isEmpty() ? null : list.get(0);
        } else if (type == 2) { // read
            return readMail(userId, p1);
        } else if (type == 3) { // fetch
            List<MailAckInfo> list = fetchMails(userId, List.of(p1));
            return list.isEmpty() ? null : list.get(0);
        }
        return null;
    }

    // ===== Helper & Cache =====
    private void invalidateCache(Long userId) {
        redis.delete(String.format(MAIL_LIST_KEY, userId));
    }

    private MailDTO entityToDto(MailEntity e) {
        MailDTO dto = new MailDTO();
        dto.setMailIndex(e.getMailIndex());
        dto.setMailType(e.getMailType());
        dto.setSubject(e.getSubject());
        dto.setContent(e.getContent());
        dto.setIsRead(e.getIsRead() != null && e.getIsRead() == 1 ? 1 : 0);
        dto.setIsFetch(e.getIsFetch() != null && e.getIsFetch() == 1 ? 1 : 0);
        dto.setRecvTime(e.getRecvTime());
        // Parse itemDataJson
        if (e.getItemDataJson() != null && !e.getItemDataJson().isEmpty()) {
            try {
                List<ItemDTO> itemList = mapper.readValue(e.getItemDataJson(), new TypeReference<List<ItemDTO>>() {});
                dto.setItemData(itemList);
            } catch (Exception ex) {
                dto.setItemData(Collections.emptyList());
            }
        } else {
            dto.setItemData(Collections.emptyList());
        }
        return dto;
    }

    private List<MailDTO> parseJsonList(String json) {
        try {
            return mapper.readValue(json, new TypeReference<List<MailDTO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }
}