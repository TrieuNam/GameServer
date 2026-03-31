package com.SouthMillion.box_service.config;

import com.SouthMillion.box_service.service.client.ConfigFeign;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;


import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class UnpackConfigCache {

    private final ConfigFeign cfg;
    private final AppProperties props;
    private final ObjectMapper om = new ObjectMapper();

    private final AtomicReference<String> etag = new AtomicReference<>();
    @Getter private volatile Map<String, Object> raw = Map.of();

    // ======= Chống spam gọi & TTL refresh =======
    private static final long REFRESH_INTERVAL_MS = 30_000; // tuỳ chỉnh
    private volatile long lastCheckMs = 0L;
    private final AtomicBoolean loading = new AtomicBoolean(false);

    /**
     * Ép reload ngay: KHÔNG gửi If-None-Match, server luôn trả 200 + body (nếu OK).
     */
    public void forceReload() {
        callServer(/*force*/ 1, /*respectTtl*/ false);
    }

    /**
     * Đảm bảo dữ liệu đã tải/đã tươi theo TTL.
     * Gọi trước khi đọc các trường randomLevel/randomColor/...
     */
    public void ensureLoaded() {
        callServer(/*force*/ 0, /*respectTtl*/ true);
    }

    // ======= Public readers =======

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> randomLevel() {
        ensureLoaded();
        return (List<Map<String, String>>) raw.getOrDefault("random_level", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> randomColor() {
        ensureLoaded();
        return (List<Map<String, Object>>) raw.getOrDefault("random_color", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> other() {
        ensureLoaded();
        return (List<Map<String, String>>) raw.getOrDefault("other", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> colorAtt() {
        ensureLoaded();
        return (List<Map<String, String>>) raw.getOrDefault("color_att", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> fixedReward() {
        ensureLoaded();
        return (List<Map<String, String>>) raw.getOrDefault("fixed_reward", List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, String>> shizhuangRate() {
        ensureLoaded();
        return (List<Map<String, String>>) raw.getOrDefault("shizhuang_rate", List.of());
    }

    // ======= Utils =======

    /**
     * Nếu force=1: không gửi If-None-Match (truyền null) để tránh 304.
     * Nếu force=0: gửi ETag hiện có, bắt 304 để giữ cache.
     * respectTtl=true: chỉ gọi server nếu đã quá REFRESH_INTERVAL_MS kể từ lần check gần nhất.
     */
    private void callServer(int force, boolean respectTtl) {
        final long now = System.currentTimeMillis();
        if (respectTtl && (now - lastCheckMs) < REFRESH_INTERVAL_MS) {
            return; // còn trong TTL, khỏi gọi server
        }

        // Single-flight: nếu đã có thread khác đang tải, thread này bỏ qua
        if (!loading.compareAndSet(false, true)) {
            return;
        }

        try {
            lastCheckMs = now; // đánh dấu đã kiểm tra (kể cả 304 để không spam)
            String path = Objects.requireNonNull(
                    props.getConfig().getUnpackPath(),
                    "config.unpackPath must not be null"
            );

            String ifNoneMatch = (force == 1) ? null : etag.get();
            ResponseEntity<byte[]> resp = cfg.getFile(path, ifNoneMatch);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                String json = new String(resp.getBody(), StandardCharsets.UTF_8);
                Map<String, Object> parsed = om.readValue(json, new TypeReference<Map<String, Object>>() {});
                // Chỉ publish khi parse OK
                raw = parsed;

                String newTag = resp.getHeaders().getETag();
                if (newTag != null) {
                    etag.set(newTag); // LƯU Ý: giữ nguyên định dạng (thường có dấu ")
                }
            }
            // 2xx nhưng body null -> giữ cache cũ

        } catch (FeignException ex) {
            if (ex.status() == 304) {
                // Not Modified -> cache cũ vẫn dùng, OK
                return;
            }
            // Các HTTP lỗi khác -> giữ cache cũ; tuỳ chọn log:
            // log.warn("config byPath error {}: {}", ex.status(), ex.getMessage());

        } catch (Exception e) {
            // Parse lỗi hoặc lỗi khác -> giữ cache cũ; tuỳ chọn log:
            // log.warn("config parse/other error: {}", e.toString());

        } finally {
            loading.set(false);
        }
    }

    /** Xoá cache & ETag (tuỳ chọn, nếu cần). */
    public void clear() {
        etag.set(null);
        raw = Map.of();
        lastCheckMs = 0L;
    }
}