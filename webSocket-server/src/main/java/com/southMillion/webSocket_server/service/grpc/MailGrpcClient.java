package com.SouthMillion.webSocket_server.service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.SouthMillion.grpc.common.ResponseStatus;
import org.SouthMillion.proto.mail.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailGrpcClient {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MailGrpcClient.class);

    @GrpcClient("mail-service")
    private MailServiceGrpc.MailServiceBlockingStub stub;

    private static boolean isUnavailable(Exception e) {
        return e instanceof StatusRuntimeException s && s.getStatus().getCode() == Status.Code.UNAVAILABLE;
    }

    public GetMailListResponse getMailList(String roleId) {
        try {
            return stub.getMailList(GetMailListRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mail] getMailList: mail-service unavailable");
            else log.error("[grpc-mail] getMailList error: {}", e.getMessage());
            return GetMailListResponse.newBuilder().setSuccess(false).build();
        }
    }

    public MailResponse readMail(long mailId) {
        try {
            return stub.readMail(ReadMailRequest.newBuilder().setMailId(mailId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mail] readMail: mail-service unavailable");
            else log.error("[grpc-mail] readMail error: {}", e.getMessage());
            return MailResponse.newBuilder().setSuccess(false).build();
        }
    }

    public ResponseStatus deleteMail(long mailId) {
        try {
            return stub.deleteMail(DeleteMailRequest.newBuilder().setMailId(mailId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mail] deleteMail: mail-service unavailable");
            else log.error("[grpc-mail] deleteMail error: {}", e.getMessage());
            return ResponseStatus.newBuilder().setSuccess(false).setCode(-1).build();
        }
    }

    public ClaimAttachmentResponse claimAttachment(long mailId) {
        try {
            return stub.claimAttachment(ClaimAttachmentRequest.newBuilder().setMailId(mailId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mail] claimAttachment: mail-service unavailable");
            else log.error("[grpc-mail] claimAttachment error: {}", e.getMessage());
            return ClaimAttachmentResponse.newBuilder().setSuccess(false).build();
        }
    }

    public FetchAllAttachmentsResponse fetchAllAttachments(String roleId) {
        try {
            return stub.fetchAllAttachments(FetchAllAttachmentsRequest.newBuilder().setRoleId(roleId).build());
        } catch (Exception e) {
            if (isUnavailable(e)) log.warn("[grpc-mail] fetchAllAttachments: mail-service unavailable");
            else log.error("[grpc-mail] fetchAllAttachments error: {}", e.getMessage());
            return FetchAllAttachmentsResponse.newBuilder().setSuccess(false).build();
        }
    }
}
