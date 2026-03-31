package com.SouthMillion.mail_service.service;

import com.SouthMillion.mail_service.client.BagClient;
import com.SouthMillion.mail_service.client.WalletClient;
import com.SouthMillion.mail_service.dto.MailDTO;
import com.SouthMillion.mail_service.entity.Mail;
import com.SouthMillion.mail_service.entity.MailAttachment;
import com.SouthMillion.mail_service.repository.MailAttachmentRepository;
import com.SouthMillion.mail_service.repository.MailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MailService Tests")
class MailServiceTest {

    @Mock
    private MailRepository mailRepository;

    @Mock
    private MailAttachmentRepository attachmentRepository;

    @Mock
    private WalletClient walletClient;

    @Mock
    private BagClient bagClient;

    @InjectMocks
    private MailService mailService;

    private static final String RECEIVER_ID = "1001";
    private static final Long MAIL_ID = 1L;

    // =========================================================
    // sendMail
    // =========================================================
    @Nested
    @DisplayName("sendMail()")
    class SendMail {

        @Test
        @DisplayName("TC-MAIL-001 [P] Gui mail khong co attachment thanh cong")
        void sendMail_noAttachment_success() {
            MailDTO.SendMailRequest req = MailDTO.SendMailRequest.builder()
                    .type(1)
                    .senderId("system")
                    .senderName("System")
                    .receiverId(RECEIVER_ID)
                    .title("Welcome")
                    .content("Hello!")
                    .expirationDays(7)
                    .attachments(List.of())
                    .build();

            Mail saved = new Mail();
            saved.setId(MAIL_ID);
            saved.setReceiverId(Long.parseLong(RECEIVER_ID));
            given(mailRepository.save(any(Mail.class))).willReturn(saved);
            given(attachmentRepository.findByMailId(MAIL_ID)).willReturn(List.of());

            MailDTO.Response<MailDTO.MailInfo> resp = mailService.sendMail(req);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).isNotNull();
            then(attachmentRepository).should(never()).save(any(MailAttachment.class));
        }

        @Test
        @DisplayName("TC-MAIL-002 [P] Gui mail co attachment – luu attachment")
        void sendMail_withAttachment_savesAttachment() {
            MailDTO.AttachmentInfo att = MailDTO.AttachmentInfo.builder()
                    .attachmentType(1)
                    .itemId("100")
                    .itemName("Gold")
                    .quantity(500)
                    .quality(1)
                    .build();

            MailDTO.SendMailRequest req = MailDTO.SendMailRequest.builder()
                    .type(3)
                    .senderId("system")
                    .senderName("System")
                    .receiverId(RECEIVER_ID)
                    .title("Reward Mail")
                    .content("Your reward!")
                    .expirationDays(7)
                    .attachments(List.of(att))
                    .build();

            Mail saved = new Mail();
            saved.setId(MAIL_ID);
            saved.setReceiverId(Long.parseLong(RECEIVER_ID));
            given(mailRepository.save(any(Mail.class))).willReturn(saved);
            given(attachmentRepository.save(any(MailAttachment.class))).willAnswer(inv -> inv.getArgument(0));
            given(attachmentRepository.findByMailId(MAIL_ID)).willReturn(List.of());

            MailDTO.Response<MailDTO.MailInfo> resp = mailService.sendMail(req);

            assertThat(resp.getCode()).isZero();
            then(attachmentRepository).should(times(1)).save(any(MailAttachment.class));
        }

