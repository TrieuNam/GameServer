package com.southMillion.webSocket_server.service.client;

import org.SouthMillion.dto.ShiZhuang.ShiZhuangDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "shizhuang-service")
public interface ShiZhuangFeignClient {

    @GetMapping("/api/shizhuang/get")
    ShiZhuangDto get(@RequestParam("userId") String userId, @RequestParam("id") int id);

    @GetMapping("/api/shizhuang/list")
    List<ShiZhuangDto> list(@RequestParam("userId") String userId);

    @PostMapping("/api/shizhuang/add")
    ShiZhuangDto add(@RequestParam("userId") String userId, @RequestParam("id") int id, @RequestParam("level") int level);

    @DeleteMapping("/api/shizhuang/delete")
    void delete(@RequestParam("userId") String userId, @RequestParam("id") int id);
}