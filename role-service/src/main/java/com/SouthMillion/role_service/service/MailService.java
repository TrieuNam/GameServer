package com.SouthMillion.role_service.service;

import com.SouthMillion.role_service.entity.Mail;
import com.SouthMillion.role_service.entity.Role;
import com.SouthMillion.role_service.repository.MailRepository;
import com.SouthMillion.role_service.repository.RoleRepository;
import com.SouthMillion.role_service.service.client.BagFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.BagAddItemReq;
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
    private final RoleRepository roleRepo;
    private final BagFeign bagFeign;

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

        if (m.getItems() != null && !m.getItems().isEmpty()) {
            // Resolve roleId from userId (take first/primary role)
            Long roleId = roleRepo.findByUserIdOrderByRoleIdAsc(userId)
                    .stream().findFirst().map(Role::getRoleId).orElse(null);
            if (roleId == null) {
                log.warn("[Mail] No role found for userId={}, skipping bag grant for mailId={}", userId, mailId);
            } else {
                List<BagAddItemReq.Item> bagItems = new ArrayList<>();
                for (MailItem mi : m.getItems()) {
                    if ("ITEM".equalsIgnoreCase(mi.getType())) {
                        try {
                            bagItems.add(BagAddItemReq.Item.builder()
                                    .itemId(Integer.parseInt(mi.getItemId()))
                                    .amount((int) mi.getCount())
                                    .bound(false)
                                    .build());
                        } catch (NumberFormatException ex) {
                            log.warn("[Mail] Non-numeric itemId='{}' for userId={}, skipping", mi.getItemId(), userId);
                        }
                    } else {
                        // CURRENCY type: wallet-service integration pending
                        log.info("[Mail] Currency reward itemId={} count={} for userId={} (wallet grant pending)",
                                mi.getItemId(), mi.getCount(), userId);
                    }
                }
                if (!bagItems.isEmpty()) {
                    try {
                        BagAddItemReq req = BagAddItemReq.builder()
                                .userId(0L) // userId is UUID string in role-service; bag-service only uses this as audit field
                                .roleId(roleId)
                                .items(bagItems)
                                .source("mail-reward")
                                .idemKey(mailId) // mailId as idempotency key — safe to retry
                                .build();
                        bagFeign.add(req);
                        log.info("[Mail] Granted {} bag items via REST for userId={} roleId={} mailId={}",
                                bagItems.size(), userId, roleId, mailId);
                    } catch (Exception e) {
                        log.error("[Mail] Failed to grant bag items for userId={} roleId={} mailId={}: {}",
                                userId, roleId, mailId, e.getMessage());
                        throw new IllegalStateException("Bag grant failed, mail fetch aborted: " + e.getMessage());
                    }
                }
            }
        }

        m.setFetched(true);
        m.setRead(true);
        repo.save(m);
        return new MailDTOs.FetchMailResp(mailId, true, m.getItems());
    }
}