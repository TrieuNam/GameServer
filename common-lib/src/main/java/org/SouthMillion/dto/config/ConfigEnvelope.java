package org.SouthMillion.dto.config;

public record ConfigEnvelope<T>(
        String key,
        String revision,
        long lastModifiedEpoch,
        String etag,
        T content
) {}