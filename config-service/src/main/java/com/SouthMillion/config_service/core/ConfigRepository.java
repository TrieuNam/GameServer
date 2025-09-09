package com.SouthMillion.config_service.core;

import org.SouthMillion.dto.config.ConfigEnvelope;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ConfigRepository {
    Optional<ConfigEnvelope> read(String path);
    boolean exists(String path);
    List<String> list(String prefix);
}