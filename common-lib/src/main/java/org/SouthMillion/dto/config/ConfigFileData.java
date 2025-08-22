package org.SouthMillion.dto.config;

public record ConfigFileData(
        String key, String revision, long lastModifiedEpoch,
        String etag, String contentType, byte[] content) {}