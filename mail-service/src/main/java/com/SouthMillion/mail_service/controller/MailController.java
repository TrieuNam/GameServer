package com.SouthMillion.mail_service.controller;

import com.SouthMillion.mail_service.dto.MailDTO;
import com.SouthMillion.mail_service.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

/**
 * Mail REST Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Validated
public class MailController {

    private final MailService mailService;

    @PostMapping("/send")
    public ResponseEntity<MailDTO.Response<MailDTO.MailInfo>> sendMail(
            @Valid @RequestBody MailDTO.SendMailRequest request) {
        log.info("REST API: Send mail - receiver={}", request.getReceiverId());
        return ResponseEntity.ok(mailService.sendMail(request));
    }

    @PostMapping("/send-bulk")
    public ResponseEntity<MailDTO.Response<Integer>> sendBulkMail(
            @Valid @RequestBody MailDTO.BulkMailRequest request) {
        log.info("REST API: Send bulk mail - receivers={}", request.getReceiverIds().size());
        return ResponseEntity.ok(mailService.sendBulkMail(request));
    }

    @GetMapping("/list/{roleId}")
    public ResponseEntity<MailDTO.Response<MailDTO.MailListResponse>> getMailList(
            @PathVariable String roleId) {
        log.info("REST API: Get mail list - roleId={}", roleId);
        return ResponseEntity.ok(mailService.getMailList(roleId));
    }

    @PutMapping("/{mailId}/read")
    public ResponseEntity<MailDTO.Response<MailDTO.MailInfo>> readMail(
            @PathVariable Long mailId) {
        log.info("REST API: Read mail - mailId={}", mailId);
        return ResponseEntity.ok(mailService.readMail(mailId));
    }

    @PostMapping("/{mailId}/claim")
    public ResponseEntity<MailDTO.Response<MailDTO.ClaimAttachmentResponse>> claimAttachment(
            @PathVariable Long mailId) {
        log.info("REST API: Claim attachment - mailId={}", mailId);
        return ResponseEntity.ok(mailService.claimAttachment(mailId));
    }

    @DeleteMapping("/{mailId}")
    public ResponseEntity<MailDTO.Response<Void>> deleteMail(@PathVariable Long mailId) {
        log.info("REST API: Delete mail - mailId={}", mailId);
        return ResponseEntity.ok(mailService.deleteMail(mailId));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Mail Service is running!");
    }

    @DeleteMapping("/batch/read/{roleId}")
    public ResponseEntity<MailDTO.Response<Integer>> deleteAllReadMails(
            @PathVariable String roleId) {
        log.info("REST API: Delete all read mails - roleId={}", roleId);
        return ResponseEntity.ok(mailService.deleteAllReadMails(roleId));
    }

    @PostMapping("/fetch/all/{roleId}")
    public ResponseEntity<MailDTO.Response<Integer>> fetchAllAttachments(
            @PathVariable String roleId) {
        log.info("REST API: Fetch all attachments - roleId={}", roleId);
        return ResponseEntity.ok(mailService.fetchAllAttachments(roleId));
    }
}
