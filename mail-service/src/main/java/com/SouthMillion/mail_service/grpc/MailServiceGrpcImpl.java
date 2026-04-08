package com.SouthMillion.mail_service.grpc;

import com.SouthMillion.mail_service.dto.MailDTO;
import com.SouthMillion.mail_service.service.MailService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.mail.*;


@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MailServiceGrpcImpl extends MailServiceGrpc.MailServiceImplBase {

    private final MailService mailService;

    private boolean isOk(MailDTO.Response<?> r) {
        return r != null && Integer.valueOf(0).equals(r.getCode());
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void sendMail(SendMailRequest request, StreamObserver<SendMailResponse> observer) {
        log.info("[MailGrpc] SendMail: receiver={}", request.getReceiverId());
        try {
            MailDTO.SendMailRequest dto = MailDTO.SendMailRequest.builder()
                    .type(request.getMailType() > 0 ? request.getMailType() : 1)
                    .senderId(request.getSenderId())
                    .senderName(request.getSenderName())
                    .receiverId(request.getReceiverId())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .attachments(request.getAttachmentsList().stream()
                            .map(a -> MailDTO.AttachmentInfo.builder()
                                    .attachmentType(a.getItemId() > 0 ? 3 : 1)
                                    .itemId(String.valueOf(a.getItemId()))
                                    .quantity(a.getQuantity() > 0 ? a.getQuantity() : 1)
                                    .build())
                            .toList())
                    .build();
            var result = mailService.sendMail(dto);
            boolean ok0 = isOk(result);
            observer.onNext(SendMailResponse.newBuilder()
                    .setSuccess(ok0)
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setMailId(ok0 && result.getData() != null && result.getData().getId() != null ? result.getData().getId() : 0L)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[MailGrpc] SendMail error", e);
            observer.onNext(SendMailResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void sendBulkMail(SendBulkMailRequest request, StreamObserver<SendBulkMailResponse> observer) {
        log.info("[MailGrpc] SendBulkMail: receivers={}", request.getReceiverIdsCount());
        try {
            MailDTO.BulkMailRequest dto = MailDTO.BulkMailRequest.builder()
                    .type(request.getMailType() > 0 ? request.getMailType() : 1)
                    .senderId(request.getSenderId())
                    .senderName(request.getSenderName())
                    .receiverIds(request.getReceiverIdsList())
                    .title(request.getTitle())
                    .content(request.getContent())
                    .build();
            var result = mailService.sendBulkMail(dto);
            boolean ok1 = isOk(result);
            observer.onNext(SendBulkMailResponse.newBuilder()
                    .setSuccess(ok1)
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setSentCount(ok1 && result.getData() != null ? result.getData() : 0)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[MailGrpc] SendBulkMail error", e);
            observer.onNext(SendBulkMailResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void getMailList(GetMailListRequest request, StreamObserver<GetMailListResponse> observer) {
        log.info("[MailGrpc] GetMailList: roleId={}", request.getRoleId());
        try {
            var result = mailService.getMailList(request.getRoleId());
            boolean ok2 = isOk(result);
            GetMailListResponse.Builder resp = GetMailListResponse.newBuilder().setSuccess(ok2);
            if (ok2 && result.getData() != null) {
                resp.setUnreadCount(result.getData().getUnreadCount() != null ? result.getData().getUnreadCount() : 0);
                if (result.getData().getMails() != null) {
                    result.getData().getMails().forEach(m -> resp.addMails(toMailData(m)));
                }
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[MailGrpc] GetMailList error", e);
            observer.onNext(GetMailListResponse.newBuilder().setSuccess(false).build());
            observer.onCompleted();
        }
    }

    @Override
    public void readMail(ReadMailRequest request, StreamObserver<MailResponse> observer) {
        log.info("[MailGrpc] ReadMail: mailId={}", request.getMailId());
        try {
            var result = mailService.readMail(request.getMailId());
            boolean ok3 = isOk(result);
            MailResponse.Builder resp = MailResponse.newBuilder()
                    .setSuccess(ok3)
                    .setMessage(result.getMessage() != null ? result.getMessage() : "");
            if (ok3 && result.getData() != null) {
                resp.setMail(toMailData(result.getData()));
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[MailGrpc] ReadMail error", e);
            observer.onNext(MailResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void claimAttachment(ClaimAttachmentRequest request, StreamObserver<ClaimAttachmentResponse> observer) {
        log.info("[MailGrpc] ClaimAttachment: mailId={}", request.getMailId());
        try {
            var result = mailService.claimAttachment(request.getMailId());
            boolean ok4 = isOk(result);
            ClaimAttachmentResponse.Builder resp = ClaimAttachmentResponse.newBuilder()
                    .setSuccess(ok4)
                    .setMessage(result.getMessage() != null ? result.getMessage() : "");
            if (ok4 && result.getData() != null && result.getData().getClaimedAttachments() != null) {
                result.getData().getClaimedAttachments().forEach(a ->
                        resp.addClaimed(MailAttachmentData.newBuilder()
                                .setItemId(a.getItemId() != null ? parseInt(a.getItemId()) : 0)
                                .setQuantity(a.getQuantity() != null ? a.getQuantity() : 0)
                                .build()));
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[MailGrpc] ClaimAttachment error", e);
            observer.onNext(ClaimAttachmentResponse.newBuilder().setSuccess(false).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void deleteMail(DeleteMailRequest request, StreamObserver<ResponseStatus> observer) {
        log.info("[MailGrpc] DeleteMail: mailId={}", request.getMailId());
        try {
            var result = mailService.deleteMail(request.getMailId());
            boolean ok5 = isOk(result);
            observer.onNext(ResponseStatus.newBuilder()
                    .setSuccess(ok5)
                    .setMessage(result.getMessage() != null ? result.getMessage() : "")
                    .setCode(ok5 ? 200 : 400)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[MailGrpc] DeleteMail error", e);
            observer.onNext(ResponseStatus.newBuilder().setSuccess(false).setCode(500).setMessage(e.getMessage()).build());
            observer.onCompleted();
        }
    }

    @Override
    public void fetchAllAttachments(FetchAllAttachmentsRequest request, StreamObserver<FetchAllAttachmentsResponse> observer) {
        log.info("[MailGrpc] FetchAllAttachments: roleId={}", request.getRoleId());
        try {
            var result = mailService.fetchAllAttachments(request.getRoleId());
            boolean ok6 = isOk(result);
            observer.onNext(FetchAllAttachmentsResponse.newBuilder()
                    .setSuccess(ok6)
                    .setClaimedCount(ok6 && result.getData() != null ? result.getData() : 0)
                    .build());
            observer.onCompleted();
        } catch (Exception e) {
            log.error("[MailGrpc] FetchAllAttachments error", e);
            observer.onNext(FetchAllAttachmentsResponse.newBuilder().setSuccess(false).build());
            observer.onCompleted();
        }
    }

    private MailData toMailData(MailDTO.MailInfo m) {
        MailData.Builder builder = MailData.newBuilder()
                .setId(m.getId() != null ? m.getId() : 0L)
                .setSenderId(m.getSenderId() != null ? m.getSenderId() : "")
                .setSenderName(m.getSenderName() != null ? m.getSenderName() : "")
                .setReceiverId(m.getReceiverId() != null ? m.getReceiverId() : "")
                .setTitle(m.getTitle() != null ? m.getTitle() : "")
                .setContent(m.getContent() != null ? m.getContent() : "")
                .setMailType(m.getType() != null ? m.getType() : 0)
                .setIsRead(Boolean.TRUE.equals(m.getIsRead()))
                .setHasAttachment(Boolean.TRUE.equals(m.getHasAttachments()))
                .setAttachmentClaimed(Boolean.TRUE.equals(m.getIsClaimedAttachment()))
                .setCreatedAtMs(m.getCreatedAt() != null ? m.getCreatedAt().toEpochSecond(java.time.ZoneOffset.UTC) * 1000L : 0L);
        return builder.build();
    }
}
