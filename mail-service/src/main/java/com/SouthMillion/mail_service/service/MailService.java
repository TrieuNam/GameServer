package com.SouthMillion.mail_service.service;

import com.SouthMillion.mail_service.client.BagClient;
import com.SouthMillion.mail_service.client.WalletClient;
import com.SouthMillion.mail_service.dto.MailDTO;
import com.SouthMillion.mail_service.entity.*;
import com.SouthMillion.mail_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.bag.GrantReq;
import org.SouthMillion.dto.bag.ItemInfo;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Mail Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final MailRepository mailRepository;
    private final MailAttachmentRepository attachmentRepository;
    private final WalletClient walletClient;
    private final BagClient bagClient;

    // Virtual Thread executor for parallel operations
    private final Executor virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Transactional
    public MailDTO.Response<MailDTO.MailInfo> sendMail(MailDTO.SendMailRequest request) {
        log.info("Sending mail: type={}, receiver={}", request.getType(), request.getReceiverId());

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(request.getExpirationDays());

        Mail mail = Mail.builder()
                .type(request.getType())
                .senderId(request.getSenderId() != null && !request.getSenderId().isEmpty() ? Long.parseLong(request.getSenderId()) : null)
                .senderName(request.getSenderName())
                .receiverId(Long.parseLong(request.getReceiverId()))
                .title(request.getTitle())
                .content(request.getContent())
                .expiresAt(expiresAt)
                .build();

        mail = mailRepository.save(mail);
        log.info("Mail created: id={}", mail.getId());

        // Save attachments
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (MailDTO.AttachmentInfo attachmentInfo : request.getAttachments()) {
                MailAttachment attachment = MailAttachment.builder()
                        .mailId(mail.getId())
                        .attachmentType(attachmentInfo.getAttachmentType())
                        .itemId(attachmentInfo.getItemId())
                        .itemName(attachmentInfo.getItemName())
                        .quantity(attachmentInfo.getQuantity())
                        .quality(attachmentInfo.getQuality())
                        .build();
                attachmentRepository.save(attachment);
            }
            log.info("Attachments saved: count={}", request.getAttachments().size());
        }

        return MailDTO.Response.success(buildMailInfo(mail));
    }

    @Transactional
    public MailDTO.Response<Integer> sendBulkMail(MailDTO.BulkMailRequest request) {
        log.info("Sending bulk mail: receivers={}", request.getReceiverIds().size());

        int sentCount = 0;
        for (String receiverId : request.getReceiverIds()) {
            MailDTO.SendMailRequest singleRequest = MailDTO.SendMailRequest.builder()
                    .type(request.getType())
                    .senderId(request.getSenderId())
                    .senderName(request.getSenderName())
                    .receiverId(receiverId)
                    .title(request.getTitle())
                    .content(request.getContent())
                    .expirationDays(request.getExpirationDays())
                    .attachments(request.getAttachments())
                    .build();

            sendMail(singleRequest);
            sentCount++;
        }

        log.info("Bulk mail sent: count={}", sentCount);
        return MailDTO.Response.success(sentCount);
    }

    @Transactional(readOnly = true)
    public MailDTO.Response<MailDTO.MailListResponse> getMailList(String roleId) {
        log.info("Getting mail list: roleId={}", roleId);

        List<Mail> mails = mailRepository.findByReceiverIdAndIsDeletedFalseOrderByCreatedAtDesc(Long.parseLong(roleId));
        Integer unreadCount = mailRepository.countUnreadMails(Long.parseLong(roleId));

        List<MailDTO.MailInfo> mailInfos = mails.stream()
                .map(this::buildMailInfo)
                .collect(Collectors.toList());

        MailDTO.MailListResponse response = MailDTO.MailListResponse.builder()
                .totalCount(mails.size())
                .unreadCount(unreadCount)
                .mails(mailInfos)
                .build();

        return MailDTO.Response.success(response);
    }

    @Transactional
    public MailDTO.Response<MailDTO.MailInfo> readMail(Long mailId) {
        log.info("Reading mail: mailId={}", mailId);

        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        if (mail.isExpired()) {
            return MailDTO.Response.error(-1, "Mail has expired");
        }

        mail.markAsRead();
        mailRepository.save(mail);

        return MailDTO.Response.success(buildMailInfo(mail));
    }

    @Transactional
    public MailDTO.Response<MailDTO.ClaimAttachmentResponse> claimAttachment(Long mailId) {
        log.info("Claiming attachment: mailId={}", mailId);

        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        if (mail.isExpired()) {
            return MailDTO.Response.error(-1, "Mail has expired");
        }

        if (mail.getIsClaimedAttachment()) {
            return MailDTO.Response.error(-2, "Attachment already claimed");
        }

        List<MailAttachment> attachments = attachmentRepository.findByMailId(mailId);
        if (attachments.isEmpty()) {
            return MailDTO.Response.error(-3, "No attachments found");
        }

        mail.markAsClaimedAttachment();
        mailRepository.save(mail);

        List<MailDTO.AttachmentInfo> attachmentInfos = attachments.stream()
                .map(this::buildAttachmentInfo)
                .collect(Collectors.toList());

        // Grant rewards to player
        try {
            grantRewardsToPlayer(String.valueOf(mail.getReceiverId()), attachments);
            log.info("Rewards granted: roleId={}, count={}", mail.getReceiverId(), attachments.size());
        } catch (Exception e) {
            log.error("Failed to grant rewards: {}", e.getMessage());
            return MailDTO.Response.error(-4, "Failed to grant rewards");
        }

        MailDTO.ClaimAttachmentResponse response = MailDTO.ClaimAttachmentResponse.builder()
                .mailId(mailId)
                .claimedAttachments(attachmentInfos)
                .message("Attachments claimed successfully")
                .build();

        return MailDTO.Response.success(response);
    }

    @Transactional
    public MailDTO.Response<Void> deleteMail(Long mailId) {
        log.info("Deleting mail: mailId={}", mailId);

        Mail mail = mailRepository.findById(mailId)
                .orElseThrow(() -> new RuntimeException("Mail not found"));

        if (!mail.getIsClaimedAttachment() && attachmentRepository.existsByMailId(mailId)) {
            return MailDTO.Response.error(-1, "Cannot delete mail with unclaimed attachments");
        }

        mail.markAsDeleted();
        mailRepository.save(mail);

        return MailDTO.Response.success(null);
    }

    @Transactional
    public MailDTO.Response<Integer> deleteAllReadMails(String roleId) {
        Long receiverId = Long.parseLong(roleId);
        List<Mail> readMails = mailRepository.findReadMails(receiverId);
        int count = 0;
        for (Mail mail : readMails) {
            if (Boolean.TRUE.equals(mail.getIsClaimedAttachment()) || 
                !attachmentRepository.existsByMailId(mail.getId())) {
                mail.setIsDeleted(true);
                count++;
            }
        }
        mailRepository.saveAll(readMails.stream()
            .filter(m -> Boolean.TRUE.equals(m.getIsDeleted()))
            .collect(Collectors.toList()));
        log.info("Deleted {} read mails for roleId={}", count, roleId);
        return MailDTO.Response.success(count);
    }

    @Transactional
    public MailDTO.Response<Integer> fetchAllAttachments(String roleId) {
        Long receiverId = Long.parseLong(roleId);
        List<Mail> unclaimedMails = mailRepository.findUnclaimedMails(receiverId);

        // FIX N+1 QUERY: Batch load all attachments at once
        List<Long> mailIds = unclaimedMails.stream().map(Mail::getId).collect(Collectors.toList());
        Map<Long, List<MailAttachment>> attachmentsByMail = attachmentRepository.findByMailIdIn(mailIds)
                .stream()
                .collect(Collectors.groupingBy(MailAttachment::getMailId));

        int count = 0;
        for (Mail mail : unclaimedMails) {
            List<MailAttachment> attachments = attachmentsByMail.getOrDefault(mail.getId(), List.of());
            if (!attachments.isEmpty()) {
                grantRewardsToPlayer(roleId, attachments);
                mail.setIsClaimedAttachment(true);
                mailRepository.save(mail);
                count++;
            }
        }
        log.info("Fetched attachments from {} mails for roleId={}", count, roleId);
        return MailDTO.Response.success(count);
    }

    private MailDTO.MailInfo buildMailInfo(Mail mail) {
        List<MailAttachment> attachments = attachmentRepository.findByMailId(mail.getId());

        return MailDTO.MailInfo.builder()
                .id(mail.getId())
                .type(mail.getType())
                .senderId(mail.getSenderId() != null ? String.valueOf(mail.getSenderId()) : null)
                .senderName(mail.getSenderName())
                .receiverId(String.valueOf(mail.getReceiverId()))
                .title(mail.getTitle())
                .content(mail.getContent())
                .isRead(mail.getIsRead())
                .isClaimedAttachment(mail.getIsClaimedAttachment())
                .hasAttachments(!attachments.isEmpty())
                .expiresAt(mail.getExpiresAt())
                .createdAt(mail.getCreatedAt())
                .attachments(attachments.stream()
                        .map(this::buildAttachmentInfo)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Grant rewards to player from mail attachments
     * OPTIMIZED: Parallel wallet and bag calls using Virtual Threads
     */
    private void grantRewardsToPlayer(String roleId, List<MailAttachment> attachments) {
        // Separate currency and items
        long totalGold = 0;
        long totalDiamond = 0;
        List<ItemInfo> items = new ArrayList<>();

        for (MailAttachment attachment : attachments) {
            Integer type = attachment.getAttachmentType();
            if (type == 1) { // GOLD
                totalGold += attachment.getQuantity();
            } else if (type == 2) { // DIAMOND
                totalDiamond += attachment.getQuantity();
            } else if (type == 3 || type == 4) { // ITEM or EQUIPMENT
                try {
                    Integer itemIdInt = Integer.parseInt(attachment.getItemId());
                    items.add(ItemInfo.builder()
                            .itemId(itemIdInt)
                            .quantity(attachment.getQuantity())
                            .build());
                } catch (NumberFormatException e) {
                    log.warn("Invalid item ID format: {}", attachment.getItemId());
                }
            } else {
                log.warn("Unknown attachment type: {}", type);
            }
        }

        // Build currency request
        final long finalTotalGold = totalGold;
        final long finalTotalDiamond = totalDiamond;
        WalletDTOs.BatchReq walletReq = null;
        if (totalGold > 0 || totalDiamond > 0) {
            List<WalletDTOs.Change> changes = new ArrayList<>();
            if (totalGold > 0) {
                changes.add(WalletDTOs.Change.builder()
                        .itemId(1L) // Gold
                        .amount(totalGold)
                        .build());
            }
            if (totalDiamond > 0) {
                changes.add(WalletDTOs.Change.builder()
                        .itemId(3L) // Diamond
                        .amount(totalDiamond)
                        .build());
            }
            walletReq = WalletDTOs.BatchReq.builder()
                    .roleId(roleId)
                    .changes(changes)
                    .reason(201) // MAIL_CLAIM
                    .build();
        }

        // Build items request
        GrantReq grantReq = null;
        if (!items.isEmpty()) {
            grantReq = GrantReq.builder()
                    .userId(1L) // audit field
                    .roleId(roleId)
                    .items(items)
                    .eventId("MAIL_CLAIM_" + System.currentTimeMillis())
                    .source("Mail reward")
                    .build();
        }

        // PARALLEL EXECUTION with Virtual Threads
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Grant currency if any
        if (walletReq != null) {
            final WalletDTOs.BatchReq finalWalletReq = walletReq;
            CompletableFuture<Void> walletFuture = CompletableFuture.runAsync(() -> {
                walletClient.grantCurrency(roleId, finalWalletReq);
                log.info("Currency granted: roleId={}, gold={}, diamond={}", roleId, finalTotalGold, finalTotalDiamond);
            }, virtualExecutor);
            futures.add(walletFuture);
        }

        // Grant items if any
        if (grantReq != null) {
            final GrantReq finalGrantReq = grantReq;
            final int itemCount = items.size();
            CompletableFuture<Void> bagFuture = CompletableFuture.runAsync(() -> {
                bagClient.grantItems(finalGrantReq);
                log.info("Items granted: roleId={}, count={}", roleId, itemCount);
            }, virtualExecutor);
            futures.add(bagFuture);
        }

        // Wait for all parallel operations to complete
        if (!futures.isEmpty()) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    private MailDTO.AttachmentInfo buildAttachmentInfo(MailAttachment attachment) {
        return MailDTO.AttachmentInfo.builder()
                .attachmentType(attachment.getAttachmentType())
                .itemId(attachment.getItemId())
                .itemName(attachment.getItemName())
                .quantity(attachment.getQuantity())
                .quality(attachment.getQuality())
                .build();
    }
}
