package com.SouthMillion.box_service.config;

import com.SouthMillion.box_service.service.client.ConfigFeign;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class GiftConfigResolver {

    private final ConfigFeign cfg;
    private final BoxProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private volatile Integer cachedId;

    public int resolveStarterGiftItemId() {

        // 1) Ưu tiên cấu hình cố định
        if (props.getInitItemId() > 0) {
            return Math.toIntExact(props.getInitItemId());
        }
        if (cachedId != null) return cachedId;

        // Lấy file config theo REL dưới "config/" => "gameworld/item/gift.json"
        // Dùng force=1 để luôn trả 200 + body (tránh 304 Not Modified).
        var resp = cfg.byPath("gameworld/item/gift.json", null, 1);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Không lấy được gift.json, status=" + resp.getStatusCode());
        }

        Map<String, Object> root;
        try {
            root = om.readValue(resp.getBody(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Parse gift.json thất bại: " + e.getMessage(), e);
        }

        @SuppressWarnings("unchecked")
        var defGift = (java.util.List<java.util.Map<String, Object>>) root.get("DefGift");
        if (defGift == null || defGift.isEmpty()) {
            throw new IllegalStateException("gift.json thiếu DefGift");
        }

        String want = props.getStarterGiftName();
        if (want == null || want.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình box.starter-gift-name hoặc box.init-item-id");
        }

        Map<String, Object> match = defGift.stream()
                .filter(it -> want.equals(String.valueOf(it.get("name"))))
                .findFirst()
                .orElseGet(() -> defGift.stream()
                        .filter(it -> String.valueOf(it.get("name")).contains(want))
                        .findFirst()
                        .orElse(null));

        if (match == null) {
            throw new IllegalStateException("Không tìm thấy gift với tên: " + want);
        }

        Object idVal = match.get("id");
        int id;
        try {
            // id có thể là số hoặc chuỗi
            id = (idVal instanceof Number n) ? n.intValue() : Integer.parseInt(String.valueOf(idVal));
        } catch (Exception e) {
            throw new IllegalStateException("gift.id không hợp lệ: " + idVal);
        }
        cachedId = id;
        return id;
    }
}