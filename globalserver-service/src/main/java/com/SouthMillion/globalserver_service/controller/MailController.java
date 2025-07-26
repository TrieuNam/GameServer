package com.SouthMillion.globalserver_service.controller;

import com.SouthMillion.globalserver_service.service.MailServiceImpl;
import org.SouthMillion.dto.globalserver.MailAckInfo;
import org.SouthMillion.dto.globalserver.MailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mail")
public class MailController {
    @Autowired
    private MailServiceImpl mailService;

    // Lấy danh sách mail cho user
    @GetMapping("/list/{userId}")
    public List<MailDTO> getMailList(@PathVariable Long userId) {
        return mailService.getMailList(userId);
    }

    // Lấy chi tiết mail theo mailIndex
    @GetMapping("/detail/{userId}/{mailIndex}")
    public MailDTO getMailDetail(@PathVariable Long userId, @PathVariable Integer mailIndex) {
        return mailService.getMailDetail(userId, mailIndex);
    }

    // Xóa mail (danh sách mailIndexes)
    @PostMapping("/delete/{userId}")
    public List<MailAckInfo> deleteMails(@PathVariable Long userId, @RequestBody List<Integer> mailIndexes) {
        return mailService.deleteMails(userId, mailIndexes);
    }

    // Nhận thưởng mail (danh sách mailIndexes)
    @PostMapping("/fetch/{userId}")
    public List<MailAckInfo> fetchMails(@PathVariable Long userId, @RequestBody List<Integer> mailIndexes) {
        return mailService.fetchMails(userId, mailIndexes);
    }

    // Thao tác tổng hợp (type: 1=xóa, 2=đọc, 3=nhận)
    @PostMapping("/op/{userId}")
    public MailAckInfo mailOperation(
            @PathVariable Long userId,
            @RequestParam Integer type,
            @RequestParam Integer p1,
            @RequestParam(required = false) Integer p2) {
        return mailService.mailOperation(userId, type, p1, p2);
    }
}
