package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.config.ItemRegistry;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.ItemMeta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/item")
@RequiredArgsConstructor
public class ItemMetaController {

    private final ItemRegistry registry;

    /**
     * ids=1,2,3 -> {
     *   "1": {"itemId":1,"pileLimit":99,"isVirtual":0,"normalizedId":1},
     *   "2": {...}
     * }
     */
    @GetMapping("/meta")
    public Map<String, Map<String, Object>> batchMeta(@RequestParam("ids") String idsCsv) {
        var out = new LinkedHashMap<String, Map<String, Object>>();
        if (idsCsv == null || idsCsv.isBlank()) return out;

        // Giữ nguyên thứ tự theo input
        for (String raw : idsCsv.split(",")) {
            String k = raw.trim();
            if (k.isEmpty()) continue;

            int id;
            try { id = Integer.parseInt(k); } catch (NumberFormatException e) { continue; }

            var metaOpt = registry.meta(id);
            if (metaOpt.isEmpty()) {
                // Vẫn trả object để caller dễ xử lý (tuỳ bạn muốn bỏ qua cũng được)
                out.put(k, Map.of("itemId", id, "notFound", true));
                continue;
            }
            var meta = metaOpt.get();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", meta.id());
            m.put("pileLimit", meta.pileLimit());              // 0 => unlimited (theo rule C++)
            m.put("isVirtual", meta.virtualItem() ? 1 : 0);    // int 0/1 như ví dụ của bạn
            m.put("normalizedId", normalizedId(meta));         // hiện = chính nó; mở rộng sau nếu cần
            out.put(k, m);
        }
        return out;
    }

    // Hook để hợp nhất các biến thể (bound/unbound, skin…), mặc định trả chính nó
    private int normalizedId(ItemMeta meta) {
        return meta.id();
    }
}