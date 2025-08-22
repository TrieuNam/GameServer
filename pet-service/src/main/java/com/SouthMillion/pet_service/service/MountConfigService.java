package com.SouthMillion.pet_service.service;

import com.SouthMillion.pet_service.service.client.ConfigFeignClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.SouthMillion.dto.item.Knapsack.HarnessItemConfigDTO;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MountConfigService {
    private final ConfigFeignClient configFeignClient;
    private final ObjectMapper objectMapper;

    private Map<Integer, ModelItemConfigDTO> modelMap = new HashMap<>();
    private Map<Integer, HarnessItemConfigDTO> harnessMap = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            reloadAll();
        } catch (Exception ex) {
            ex.printStackTrace(); // Đừng để app die, log ra rõ
        }
    }

    public void reloadAll() {
        // Load model_item.json
        JsonNode config = configFeignClient.getConfig("model_item");
        JsonNode modelNode = config.get("model");  // Lấy mảng con "model"
        if (modelNode == null || !modelNode.isArray()) {
            throw new RuntimeException("model_item config is null or not array! node=" + modelNode);
        }
        ModelItemConfigDTO[] models = objectMapper.convertValue(modelNode, ModelItemConfigDTO[].class);

        modelMap.clear();
        for (ModelItemConfigDTO m : models) modelMap.put(m.getId(), m);

        // Load harness_item.json
        JsonNode harnessNode ;
        try {
            JsonNode configNode = configFeignClient.getConfig("harness_item");
             harnessNode = configNode.get("harness_item");  // lấy mảng con
            System.out.println("harnessNode=" + harnessNode);
        } catch (Exception ex) {
            System.out.println("Lỗi Feign getConfig harness_item: " + ex.getMessage());
            throw ex;
        }
        if (harnessNode == null || !harnessNode.isArray()) {
            throw new RuntimeException("harness_item config is null or not array! node=" + harnessNode);
        }
        HarnessItemConfigDTO[] harnessArr = objectMapper.convertValue(harnessNode, HarnessItemConfigDTO[].class);
        harnessMap.clear();
        for (HarnessItemConfigDTO h : harnessArr) harnessMap.put(h.getId(), h);
    }

    // Lấy 1 model config theo id
    public ModelItemConfigDTO getModel(Integer id) {
        return modelMap.get(id);
    }
    // Lấy tất cả model config
    public Collection<ModelItemConfigDTO> getAllModel() {
        return modelMap.values();
    }

    // Lấy 1 harness config theo id
    public HarnessItemConfigDTO getHarness(Integer id) {
        return harnessMap.get(id);
    }
    // Lấy tất cả harness config
    public Collection<HarnessItemConfigDTO> getAllHarness() {
        return harnessMap.values();
    }
}