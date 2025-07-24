package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.ShiZhuangService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.ShiZhuang.ShiZhuangDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shizhuang")
@RequiredArgsConstructor
public class ShiZhuangController {
    private final ShiZhuangService service;

    @GetMapping("/get")
    public ResponseEntity<ShiZhuangDto> get(@RequestParam String userId, @RequestParam int id) {
        ShiZhuangDto res = service.get(userId, id);
        if (res == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ShiZhuangDto>> list(@RequestParam String userId) {
        return ResponseEntity.ok(service.getAll(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<ShiZhuangDto> add(@RequestBody ShiZhuangDto dto) {
        return ResponseEntity.ok(service.addOrUpdate(dto));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam String userId, @RequestParam int id) {
        boolean ok = service.delete(userId, id);
        return ok ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}