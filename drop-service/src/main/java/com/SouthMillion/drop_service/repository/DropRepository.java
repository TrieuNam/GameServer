package com.SouthMillion.drop_service.repository;

import com.SouthMillion.drop_service.config.AppProperties;
import com.SouthMillion.drop_service.service.client.ConfigFeign;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.drop.CompiledDrop;
import org.SouthMillion.dto.drop.DropXml;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class DropRepository {
    private final ConfigFeign cfg;
    private final AppProperties props;
    private final Cache<Integer, CompiledDrop> compiled; // inject từ DropBeans
    private final XmlMapper xml;                         // inject từ DropBeans

    // L1 cache compiled tables (TTL)
    @Bean
    public Cache<Integer, CompiledDrop> compiledCache(AppProperties p) {
        return Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(p.getCache().getCompiledTtlMinutes()))
                .maximumSize(p.getCache().getCompiledMaxSize())
                .build();
    }


    private volatile Set<Integer> knownDropIds = Set.of();
    private final Map<Integer, String> etags = new ConcurrentHashMap<>();

    // ===== Public APIs
    public Set<Integer> listDropIds() {
        ensureListLoaded();
        return knownDropIds;
    }

    public CompiledDrop getCompiled(int dropId) {
        ensureListLoaded();
        var cached = compiled.getIfPresent(dropId);
        if (cached != null) return cached;

        String etag = etags.get(dropId);
        ResponseEntity<byte[]> resp = cfg.drop(String.valueOf(dropId), etag);
        if (resp.getStatusCode().is2xxSuccessful()) {
            CompiledDrop cd = parse(resp.getBody());
            compiled.put(dropId, cd);
            var newTag = resp.getHeaders().getETag();
            if (StringUtils.hasText(newTag)) etags.put(dropId, newTag);
            return cd;
        }
        if (resp.getStatusCode().value() == 304) {
            var existed = compiled.getIfPresent(dropId);
            if (existed != null) return existed;
            // hiếm khi vừa 304 vừa chưa có cache (race) -> fetch lại không gắn If-None-Match
            resp = cfg.drop(String.valueOf(dropId), null);
            CompiledDrop cd = parse(resp.getBody());
            compiled.put(dropId, cd);
            var newTag = resp.getHeaders().getETag();
            if (StringUtils.hasText(newTag)) etags.put(dropId, newTag);
            return cd;
        }
        throw new IllegalArgumentException("Cannot load drop "+dropId+" HTTP="+resp.getStatusCode());
    }

    // ===== Helpers
    private CompiledDrop parse(byte[] xmlBytes) {
        try {
            DropXml x = xml.readValue(xmlBytes, DropXml.class);
            return new CompiledDrop(x);
        } catch (Exception e) {
            throw new RuntimeException("Parse drop xml failed: "+e.getMessage(), e);
        }
    }

    private void ensureListLoaded() {
        if (knownDropIds.isEmpty()) reloadList();
    }

    @Scheduled(fixedDelayString = "#{${app.config.managerReloadSeconds:0} * 1000}")
    public void periodicReload() {
        if (props.getConfig().getManagerReloadSeconds() <= 0) return;
        reloadList();
    }

    private synchronized void reloadList() {
        try {
            List<String> files = cfg.list("drop", 0, 100000);
            Set<Integer> ids = new HashSet<>();
            for (String f : files) {
                // ví dụ "2000.xml" => 2000
                String s = f;
                int dot = s.indexOf('.');
                if (dot > 0) s = s.substring(0, dot);
                try { ids.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            }
            knownDropIds = Set.copyOf(ids);
            // evict cache của id không còn trong list
            compiled.asMap().keySet().removeIf(id -> !knownDropIds.contains(id));
            etags.keySet().removeIf(id -> !knownDropIds.contains(id));
            log.info("drop list loaded: {} tables", knownDropIds.size());
        } catch (Exception e) {
            log.warn("reload drop list failed: {}", e.toString());
        }
    }
}