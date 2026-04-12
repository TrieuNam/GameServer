package com.SouthMillion.webSocket_server.handler.mail;

import com.SouthMillion.webSocket_server.dto.PlayerSession;
import com.SouthMillion.webSocket_server.net.Emitters;
import com.SouthMillion.webSocket_server.net.MessageHandler;
import com.SouthMillion.webSocket_server.service.client.BagFeign;
import com.SouthMillion.webSocket_server.service.client.WalletHttpClient;
import com.SouthMillion.webSocket_server.service.grpc.MailGrpcClient;
import org.SouthMillion.dto.bag.BagDTOs;
import org.SouthMillion.dto.wallet.WalletDTOs;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.mail.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.SouthMillion.proto.msgmail.Msgmail;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles mail operations from the client.
 *
 * Proto: PB_CSMailReq (9551) — type + p_1 + p_2
 * Responses:
 *   9501 PB_SCMailDeleteAck  — delete / mark-read ack
 *   9504 PB_SCMailListAck    — mail list
 *   9505 PB_SCMailDetail     — mail detail
 *   9506 PB_SCFetchMailAck   — fetch attachment ack
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailHandler implements MessageHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MailHandler.class);

    private final MailGrpcClient mailGrpcClient;
    private final BagFeign bagFeign;
    private final WalletHttpClient walletHttpClient;

    private static final int OP_GET_LIST        = 0;
    private static final int OP_GET_DETAIL      = 1;
    private static final int OP_DELETE          = 2;
    private static final int OP_FETCH           = 3;
    private static final int OP_MARK_READ       = 4;
    private static final int OP_DELETE_ALL_READ = 5;
    private static final int OP_FETCH_ALL       = 6;

    @Override
    public int[] interests() {
        return new int[]{9551}; // PB_CSMailReq
    }

    /** Gọi sau login: đẩy danh sách thư (9504) về client. */
    public Mono<Void> pushAll(PlayerSession session) {
        Long roleId = session.getRoleId();
        if (roleId == null) return Mono.empty();
        return Mono.fromRunnable(() -> handleGetList(session, roleId));
    }

    @Override
    public Mono<Void> handle(PlayerSession session, int msgId, byte[] payload) {
        return Mono.fromRunnable(() -> {
            try {
                Msgmail.PB_CSMailReq req = Msgmail.PB_CSMailReq.parseFrom(payload);
                int type  = req.hasType() ? req.getType() : 0;
                int p1    = req.hasP1()   ? req.getP1()   : 0;
                int p2    = req.hasP2()   ? req.getP2()   : 0;
                Long roleId = session.getRoleId();

                log.debug("[Mail] op={}, p1={}, p2={}, roleId={}", type, p1, p2, roleId);

                switch (type) {
                    case OP_GET_LIST        -> handleGetList(session, roleId);
                    case OP_GET_DETAIL      -> handleGetDetail(session, (long) p1);
                    case OP_DELETE          -> handleDelete(session, p1, p2);
                    case OP_FETCH           -> handleFetch(session, roleId, (long) p1, p2);
                    case OP_MARK_READ       -> handleMarkRead(session, p1, p2);
                    case OP_DELETE_ALL_READ -> handleDeleteAllRead(session, roleId);
                    case OP_FETCH_ALL       -> handleFetchAll(session, roleId);
                    default -> {
                        log.warn("[Mail] Unknown op={}", type);
                        sendDeleteAck(session, 0, 0, -1);
                    }
                }
            } catch (Exception e) {
                log.error("[Mail] Error for roleId={}", session.getRoleId(), e);
                sendDeleteAck(session, 0, 0, -1);
            }
        });
    }

    // op=0: Get mail list → 9504 PB_SCMailListAck
    private void handleGetList(PlayerSession session, Long roleId) {
        try {
            GetMailListResponse resp = mailGrpcClient.getMailList(String.valueOf(roleId));
            Msgmail.PB_SCMailListAck.Builder builder = Msgmail.PB_SCMailListAck.newBuilder();
            for (MailData mail : resp.getMailsList()) {
                builder.addMailBriefData(buildBriefData(mail));
            }
            Emitters.emit(session, 9504, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Mail] handleGetList error", e);
            Emitters.emit(session, 9504, Msgmail.PB_SCMailListAck.newBuilder().build().toByteArray());
        }
    }

    // op=1: Get mail detail → 9505 PB_SCMailDetail
    private void handleGetDetail(PlayerSession session, long mailId) {
        try {
            MailResponse resp = mailGrpcClient.readMail(mailId);
            Msgmail.PB_SCMailDetail.Builder builder = Msgmail.PB_SCMailDetail.newBuilder();
            if (resp.getSuccess() && resp.hasMail()) {
                MailData m = resp.getMail();
                builder.setMailType(m.getMailType());
                builder.setMailIndex((int) m.getId());
                if (!m.getTitle().isBlank()) builder.setSubject(ByteString.copyFromUtf8(m.getTitle()));
                if (!m.getContent().isBlank()) builder.setContenttxt(ByteString.copyFromUtf8(m.getContent()));
            }
            Emitters.emit(session, 9505, builder.build().toByteArray());
        } catch (Exception e) {
            log.error("[Mail] handleGetDetail error for mailId={}", mailId, e);
            Emitters.emit(session, 9505, Msgmail.PB_SCMailDetail.newBuilder().build().toByteArray());
        }
    }

    // op=2: Delete mail → 9501 PB_SCMailDeleteAck
    private void handleDelete(PlayerSession session, int mailId, int mailType) {
        try {
            ResponseStatus status = mailGrpcClient.deleteMail((long) mailId);
            sendDeleteAck(session, mailType, mailId, status.getSuccess() ? 0 : -1);
        } catch (Exception e) {
            log.error("[Mail] handleDelete error for mailId={}", mailId, e);
            sendDeleteAck(session, mailType, mailId, -1);
        }
    }

    // op=3: Fetch mail attachment → 9506 PB_SCFetchMailAck
    private void handleFetch(PlayerSession session, Long roleId, long mailId, int mailType) {
        try {
            ClaimAttachmentResponse resp = mailGrpcClient.claimAttachment(mailId);

            if (resp.getSuccess() && resp.getClaimedCount() > 0) {
                // Distribute rewards to player's bag and wallet
                distributeRewards(session, roleId, resp.getClaimedList());
                log.info("[Mail] Claimed {} attachments from mailId={} for roleId={}",
                        resp.getClaimedCount(), mailId, roleId);
            }

            sendFetchAck(session, mailType, (int) mailId, resp.getSuccess() ? 0 : -1);
        } catch (Exception e) {
            log.error("[Mail] handleFetch error for mailId={}", mailId, e);
            sendFetchAck(session, mailType, (int) mailId, -1);
        }
    }

    // op=4: Mark mail as read → 9501 PB_SCMailDeleteAck
    private void handleMarkRead(PlayerSession session, int mailId, int mailType) {
        try {
            mailGrpcClient.readMail((long) mailId); // triggers mark-as-read in service
            sendDeleteAck(session, mailType, mailId, 0);
        } catch (Exception e) {
            log.error("[Mail] handleMarkRead error for mailId={}", mailId, e);
            sendDeleteAck(session, mailType, mailId, -1);
        }
    }

    // op=5: Delete all read mails → 9501 PB_SCMailDeleteAck
    private void handleDeleteAllRead(PlayerSession session, Long roleId) {
        // No gRPC equivalent in mail-service proto; acknowledge success
        sendDeleteAck(session, 0, 0, 0);
    }

    // op=6: Fetch all attachments → 9506 PB_SCFetchMailAck
    private void handleFetchAll(PlayerSession session, Long roleId) {
        try {
            FetchAllAttachmentsResponse resp = mailGrpcClient.fetchAllAttachments(String.valueOf(roleId));

            if (resp.getSuccess() && resp.getClaimedCount() > 0) {
                log.info("[Mail] Fetched all attachments: {} items claimed for roleId={}",
                        resp.getClaimedCount(), roleId);
                // Note: FetchAllAttachments doesn't return detailed rewards in current proto
                // Rewards are distributed server-side. Just refresh player state.
                refreshPlayerState(session, roleId);
            }

            sendFetchAck(session, 0, 0, resp.getSuccess() ? 0 : -1);
        } catch (Exception e) {
            log.error("[Mail] handleFetchAll error for roleId={}", roleId, e);
            sendFetchAck(session, 0, 0, -1);
        }
    }

    private Msgmail.PB_MailBriefData buildBriefData(MailData mail) {
        Msgmail.PB_MailBriefData.Builder b = Msgmail.PB_MailBriefData.newBuilder();
        b.setMailType(mail.getMailType());
        b.setMailIndex((int) mail.getId());
        b.setRecvTime((int) (mail.getCreatedAtMs() / 1000));
        b.setIsRead(mail.getIsRead() ? 1 : 0);
        b.setIsFetch(mail.getAttachmentClaimed() ? 1 : 0);
        if (!mail.getTitle().isBlank()) b.setSubject(ByteString.copyFromUtf8(mail.getTitle()));
        return b.build();
    }

    private void sendDeleteAck(PlayerSession session, int mailType, int mailIndex, int ret) {
        try {
            Msgmail.PB_MailAckInfo ack = Msgmail.PB_MailAckInfo.newBuilder()
                    .setMailType(mailType).setMailIndex(mailIndex).setRet(ret).build();
            Msgmail.PB_SCMailDeleteAck resp = Msgmail.PB_SCMailDeleteAck.newBuilder()
                    .addAskInfo(ack).build();
            Emitters.emit(session, 9501, resp.toByteArray());
        } catch (Exception e) {
            log.error("[Mail] sendDeleteAck failed", e);
        }
    }

    private void sendFetchAck(PlayerSession session, int mailType, int mailIndex, int ret) {
        try {
            Msgmail.PB_MailAckInfo ack = Msgmail.PB_MailAckInfo.newBuilder()
                    .setMailType(mailType).setMailIndex(mailIndex).setRet(ret).build();
            Msgmail.PB_SCFetchMailAck resp = Msgmail.PB_SCFetchMailAck.newBuilder()
                    .addAskInfo(ack).build();
            Emitters.emit(session, 9506, resp.toByteArray());
        } catch (Exception e) {
            log.error("[Mail] sendFetchAck failed", e);
        }
    }

    /**
     * Distribute rewards from mail attachments to player's bag and wallet.
     *
     * @param session Player session
     * @param roleId Player role ID
     * @param attachments List of claimed attachments with items and currency
     */
    private void distributeRewards(PlayerSession session, Long roleId, List<MailAttachmentData> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        List<Integer> updatedItemIds = new ArrayList<>();

        for (MailAttachmentData attachment : attachments) {
            try {
                // Distribute items to bag
                if (attachment.getItemId() > 0 && attachment.getQuantity() > 0) {
                    BagDTOs.GrantReq itemReq = BagDTOs.GrantReq.builder()
                            .roleId(String.valueOf(roleId))
                            .items(List.of(BagDTOs.GrantItem.builder()
                                    .itemId(attachment.getItemId())
                                    .num(attachment.getQuantity())
                                    .build()))
                            .reason("mail_attachment")
                            .build();

                    bagFeign.grant(itemReq);
                    updatedItemIds.add(attachment.getItemId());

                    log.debug("[Mail] Added item {} x{} to bag for roleId={}",
                            attachment.getItemId(), attachment.getQuantity(), roleId);
                }

                // Distribute currency to wallet
                if (attachment.getCurrencyAmount() > 0 && !attachment.getCurrencyType().isBlank()) {
                    long currencyItemId = parseCurrencyType(attachment.getCurrencyType());
                    if (currencyItemId > 0) {
                        List<WalletDTOs.Change> changes = List.of(
                                WalletDTOs.Change.builder()
                                        .itemId(currencyItemId)
                                        .amount(attachment.getCurrencyAmount())
                                        .build()
                        );

                        WalletDTOs.BatchReq walletReq = WalletDTOs.BatchReq.builder()
                                .roleId(String.valueOf(roleId))
                                .changes(changes)
                                .idemKey("mail.reward:" + roleId + ":" + System.currentTimeMillis())
                                .reason(9551) // Mail operation
                                .reasonType(3) // Fetch attachment
                                .build();

                        walletHttpClient.batchAdd(walletReq);
                        updatedItemIds.add((int) currencyItemId);

                        log.debug("[Mail] Added {} {} to wallet for roleId={}",
                                attachment.getCurrencyAmount(), attachment.getCurrencyType(), roleId);
                    }
                }

            } catch (Exception e) {
                log.error("[Mail] Failed to distribute reward for roleId={}: {}", roleId, e.getMessage());
                // Continue processing other attachments even if one fails
            }
        }

        // Refresh affected items in client UI
        refreshBagItems(session, roleId, updatedItemIds);
        refreshWalletBalance(session, roleId);
    }

    /**
     * Parse currency type string to wallet item ID.
     * Common types: "gold" = 1, "diamond" = 2, "bindDiamond" = 3
     */
    private long parseCurrencyType(String currencyType) {
        if (currencyType == null || currencyType.isBlank()) {
            return 0;
        }
        return switch (currencyType.toLowerCase()) {
            case "gold" -> 1L;
            case "diamond" -> 2L;
            case "binddiamond", "bind_diamond" -> 3L;
            default -> {
                try {
                    yield Long.parseLong(currencyType);
                } catch (NumberFormatException e) {
                    log.warn("[Mail] Unknown currency type: {}", currencyType);
                    yield 0L;
                }
            }
        };
    }

    /**
     * Refresh specific bag items in client UI.
     */
    private void refreshBagItems(PlayerSession session, Long roleId, List<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }

        try {
            List<BagDTOs.ItemView> bagItems = bagFeign.list(String.valueOf(roleId));
            if (bagItems != null) {
                for (Integer itemId : itemIds) {
                    long count = bagItems.stream()
                            .filter(item -> item.getItemId() != null && item.getItemId() == itemId)
                            .mapToLong(item -> item.getNum() == null ? 0L : item.getNum().longValue())
                            .sum();

                    Emitters.sendKnapsackSingleInfo(session, itemId, count);
                }
            }
        } catch (Exception e) {
            log.warn("[Mail] Failed to refresh bag items for roleId={}: {}", roleId, e.getMessage());
        }
    }

    /**
     * Refresh wallet balances in client UI.
     */
    private void refreshWalletBalance(PlayerSession session, Long roleId) {
        try {
            WalletDTOs.BalancesResp walletResp = walletHttpClient.info(String.valueOf(roleId));
            if (walletResp != null && walletResp.balances() != null) {
                Emitters.sendWalletBalances(session, walletResp.balances());
                log.debug("[Mail] Refreshed wallet balances for roleId={}", roleId);
            }
        } catch (Exception e) {
            log.warn("[Mail] Failed to refresh wallet balance for roleId={}: {}", roleId, e.getMessage());
        }
    }

    /**
     * Refresh complete player state after fetch all operation.
     */
    private void refreshPlayerState(PlayerSession session, Long roleId) {
        refreshWalletBalance(session, roleId);

        // Optionally refresh entire bag
        try {
            List<BagDTOs.ItemView> bagItems = bagFeign.list(String.valueOf(roleId));
            Emitters.sendKnapsackAllInfo(session, bagItems);
        } catch (Exception e) {
            log.warn("[Mail] Failed to refresh bag for roleId={}: {}", roleId, e.getMessage());
        }
    }

}
