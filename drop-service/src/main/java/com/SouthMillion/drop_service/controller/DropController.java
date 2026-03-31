package com.SouthMillion.drop_service.controller;

import com.SouthMillion.drop_service.repository.DropRepository;
import com.SouthMillion.drop_service.service.DropRoller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.drop.RollRequest;
import org.SouthMillion.dto.drop.RollResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/drop")
@RequiredArgsConstructor
public class DropController {
    private final DropRepository repo;
    private final DropRoller roller;

    @GetMapping("/tables")
    public Set<Integer> tables() {
        return repo.listDropIds();
    }

    @PostMapping("/roll")
    public RollResult roll(@Valid @RequestBody RollRequest req) {
        return roller.roll(req);
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