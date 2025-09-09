package com.SouthMillion.config_service.controller;

import com.SouthMillion.config_service.config.ConfigProps;
import com.SouthMillion.config_service.service.ConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.SouthMillion.exception.NotFoundException;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService svc;
    private final ConfigProps props;
    private final ObjectMapper om;

    // ===== GET /api/config/file
    @GetMapping({"/api/config/file", "/api/config/file/{*path}"})
    public ResponseEntity<byte[]> getFile(
            @PathVariable(value = "path", required = false) String pathVar,
            @RequestParam(value = "path", required = false) String pathParam,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {

        String path = (pathVar != null && !pathVar.isBlank()) ? pathVar : pathParam;
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var opt = svc.get(path);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        var env = opt.get();
        String quotedEtag = quote(env.etag());

        HttpHeaders headers = new HttpHeaders();
        applyCommonHeaders(headers, env, quotedEtag);

        if (matchesIfNoneMatch(ifNoneMatch, quotedEtag)) {
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_MODIFIED);
        }

        MediaType ct = mediaTypeSafely(env.contentType());
        if (ct != null) headers.setContentType(ct);

        return new ResponseEntity<>(env.bytes(), headers, HttpStatus.OK);
    }


    // HEAD giống hệt GET về routing và ETag/304, nhưng không gửi body
    @RequestMapping(path = {"/api/config/file", "/api/config/file/{*path}"}, method = RequestMethod.HEAD)
    public ResponseEntity<Void> headFile(
            @PathVariable(value = "path", required = false) String pathVar,
            @RequestParam(value = "path", required = false) String pathParam,
            @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {

        String path = (pathVar != null && !pathVar.isBlank()) ? pathVar : pathParam;
        if (path == null || path.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        var opt = svc.get(path);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        var env = opt.get();
        String quotedEtag = quote(env.etag());

        HttpHeaders headers = new HttpHeaders();
        applyCommonHeaders(headers, env, quotedEtag);

        MediaType ct = mediaTypeSafely(env.contentType());
        if (ct != null) headers.setContentType(ct);
        if (env.bytes() != null) headers.setContentLength(env.bytes().length);

        if (matchesIfNoneMatch(ifNoneMatch, quotedEtag)) {
            return new ResponseEntity<>(null, headers, HttpStatus.NOT_MODIFIED);
        }
        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }


    // ===== GET /api/config/batch
    @GetMapping("/api/config/batch")
    public ResponseEntity<Map<String, Object>> batch(@RequestParam List<String> path,
                                                     @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        Map<String, Object> out = new LinkedHashMap<>();

        for (String p : path) {
            var opt = svc.get(p);
            Map<String, Object> dto = new LinkedHashMap<>();

            if (opt.isEmpty()) {
                dto.put("notFound", true);
                out.put(p, dto);
                continue;
            }

            var env = opt.get();
            String quotedEtag = quote(env.etag());
            boolean notModified = matchesIfNoneMatch(ifNoneMatch, quotedEtag);

            dto.put("etag", env.etag());
            dto.put("notModified", notModified);
            dto.put("contentType", env.contentType());
            dto.put("lastModified", env.lastModified() != null ? env.lastModified().toEpochMilli() : null);

            if (!notModified) {
                Object data;
                try {
                    if (looksLikeJson(env.contentType(), env.bytes())) {
                        data = om.readTree(env.bytes());
                    } else {
                        data = new String(env.bytes(), StandardCharsets.UTF_8);
                    }
                } catch (Exception ex) {
                    data = new String(env.bytes(), StandardCharsets.UTF_8);
                }
                dto.put("data", data);
            }

            out.put(p, dto);
        }

        return ResponseEntity.ok(out);
    }

    // ===== Helpers

    private void applyCommonHeaders(HttpHeaders h, ConfigEnvelope env, String quotedEtag) {
        if (quotedEtag != null) h.set(HttpHeaders.ETAG, quotedEtag);
        if (env.lastModified() != null) {
            h.setLastModified(env.lastModified().toEpochMilli());
        }
        if (props.publicCacheControl() != null && !props.publicCacheControl().isBlank()) {
            h.set(HttpHeaders.CACHE_CONTROL, props.publicCacheControl());
        } else {
            h.setCacheControl(CacheControl.maxAge(Duration.ofSeconds(60)));
        }
    }

    private static String quote(String etag) {
        if (etag == null) return null;
        if (etag.length() >= 2 && etag.charAt(0) == '"' && etag.charAt(etag.length() - 1) == '"') {
            return etag;
        }
        return '"' + etag + '"';
    }

    /**
     * So khớp If-None-Match (có thể là danh sách ETag, có thể chứa tiền tố W/).
     */
    private static boolean matchesIfNoneMatch(String ifNoneMatchHeader, String quotedEtag) {
        if (ifNoneMatchHeader == null || quotedEtag == null) return false;

        // Normalize server etag -> không quote, bỏ W/
        String target = stripQuotes(quotedEtag);
        target = stripWeakPrefix(target);

        List<String> tokens = Arrays.stream(ifNoneMatchHeader.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        for (String t : tokens) {
            if ("*".equals(t)) return true; // wildcard
            String cand = stripQuotes(stripWeakPrefix(t));
            if (cand.equals(target)) return true;
        }
        return false;
    }

    private static String stripWeakPrefix(String s) {
        if (s == null) return null;
        return s.startsWith("W/") ? s.substring(2).trim() : s;
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        int len = s.length();
        if (len >= 2 && s.charAt(0) == '"' && s.charAt(len - 1) == '"') {
            return s.substring(1, len - 1);
        }
        return s;
    }

    private static boolean looksLikeJson(String contentType, byte[] bytes) {
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) return true;
        if (bytes == null || bytes.length == 0) return false;
        byte b = firstNonWs(bytes);
        return b == '{' || b == '[';
    }

    private static byte firstNonWs(byte[] bytes) {
        for (byte b : bytes) {
            if (!Character.isWhitespace((char) b)) return b;
        }
        return 0;
    }

    private static MediaType mediaTypeSafely(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return MediaType.parseMediaType(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}