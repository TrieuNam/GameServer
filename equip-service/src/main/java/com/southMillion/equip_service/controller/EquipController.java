package com.southMillion.equip_service.controller;

import com.southMillion.equip_service.service.EquipService;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.item.EquipDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equip")
@RequiredArgsConstructor
public class EquipController {
    private final EquipService service;

    @GetMapping("/get")
    public ResponseEntity<EquipDto> getEquip(
            @RequestParam String userId,
            @RequestParam int equipType
    ) {
        EquipDto result = service.getEquip(userId, equipType);
        if (result != null) return ResponseEntity.ok(result);
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<EquipDto>> getAllEquip(
            @RequestParam String userId
    ) {
        List<EquipDto> list = service.getAllEquip(userId);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/update")
    public ResponseEntity<EquipDto> addOrUpdateEquip(@RequestBody EquipDto dto) {
        EquipDto result = service.addOrUpdateEquip(dto);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteEquip(
            @RequestParam String userId,
            @RequestParam int equipType
    ) {
        boolean ok = service.deleteEquip(userId, equipType);
        if (ok) return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }
}