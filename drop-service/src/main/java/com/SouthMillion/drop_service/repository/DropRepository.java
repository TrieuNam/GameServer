package com.SouthMillion.drop_service.repository;

import com.SouthMillion.drop_service.config.AppProperties;
import com.SouthMillion.drop_service.service.client.ConfigFeign;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.SouthMillion.dto.drop.CompiledDrop;
import org.SouthMillion.dto.drop.DropXml;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;

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


    private volatile Set<Integer> knownDropIds = Set.of();
    private final Map<Integer, String> etags = new ConcurrentHashMap<>();

    // ===== Public APIs
    public Set<Integer> listDropIds() {
        ensureListLoaded();
        return knownDropIds;
    }

    public CompiledDrop getCompiled(int dropId) {
        var cached = compiled.getIfPresent(dropId);
        if (cached != null) return cached;

        String etag = etags.get(dropId);
        ResponseEntity<byte[]> resp = cfg.getFile(dropConfigPath(dropId), etag);
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
            resp = cfg.getFile(dropConfigPath(dropId), null);
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

    @Scheduled(fixedDelayString = "#{${app.config.managerReloadSeconds:0} > 0 ? ${app.config.managerReloadSeconds:0} * 1000 : 2147483647}")
    public void periodicReload() {
        if (props.getConfig().getManagerReloadSeconds() <= 0) return;
        reloadList();
    }

    private synchronized void reloadList() {
        try {
            Set<Integer> ids = new HashSet<>(props.getConfig().getKnownDropIds());
            knownDropIds = Set.copyOf(ids);
            // evict cache của id không còn trong list
            compiled.asMap().keySet().removeIf(id -> !knownDropIds.contains(id));
            etags.keySet().removeIf(id -> !knownDropIds.contains(id));
            log.info("drop id list loaded from properties: {} tables", knownDropIds.size());
        } catch (Exception e) {
            log.warn("reload drop list failed: {}", e.toString());
        }
    }

    private String dropConfigPath(int dropId) {
        String pattern = props.getConfig().getDropPathTemplate();
        if (!StringUtils.hasText(pattern)) {
            pattern = "gameworld/drop/%s.xml";
        }
        return pattern.formatted(dropId);
    }
}