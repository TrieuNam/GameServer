package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.mail.MailDTOs;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailService svc;

    // type=0 -> list
    @PostMapping("/list")
    public MailDTOs.MailListResp list(@RequestBody @Valid MailDTOs.MailListReq req) {
        return svc.list(req.userId());
    }

    // type=1 -> detail (mark read)
    @GetMapping("/{userId}/{mailId}")
    public MailDTOs.MailDetailResp detailAndMarkRead(@PathVariable String userId, @PathVariable String mailId) {
        return svc.detail(userId, mailId, true);
    }

    // type=2 -> delete
    @PostMapping("/{userId}/{mailId}/delete")
    public MailDTOs.MailDeleteResp delete(@PathVariable String userId, @PathVariable String mailId) {
        return svc.delete(userId, mailId);
    }

    // type=3 -> fetch reward
    @PostMapping("/{userId}/{mailId}/fetch")
    public MailDTOs.FetchMailResp fetch(@PathVariable String userId, @PathVariable String mailId) {
        return svc.fetch(userId, mailId);
    }
}