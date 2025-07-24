package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.NoticeTimeService;
import org.SouthMillion.dto.user.NoticeTimeReqDto;
import org.SouthMillion.dto.user.NoticeTimeRespDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notice-time")
public class NoticeTimeController {
    @Autowired
    private NoticeTimeService noticeTimeService;

    @PostMapping("/get")
    public NoticeTimeRespDto getNoticeTime(@RequestBody NoticeTimeReqDto req) {
        NoticeTimeRespDto resp = new NoticeTimeRespDto();
        resp.setNoticeTime(noticeTimeService.getNoticeTime(req.getUserId()));
        return resp;
    }

    @PostMapping("/set")
    public NoticeTimeRespDto setNoticeTime(@RequestBody NoticeTimeReqDto req) {
        NoticeTimeRespDto resp = new NoticeTimeRespDto();
        resp.setNoticeTime(noticeTimeService.setNoticeTime(req.getUserId(), req.getParam()));
        return resp;
    }
}
