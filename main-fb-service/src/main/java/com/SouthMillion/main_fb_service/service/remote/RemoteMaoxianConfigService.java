package com.SouthMillion.main_fb_service.service.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to load maoxian.json from config-service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteMaoxianConfigService {
    
    private final ObjectMapper objectMapper;
    private volatile MaoxianConfig config;
    
    @PostConstruct
    public void init() {
        loadConfig();
    }
    
    public MaoxianConfig getConfig() {
        if (config == null) {
            loadConfig();
        }
        return config;
    }
    
    public Optional<MaoxianConfig.Clearance> findStage(Integer stage, Integer level) {
        if (config == null) {
            loadConfig();
        }
        return config.getClearance().stream()
                .filter(c -> c.getStage().equals(String.valueOf(stage)) && 
                           c.getLevel().equals(String.valueOf(level)))
                .findFirst();
    }
    
    public int maxLevelInStage(int stage) {
        if (config == null) {
            loadConfig();
        }
        return config.getClearance().stream()
                .filter(c -> c.getStage().equals(String.valueOf(stage)))
                .map(c -> Integer.parseInt(c.getLevel()))
                .max(Integer::compareTo)
                .orElse(0);
    }
    
    private void loadConfig() {
        // Mock data - in production would call:
        // String url = "http://config-service/configs/maoxian.json";
        // MaoxianConfig json = restTemplate.getForObject(url, MaoxianConfig.class);
        
        List<MaoxianConfig.Clearance> clearances = new ArrayList<>();
        
        // Sample stages 1-1 to 1-5
        for (int i = 1; i <= 5; i++) {
            MaoxianConfig.Clearance c = MaoxianConfig.Clearance.builder()
                    .stage("1")
                    .level(String.valueOf(i))
                    .score("100")
                    .now_box("10")
                    .quick_num("3")
                    .quick_expend("10|10|10")
                    .win(Arrays.asList(
                            new MaoxianConfig.Drop("50001", "10"),
                            new MaoxianConfig.Drop("50002", "5")
                    ))
                    .build();
            clearances.add(c);
        }
        
        List<MaoxianConfig.JieduanReward> chapterRewards = new ArrayList<>();
        MaoxianConfig.JieduanReward jr = MaoxianConfig.JieduanReward.builder()
                .stage("1")
                .chapter("chapter1")
                .clearance_condition("3")
                .win(Arrays.asList(
                        new MaoxianConfig.Drop("50001", "100"),
                        new MaoxianConfig.Drop("50002", "50")
                ))
                .build();
        chapterRewards.add(jr);
        
        config = MaoxianConfig.builder()
                .clearance(clearances)
                .jieduan_reward(chapterRewards)
                .build();
        
        log.info("Loaded {} maoxian clearances and {} chapter rewards", 
                clearances.size(), chapterRewards.size());
    }
}
