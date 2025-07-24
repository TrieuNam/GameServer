package com.SouthMillion.globalserver_service.controller;

import com.SouthMillion.globalserver_service.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notice")
public class NoticeController {
    @Autowired
    private NoticeService noticeService;

    // API cho Feign gọi
    @GetMapping("/num")
    public Integer getUnreadNoticeNum(@RequestParam String userKey) {
        return noticeService.getUnreadNoticeNum(userKey);
    }

    @GetMapping("/latest")
    public long getLatestNoticeTime() {
        return noticeService.getLatest();
    }

    @PostMapping("/set")
    public void setNoticeTime(@RequestBody long time) {
        noticeService.setNoticeTime(time);
    }
}