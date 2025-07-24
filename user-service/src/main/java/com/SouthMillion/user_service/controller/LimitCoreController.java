package com.SouthMillion.user_service.controller;

import com.SouthMillion.user_service.service.LimitCoreService;
import org.SouthMillion.dto.user.LimitCoreInfoDto;
import org.SouthMillion.dto.user.LimitCoreReqDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/limit-core")
public class LimitCoreController {
    @Autowired
    private LimitCoreService limitCoreService;

    @PostMapping("/get")
    public LimitCoreInfoDto getCoreInfo(@RequestBody LimitCoreReqDto req) {
        LimitCoreInfoDto dto = new LimitCoreInfoDto();
        dto.setCoreLevel(limitCoreService.getCoreLevels(req.getUserId()));
        return dto;
    }

    @PostMapping("/update")
    public LimitCoreInfoDto updateCoreInfo(@RequestBody LimitCoreReqDto req) {
        LimitCoreInfoDto dto = new LimitCoreInfoDto();
        dto.setCoreLevel(limitCoreService.updateCoreLevel(req.getUserId(), req.getType(), req.getP1()));
        return dto;
    }
}