        @Test
        @DisplayName("TC-MAIL-003 [P] Gui mail co 3 attachments – luu 3 lan")
        void sendMail_multipleAttachments_savesAll() {
            List<MailDTO.AttachmentInfo> atts = List.of(
                    MailDTO.AttachmentInfo.builder().attachmentType(1).itemId("1").quantity(100).build(),
                    MailDTO.AttachmentInfo.builder().attachmentType(2).itemId("2").quantity(50).build(),
                    MailDTO.AttachmentInfo.builder().attachmentType(3).itemId("3").quantity(10).build()
            );

            MailDTO.SendMailRequest req = MailDTO.SendMailRequest.builder()
                    .type(3)
                    .senderId("system")
                    .receiverId(RECEIVER_ID)
                    .title("Multi Reward")
                    .expirationDays(7)
                    .attachments(atts)
                    .build();

            Mail saved = new Mail();
            saved.setId(MAIL_ID);
            saved.setReceiverId(Long.parseLong(RECEIVER_ID));
            given(mailRepository.save(any(Mail.class))).willReturn(saved);
            given(attachmentRepository.save(any(MailAttachment.class))).willAnswer(inv -> inv.getArgument(0));
            given(attachmentRepository.findByMailId(MAIL_ID)).willReturn(List.of());

            mailService.sendMail(req);

            then(attachmentRepository).should(times(3)).save(any(MailAttachment.class));
        }
    }

    // =========================================================
    // sendBulkMail
    // =========================================================
    @Nested
    @DisplayName("sendBulkMail()")
    class SendBulkMail {

        @Test
        @DisplayName("TC-MAIL-005 [P] Gui bulk mail cho 3 nguoi – tra ve count=3")
        void sendBulkMail_threeReceivers_returnsCount3() {
            MailDTO.BulkMailRequest req = MailDTO.BulkMailRequest.builder()
                    .type(1)
                    .senderId("system")
                    .senderName("System")
                    .receiverIds(List.of("101", "102", "103"))
                    .title("Bulk Mail")
                    .content("Content")
                    .expirationDays(7)
                    .attachments(List.of())
                    .build();

            Mail saved = new Mail();
            saved.setId(MAIL_ID);
            given(mailRepository.save(any(Mail.class))).willReturn(saved);
            given(attachmentRepository.findByMailId(any())).willReturn(List.of());

            MailDTO.Response<Integer> resp = mailService.sendBulkMail(req);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData()).isEqualTo(3);
            then(mailRepository).should(times(3)).save(any(Mail.class));
        }
    }

    // =========================================================
    // getMailList
    // =========================================================
    @Nested
    @DisplayName("getMailList()")
    class GetMailList {

        @Test
        @DisplayName("TC-MAIL-010 [P] Lay danh sach mail – tra ve dung so luong")
        void getMailList_returnsMails() {
            Mail m1 = new Mail();
            m1.setId(1L);
            Mail m2 = new Mail();
            m2.setId(2L);
            given(mailRepository.findByReceiverIdAndIsDeletedFalseOrderByCreatedAtDesc(anyLong()))
                    .willReturn(List.of(m1, m2));
            given(mailRepository.countUnreadMails(anyLong())).willReturn(1);
            given(attachmentRepository.findByMailId(anyLong())).willReturn(List.of());

            MailDTO.Response<MailDTO.MailListResponse> resp = mailService.getMailList(RECEIVER_ID);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData().getTotalCount()).isEqualTo(2);
            assertThat(resp.getData().getUnreadCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-MAIL-011 [P] Khong co mail – tra ve danh sach rong")
        void getMailList_empty_returnsEmptyList() {
            given(mailRepository.findByReceiverIdAndIsDeletedFalseOrderByCreatedAtDesc(anyLong()))
                    .willReturn(List.of());
            given(mailRepository.countUnreadMails(anyLong())).willReturn(0);

            MailDTO.Response<MailDTO.MailListResponse> resp = mailService.getMailList(RECEIVER_ID);

            assertThat(resp.getData().getTotalCount()).isZero();
            assertThat(resp.getData().getMails()).isEmpty();
        }
    }

    // =========================================================
    // readMail
    // =========================================================
    @Nested
    @DisplayName("readMail()")
    class ReadMail {

        @Test
        @DisplayName("TC-MAIL-015 [P] Doc mail hop le – danh dau da doc")
        void readMail_validMail_marksRead() {
            Mail mail = mock(Mail.class);
            given(mail.isExpired()).willReturn(false);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));
            given(mailRepository.save(mail)).willReturn(mail);
            given(attachmentRepository.findByMailId(any())).willReturn(List.of());

            MailDTO.Response<MailDTO.MailInfo> resp = mailService.readMail(MAIL_ID);

            assertThat(resp.getCode()).isZero();
            then(mail).should().markAsRead();
        }

        @Test
        @DisplayName("TC-MAIL-016 [N] Mail het han – tra ve error(-1)")
        void readMail_expired_returnsError() {
            Mail mail = mock(Mail.class);
            given(mail.isExpired()).willReturn(true);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));

            MailDTO.Response<MailDTO.MailInfo> resp = mailService.readMail(MAIL_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
            assertThat(resp.getMessage()).contains("expired");
            then(mail).should(never()).markAsRead();
        }

        @Test
        @DisplayName("TC-MAIL-017 [N] Mail khong ton tai – nem RuntimeException")
        void readMail_notFound_throwsException() {
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mailService.readMail(MAIL_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mail not found");
        }
    }

    // =========================================================
    // claimAttachment
    // =========================================================
    @Nested
    @DisplayName("claimAttachment()")
    class ClaimAttachment {

        @Test
        @DisplayName("TC-MAIL-020 [P] Nhan qua dinh kem thanh cong")
        void claimAttachment_success() {
            Mail mail = mock(Mail.class);
            given(mail.isExpired()).willReturn(false);
            given(mail.getIsClaimedAttachment()).willReturn(false);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));

            MailAttachment att = new MailAttachment();
            att.setMailId(MAIL_ID);
            att.setAttachmentType(5); // EXP type – no wallet/bag call needed
            att.setQuantity(100);
            given(attachmentRepository.findByMailId(MAIL_ID)).willReturn(List.of(att));
            given(mailRepository.save(mail)).willReturn(mail);

            MailDTO.Response<MailDTO.ClaimAttachmentResponse> resp = mailService.claimAttachment(MAIL_ID);

            assertThat(resp.getCode()).isZero();
            assertThat(resp.getData().getClaimedAttachments()).hasSize(1);
            then(mail).should().markAsClaimedAttachment();
        }

        @Test
        @DisplayName("TC-MAIL-021 [N] Mail het han – tra ve error(-1)")
        void claimAttachment_expired_returnsError() {
            Mail mail = mock(Mail.class);
            given(mail.isExpired()).willReturn(true);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));

            MailDTO.Response<MailDTO.ClaimAttachmentResponse> resp = mailService.claimAttachment(MAIL_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
        }

        @Test
        @DisplayName("TC-MAIL-022 [N] Da nhan qua truoc do – tra ve error(-2)")
        void claimAttachment_alreadyClaimed_returnsError() {
            Mail mail = mock(Mail.class);
            given(mail.isExpired()).willReturn(false);
            given(mail.getIsClaimedAttachment()).willReturn(true);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));

            MailDTO.Response<MailDTO.ClaimAttachmentResponse> resp = mailService.claimAttachment(MAIL_ID);

            assertThat(resp.getCode()).isEqualTo(-2);
            assertThat(resp.getMessage()).contains("claimed");
            then(mail).should(never()).markAsClaimedAttachment();
        }

        @Test
        @DisplayName("TC-MAIL-023 [N] Khong co attachment – tra ve error(-3)")
        void claimAttachment_noAttachment_returnsError() {
            Mail mail = mock(Mail.class);
            given(mail.isExpired()).willReturn(false);
            given(mail.getIsClaimedAttachment()).willReturn(false);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));
            given(attachmentRepository.findByMailId(MAIL_ID)).willReturn(List.of());

            MailDTO.Response<MailDTO.ClaimAttachmentResponse> resp = mailService.claimAttachment(MAIL_ID);

            assertThat(resp.getCode()).isEqualTo(-3);
        }

        @Test
        @DisplayName("TC-MAIL-024 [N] Mail khong ton tai – nem RuntimeException")
        void claimAttachment_notFound_throwsException() {
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mailService.claimAttachment(MAIL_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mail not found");
        }
    }

    // =========================================================
    // deleteMail
    // =========================================================
    @Nested
    @DisplayName("deleteMail()")
    class DeleteMail {

        @Test
        @DisplayName("TC-MAIL-030 [P] Xoa mail da nhan qua – thanh cong")
        void deleteMail_claimedAttachment_success() {
            Mail mail = mock(Mail.class);
            given(mail.getIsClaimedAttachment()).willReturn(true);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));
            given(mailRepository.save(mail)).willReturn(mail);

            MailDTO.Response<Void> resp = mailService.deleteMail(MAIL_ID);

            assertThat(resp.getCode()).isZero();
            then(mail).should().markAsDeleted();
        }

        @Test
        @DisplayName("TC-MAIL-031 [P] Xoa mail khong co attachment – thanh cong")
        void deleteMail_noAttachment_success() {
            Mail mail = mock(Mail.class);
            given(mail.getIsClaimedAttachment()).willReturn(false);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));
            given(attachmentRepository.existsByMailId(MAIL_ID)).willReturn(false);
            given(mailRepository.save(mail)).willReturn(mail);

            MailDTO.Response<Void> resp = mailService.deleteMail(MAIL_ID);

            assertThat(resp.getCode()).isZero();
        }

        @Test
        @DisplayName("TC-MAIL-032 [N] Xoa mail chua nhan qua – tra ve error(-1)")
        void deleteMail_unclaimedAttachment_returnsError() {
            Mail mail = mock(Mail.class);
            given(mail.getIsClaimedAttachment()).willReturn(false);
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.of(mail));
            given(attachmentRepository.existsByMailId(MAIL_ID)).willReturn(true);

            MailDTO.Response<Void> resp = mailService.deleteMail(MAIL_ID);

            assertThat(resp.getCode()).isEqualTo(-1);
            assertThat(resp.getMessage()).contains("unclaimed");
            then(mail).should(never()).markAsDeleted();
        }

        @Test
        @DisplayName("TC-MAIL-033 [N] Mail khong ton tai – nem RuntimeException")
        void deleteMail_notFound_throwsException() {
            given(mailRepository.findById(MAIL_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mailService.deleteMail(MAIL_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Mail not found");
        }
    }
}
