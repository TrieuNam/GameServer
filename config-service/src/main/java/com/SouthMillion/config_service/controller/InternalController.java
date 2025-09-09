package com.SouthMillion.config_service.controller;

import com.SouthMillion.config_service.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final ConfigService svc;

    /** Xóa cache (L1+L2) cho một path. */
    @PostMapping("/invalidate")
    public ResponseEntity<Map<String, Object>> invalidate(@RequestParam String path) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "ok", false,
                    "error", "path is blank"
            ));
        }

        svc.evict(path);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("path", path);
        return ResponseEntity.ok(out);
    }

    /**
     * Reload: evict trước rồi nạp lại từ repository vào cache.
     * Trả về etag nếu nạp thành công, hoặc notFound=true nếu không tồn tại.
     */
    @PostMapping("/reload")
    public ResponseEntity<Map<String, Object>> reload(@RequestParam("path") String path) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "ok", false,
                    "error", "path is blank"
            ));
        }

        svc.evict(path);
        Optional<ConfigEnvelope> envOpt = svc.get(path);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("path", path);

        if (envOpt.isPresent()) {
            out.put("ok", true);
            out.put("etag", envOpt.get().etag());
        } else {
            out.put("ok", false);
            out.put("notFound", true);
        }

        return ResponseEntity.ok(out);
    }
}