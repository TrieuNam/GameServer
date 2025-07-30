package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.service.config.ItemConfigService;
import org.SouthMillion.dto.item.Knapsack.ItemConfigDTO;
import org.SouthMillion.dto.item.Knapsack.ItemRetrieveConfigDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/item-config")
public class ItemConfigController {

    @Autowired
    private ItemConfigService itemConfigService;

    /**
     * Lấy thông tin cấu hình của item bất kỳ theo itemId (tìm trong tất cả các file json)
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<?> getItemConfig(@PathVariable int itemId) {
        Object config = itemConfigService.getConfigById(itemId);
        if (config == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(config); // sẽ trả về đúng kiểu DTO hoặc Map tuỳ từng itemId
    }

    @GetMapping("/item-retrieve")
    public ItemRetrieveConfigDTO getItemRetrieveConfig() {
        return itemConfigService.getItemRetrieveConfig();
    }
}