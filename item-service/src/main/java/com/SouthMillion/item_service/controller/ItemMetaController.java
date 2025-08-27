package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.config.ItemRegistry;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.ItemMeta;
import org.springframework.web.bind.annotation.*;

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
        registry.ensureLoaded();

        var out = new LinkedHashMap<String, Map<String, Object>>();
        if (idsCsv == null || idsCsv.isBlank()) return out;

        for (String raw : idsCsv.split(",")) {
            String k = raw.trim();
            if (k.isEmpty()) continue;

            final int id;
            try { id = Integer.parseInt(k); } catch (NumberFormatException e) { continue; }

            var metaOpt = registry.meta(id);
            if (metaOpt.isEmpty()) {
                out.put(k, Map.of("itemId", id, "notFound", true));
                continue;
            }

            var meta = metaOpt.get();

            // Fallback name nếu data không có tên
            String name = (meta.getName() != null && !meta.getName().isBlank())
                    ? meta.getName()
                    : fallbackName(meta);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", meta.getId());
            m.put("name", name);                               // NEW
            m.put("type", meta.getItemType().name());             // NEW: EQUIP / CONSUME / ...
            m.put("isEquip", meta.getItemType().name().equals("EQUIP") ? 1 : 0); // NEW (int 0/1)
            m.put("pileLimit", meta.getPileLimit());              // 0 => unlimited (rule C++)
            m.put("isVirtual", meta.isVirtualItem() ? 1 : 0);    // int 0/1
            m.put("sellPrice", meta.getSellPrice());              // NEW
            m.put("invalidTime", meta.getInvalidTime());          // NEW (epoch, nếu 0 là vĩnh viễn)
            m.put("rawTopNode", meta.getRawTopNode());            // NEW (vd: "hujian" cho equip)
            m.put("sourceFile", meta.getSourceFile());            // NEW (vd: "equipment")
            m.put("normalizedId", normalizedId(meta));         // hiện = chính nó; mở rộng sau nếu cần

            out.put(k, m);
        }
        return out;
    }

    private static String fallbackName(ItemMeta meta) {
        // tuỳ ý: ghép topNode + id, để client vẫn có text hiển thị
        String top = meta.getRawTopNode();
        if (top == null || top.isBlank()) top = meta.getItemType().name();
        return top + "#" + meta.getId();
    }

    // Hook để hợp nhất các biến thể (bound/unbound, skin…), mặc định trả chính nó
    private int normalizedId(ItemMeta meta) {
        return meta.getId();
    }

    @PostMapping("/meta/reload")
    public Map<String,Object> reloadMeta() {
        registry.reload(true);
        return Map.of("ok", true, "revision", registry.revision(), "size", registry.size());
    }
}