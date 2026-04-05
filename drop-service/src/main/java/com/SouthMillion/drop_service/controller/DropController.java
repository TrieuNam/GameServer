package com.SouthMillion.drop_service.controller;

import com.SouthMillion.drop_service.config.DropConfigRedisPreloader;
import com.SouthMillion.drop_service.config.DropRedisStatusService;
import com.SouthMillion.drop_service.repository.DropRepository;
import com.SouthMillion.drop_service.service.DropRoller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.drop.RollRequest;
import org.SouthMillion.dto.drop.RollResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/drop")
@RequiredArgsConstructor
public class DropController {
    private final DropRepository repo;
    private final DropRoller roller;
    private final DropRedisStatusService dropRedisStatusService;
    private final DropConfigRedisPreloader dropConfigRedisPreloader;

    @GetMapping("/tables")
    public Set<Integer> tables() {
        return repo.listDropIds();
    }

    @PostMapping("/roll")
    public RollResult roll(@Valid @RequestBody RollRequest req) {
        return roller.roll(req);
    }

    @GetMapping("/redis-status")
    public ResponseEntity<DropRedisStatusService.DropRedisStatus> redisStatus(
            @RequestParam(defaultValue = "20") int limit) {
        var status = dropRedisStatusService.snapshot(Math.max(1, Math.min(limit, 100)));
        return ResponseEntity.status(status.ready() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(status);
    }

    @PostMapping("/rewarm")
    public ResponseEntity<DropConfigRedisPreloader.RewarmResult> rewarm(
            @RequestParam(defaultValue = "false") boolean missingOnly,
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestParam(required = false) List<String> ids,
            @RequestParam(required = false) List<String> ranges) {
        DropConfigRedisPreloader.RewarmResult result;
        if (missingOnly) {
            result = dropConfigRedisPreloader.rewarmMissingDrops();
        } else {
            List<Integer> targetIds = dropRedisStatusService.parseDropSelectors(ids, ranges);
            if (targetIds.isEmpty()) {
                targetIds = repo.listDropIds().stream().sorted().toList();
            }
            result = dropConfigRedisPreloader.rewarmTargetedDrops(targetIds, forceRefresh);
        }
        HttpStatus status = (result.failedCount() == 0 && result.ready()) ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(result);
    }

    // QA: mô phỏng tần suất
    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@RequestParam int dropId, @RequestParam(defaultValue = "100000") int n) {
        var compiled = repo.getCompiled(dropId);
        var rnd = new java.util.Random(12345L);
        var count = new java.util.HashMap<Integer,Integer>();
        for (int i=0;i<n;i++) {
            var r = compiled.pick(rnd);
            count.merge(r.itemId(), 1, Integer::sum);
        }
        return ResponseEntity.ok(Map.of("dropId", dropId, "n", n, "freq", count));
    }
}