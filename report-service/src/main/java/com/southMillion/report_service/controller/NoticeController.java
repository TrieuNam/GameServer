package com.southMillion.report_service.controller;

import com.southMillion.report_service.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.report.NoticeDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping("/create")
    public NoticeDTO createNotice(@RequestBody NoticeDTO dto) {
        return noticeService.createNotice(dto);
    }

    @GetMapping("/list")
    public List<NoticeDTO> getAllNotice() {
        return noticeService.getAllNotice();
    }
}
