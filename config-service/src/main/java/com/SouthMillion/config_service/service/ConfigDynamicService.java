package com.SouthMillion.config_service.service;


import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ConfigDynamicService {
    private final ConfigFileService fileService;
    private final ConfigRedisService redisService;

    // Khai báo Virtual Thread Pool (Java 21+)
    private final ExecutorService vThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    // Khai báo Platform Thread Pool (OS Thread)
    private final ExecutorService platformThreadPool = Executors.newFixedThreadPool(4);

    // Danh sách mapping từng nhóm file
    private static final List<String> TASK_FILES = List.of("task_cfg", "task_day", "escort");
    private static final List<String> REWARD_FILES = List.of("chongzhireward_spid", "gem_cfg", "sumo_pagoda");
    private static final List<String> DUNGEON_FILES = List.of("lumo", "tumo", "hunpo");
    private static final List<String> SYSTEM_FILES = List.of("commonconfig_fable", "commonconfig_table", "role_name");
    private static final List<String> AUDIO_LOG_FILES = List.of("audio", "logconfig");
    private static final List<String> SHOP_FILES = List.of("shop_cfg", "inscription");

    public ConfigDynamicService(ConfigFileService fileService, ConfigRedisService redisService) {
        this.fileService = fileService;
        this.redisService = redisService;
    }

    /** Đọc/caching 1 file config, ưu tiên cache, dùng cho mọi API */
    public JsonNode getConfig(String name) {
        JsonNode config = redisService.getConfig(name);
        if (config != null) return config;
        config = fileService.readConfig(name);
        if (config != null) redisService.setConfig(name, config);
        return config;
    }

    /** Lấy group nhiệm vụ, preload lên Redis trên Virtual Thread */
    public CompletableFuture<Map<String, JsonNode>> preloadTasksAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, JsonNode> map = new LinkedHashMap<>();
            for (String name : TASK_FILES) {
                JsonNode node = fileService.readConfig(name);
                if (node != null) {
                    redisService.setConfig(name, node);
                    map.put(name, node);
                }
            }
            return map;
        }, vThreadPool);
    }

    /** Preload nhóm reward (I/O bound, Virtual Thread) */
    public CompletableFuture<Map<String, JsonNode>> preloadRewardsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, JsonNode> map = new LinkedHashMap<>();
            for (String name : REWARD_FILES) {
                JsonNode node = fileService.readConfig(name);
                if (node != null) {
                    redisService.setConfig(name, node);
                    map.put(name, node);
                }
            }
            return map;
        }, vThreadPool);
    }

    /** API group get từng nhóm (ưu tiên cache, không reload lại file) */
    public Map<String, JsonNode> getAllTasks() {
        return TASK_FILES.stream().collect(Collectors.toMap(n -> n, this::getConfig));
    }
    /** Lấy tất cả phần thưởng config (ưu tiên cache, không reload lại file) */
    public Map<String, JsonNode> getAllRewards() {
        return REWARD_FILES.stream().collect(Collectors.toMap(n -> n, this::getConfig));
    }
    /** Lấy tất cả dungeon config (ưu tiên cache, không reload lại file) */
    public Map<String, JsonNode> getAllDungeons() {
        return DUNGEON_FILES.stream().collect(Collectors.toMap(n -> n, this::getConfig));
    }
    /** Lấy tất cả hệ thống config (ưu tiên cache, không reload lại file) */
    public Map<String, JsonNode> getAllSystems() {
        return SYSTEM_FILES.stream().collect(Collectors.toMap(n -> n, this::getConfig));
    }
    /** Lấy tất cả audio log config (ưu tiên cache, không reload lại file) */
    public Map<String, JsonNode> getAllAudioLog() {
        return AUDIO_LOG_FILES.stream().collect(Collectors.toMap(n -> n, this::getConfig));
    }
    /** Lấy tất cả shop config (ưu tiên cache, không reload lại file) */
    public Map<String, JsonNode> getAllShops() {
        return SHOP_FILES.stream().collect(Collectors.toMap(n -> n, this::getConfig));
    }

    /** Xóa toàn bộ cache (test Platform Thread) */
    public void clearConfigCache() {
        platformThreadPool.execute(() -> {
            redisService.deleteAll();
            System.out.println("Cache cleared on platform thread: " + Thread.currentThread());
        });
    }

    /** Task compute nặng (chạy Platform Thread) */
    public void runHeavyCompute() {
        platformThreadPool.execute(() -> {
            System.out.println("Start heavy compute on platform thread: " + Thread.currentThread());
            long sum = 0;
            for (int i = 0; i < 1_000_000_000; i++) sum += i;
            System.out.println("Heavy compute done, result: " + sum);
        });
    }

    /** Scheduler tự động clear cache mỗi 3 ngày (chạy Platform Thread) */
    @Scheduled(cron = "0 0 3 */3 * ?")
    public void autoClearConfigCache() {
        clearConfigCache();
    }
}