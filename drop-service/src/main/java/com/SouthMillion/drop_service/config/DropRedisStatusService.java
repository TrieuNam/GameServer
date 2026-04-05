package com.SouthMillion.drop_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DropRedisStatusService {

    private final AppProperties props;
    private final StringRedisTemplate redis;

    @Value("${drop.config.redis-enabled:true}")
    private boolean redisEnabled;
    @Value("${drop.config.allow-remote-fallback-on-miss:false}")
    private boolean allowRemoteFallbackOnMiss;
    @Value("${drop.config.health-sample-limit:20}")
    private int healthSampleLimit;

    public DropRedisStatus snapshot() {
        return snapshot(healthSampleLimit);
    }

    public DropRedisStatus snapshot(int rangeLimit) {
        List<Integer> knownIds = props.getConfig().resolveKnownDropIds();
        boolean strictRedisFirst = !allowRemoteFallbackOnMiss;

        if (!redisEnabled) {
            return new DropRedisStatus(false, strictRedisFirst, knownIds.size(), 0, knownIds.size(), null,
                    List.of(), false, "drop.config.redis-enabled=false");
        }

        if (knownIds.isEmpty()) {
            return new DropRedisStatus(true, strictRedisFirst, 0, 0, 0, null,
                    List.of(), false, "No known drop ids/ranges configured");
        }

        List<Integer> missing = findMissingDropIds(knownIds);

        int cachedCount = knownIds.size() - missing.size();
        Integer firstMissingId = missing.isEmpty() ? null : missing.get(0);
        List<String> missingRanges = summarizeRanges(missing, Math.max(1, rangeLimit));
        boolean ready = missing.isEmpty();
        String reason = ready ? "All known drop XML present in Redis" : "Missing drop XML in Redis";

        return new DropRedisStatus(true, strictRedisFirst, knownIds.size(), cachedCount, missing.size(),
                firstMissingId, missingRanges, ready, reason);
    }

    public List<Integer> findMissingDropIds(List<Integer> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }
        List<String> keys = candidateIds.stream().map(this::toRedisKey).toList();
        List<String> values = redis.opsForValue().multiGet(keys);

        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < candidateIds.size(); i++) {
            String cached = (values != null && i < values.size()) ? values.get(i) : null;
            if (!StringUtils.hasText(cached)) {
                missing.add(candidateIds.get(i));
            }
        }
        return missing;
    }

    public List<Integer> parseDropSelectors(List<String> ids, List<String> ranges) {
        Set<Integer> out = new LinkedHashSet<>();
        if (ids != null) {
            for (String raw : ids) {
                if (!StringUtils.hasText(raw)) continue;
                out.add(Integer.parseInt(raw.trim()));
            }
        }
        if (ranges != null) {
            for (String raw : ranges) {
                if (!StringUtils.hasText(raw)) continue;
                String token = raw.trim();
                if (token.contains("-")) {
                    String[] parts = token.split("-", 2);
                    int start = Integer.parseInt(parts[0].trim());
                    int end = Integer.parseInt(parts[1].trim());
                    if (end < start) {
                        int tmp = start;
                        start = end;
                        end = tmp;
                    }
                    for (int id = start; id <= end; id++) {
                        out.add(id);
                    }
                } else {
                    out.add(Integer.parseInt(token));
                }
            }
        }
        return new ArrayList<>(out);
    }

    private List<String> summarizeRanges(List<Integer> ids, int limit) {
        List<String> out = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }

        int start = ids.get(0);
        int prev = start;
        for (int i = 1; i < ids.size(); i++) {
            int cur = ids.get(i);
            if (cur == prev + 1) {
                prev = cur;
                continue;
            }
            out.add(formatRange(start, prev));
            if (out.size() >= limit) {
                return out;
            }
            start = cur;
            prev = cur;
        }
        if (out.size() < limit) {
            out.add(formatRange(start, prev));
        }
        return out;
    }

    private String formatRange(int start, int end) {
        return start == end ? String.valueOf(start) : start + "-" + end;
    }

    private String toRedisKey(int dropId) {
        return toRedisKey(props.getConfig().getDropPathTemplate().formatted(dropId));
    }

    private String toRedisKey(String path) {
        return "cfg:file:" + path.replace('/', ':');
    }

    public record DropRedisStatus(
            boolean redisEnabled,
            boolean strictRedisFirst,
            int knownDropCount,
            int cachedCount,
            int missingCount,
            Integer firstMissingId,
            List<String> missingRanges,
            boolean ready,
            String reason
    ) {}
}
