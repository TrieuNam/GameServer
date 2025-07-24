package com.SouthMillion.globalserver_service.controller;

import com.SouthMillion.globalserver_service.service.MailService;
import org.SouthMillion.dto.globalserver.MailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mail")
public class MailController {
    @Autowired
    private MailService mailService;

    // Lấy danh sách mail
    @GetMapping("/list/{userId}")
    public List<MailDTO> list(@PathVariable Long userId) {
        return mailService.getUserMailList(userId);
    }

    // Lấy chi tiết mail
    @GetMapping("/detail/{userId}/{mailId}")
    public MailDTO detail(@PathVariable Long userId, @PathVariable Integer mailId) {
        return mailService.getMailDetail(mailId, userId);
    }

    // Đọc mail (mark read)
    @PostMapping("/read/{userId}/{mailId}")
    public void read(@PathVariable Long userId, @PathVariable Integer mailId) {
        mailService.readMail(mailId, userId);
    }

    // Nhận thưởng mail
    @PostMapping("/fetch/{userId}/{mailId}")
    public void fetch(@PathVariable Long userId, @PathVariable Integer mailId) {
        mailService.fetchMail(mailId, userId);
    }

    // Xoá mail
    @DeleteMapping("/delete/{userId}/{mailId}")
    public void delete(@PathVariable Long userId, @PathVariable Integer mailId) {
        mailService.deleteMail(mailId, userId);
    }
}
