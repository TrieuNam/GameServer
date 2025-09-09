package com.SouthMillion.config_service.service;

import com.SouthMillion.config_service.config.ConfigProps;
import com.SouthMillion.config_service.config.cache.CacheTier;
import com.SouthMillion.config_service.core.ClasspathConfigRepository;
import com.SouthMillion.config_service.core.ConfigRepository;
import com.SouthMillion.config_service.core.FilesystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.config.ConfigEnvelope;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConfigService {

    private final CacheTier cache;
    private final ConfigRepository repo;

    public ConfigService(List<ConfigRepository> repositories,
                         CacheTier cache,
                         ConfigProps props) {
        String mode = props.mode() == null ? "classpath" : props.mode().trim().toLowerCase();
        this.repo = repositories.stream()
                .filter(r -> ("filesystem".equals(mode) && r instanceof FilesystemConfigRepository)
                        || ("classpath".equals(mode)  && r instanceof ClasspathConfigRepository))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No repository for mode=" + mode));
        this.cache = cache;
    }

    /** Trả về Optional để thể hiện not-found. */
    public Optional<ConfigEnvelope> get(String path) {
        if (path == null || path.isBlank()) return Optional.empty();

        var cached = cache.get(path);
        if (cached.isPresent()) return cached;

        var env = repo.read(path);
        env.ifPresent(e -> cache.put(path, e));
        return env;
    }

    public boolean exists(String path) {
        return repo.exists(path);
    }

    public List<String> list(String prefix) {
        return repo.list(prefix);
    }

    public void evict(String path) {
        cache.evict(path);
    }
}