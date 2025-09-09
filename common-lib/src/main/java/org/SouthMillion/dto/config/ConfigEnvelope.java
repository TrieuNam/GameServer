package org.SouthMillion.dto.config;


import java.time.Instant;

public record ConfigEnvelope(
        byte[]  bytes,
        String  etag,
        String  contentType,
        Instant lastModified
) {}