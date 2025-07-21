package com.southMillion.serverInfo_service.controller;

import com.southMillion.serverInfo_service.service.ServerInfoService;
import org.SouthMillion.dto.serverInfor.ServerInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/server-info")
public class ServerInfoController {
    @Autowired
    private ServerInfoService service;

    // Lấy object đầy đủ
    @GetMapping
    public ServerInfoDto getServerInfo() {
        return service.getServerInfo();
    }

    @GetMapping("/real-start-time")
    public long getServerRealStartTime() {
        return service.getServerInfo().getRealStartTime();
    }

    // Update (admin tool, chỉ dùng khi thật sự cần)
    @PostMapping
    public ServerInfoDto update(@RequestBody ServerInfoDto dto) {
        return service.updateServerInfo(dto);
    }

    @GetMapping("/combine-time")
    public Long getServerCombineTime() {
        return service.getServerCombineTime();
    }
}