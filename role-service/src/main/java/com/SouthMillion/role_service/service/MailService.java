package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.Mail;
import com.SouthMillion.role_service.repository.MailRepository;
import com.SouthMillion.role_service.service.producer.BagEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.role.event.BagGrantEvent;
import org.SouthMillion.dto.role.mail.MailDTOs;
import org.SouthMillion.dto.role.mail.MailItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailRepository repo;
    private final BagEventProducer bagEventProducer;

    @Transactional(readOnly = true)
    public MailDTOs.MailListResp list(String userId) {
        var now = Instant.now();
        List<Mail> list = repo.findActiveByUserId(userId, now);
        var summaries = list.stream()
                .map(m -> new MailDTOs.MailSummary(m.getMailId(), m.getTitle(), m.isRead(), m.isFetched(), m.hasReward(), m.getExpireAt(), m.getCreatedAt()))
                .toList();
        return new MailDTOs.MailListResp(summaries);
    }

    @Transactional
    public MailDTOs.MailDetailResp detail(String userId, String mailId, boolean markRead) {
        Mail m = repo.findByMailIdAndUserId(mailId, userId).orElseThrow(() -> new IllegalArgumentException("Mail not found"));
        if (m.isExpired()) throw new IllegalArgumentException("Mail expired");
        if (markRead && !m.isRead()) { m.setRead(true); repo.save(m); }
        return new MailDTOs.MailDetailResp(m.getMailId(), m.getUserId(), m.getTitle(), m.getContent(), m.isRead(), m.isFetched(), m.getItems(), m.getExpireAt(), m.getCreatedAt());
    }

    @Transactional
    public MailDTOs.MailDeleteResp delete(String userId, String mailId) {
        Mail m = repo.findByMailIdAndUserId(mailId, userId).orElseThrow(() -> new IllegalArgumentException("Mail not found"));
        if (m.hasReward() && !m.isFetched()) {
            throw new IllegalArgumentException("Không thể xóa thư có quà chưa nhận");
        }
        repo.delete(m);
        return new MailDTOs.MailDeleteResp(mailId, true);
    }

    @Transactional
    public MailDTOs.FetchMailResp fetch(String userId, String mailId) {
        Mail m = repo.findByMailIdAndUserId(mailId, userId).orElseThrow(() -> new IllegalArgumentException("Mail not found"));
        if (m.isExpired()) throw new IllegalArgumentException("Mail expired");
        if (m.isFetched()) {
            return new MailDTOs.FetchMailResp(mailId, true, m.getItems());
        }
        // Grant items from mail via bag-service Kafka event
        if (m.getItems() != null && !m.getItems().isEmpty()) {
            List<BagGrantEvent.Item> bagItems = new ArrayList<>();
            for (MailItem mi : m.getItems()) {
                if ("ITEM".equalsIgnoreCase(mi.getType())) {
                    try {
                        bagItems.add(BagGrantEvent.Item.builder()
                                .itemId(Integer.parseInt(mi.getItemId()))
                                .num((int) mi.getCount())
                                .bind(false)
                                .expireAt(null)
                                .build());
                    } catch (NumberFormatException ex) {
                        log.warn("[Mail] Non-numeric itemId='{}' for userId={}, skipping bag grant",
                                mi.getItemId(), userId);
                    }
                } else {
                    // CURRENCY type: wallet-service integration pending
                    log.info("[Mail] Currency reward itemId={} count={} for userId={} (wallet grant pending)",
                            mi.getItemId(), mi.getCount(), userId);
                }
            }
            if (!bagItems.isEmpty()) {
                bagEventProducer.publishGrant(userId, mailId, bagItems, "mail-reward");
                log.info("[Mail] Published {} bag items for userId={}, mailId={}",
                        bagItems.size(), userId, mailId);
            }
        }
        m.setFetched(true);
        m.setRead(true);
        repo.save(m);
        return new MailDTOs.FetchMailResp(mailId, true, m.getItems());
    }
}