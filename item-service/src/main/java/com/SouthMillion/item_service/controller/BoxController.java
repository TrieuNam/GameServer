package com.SouthMillion.item_service.controller;

import com.SouthMillion.item_service.entity.BoxRecord;
import com.SouthMillion.item_service.service.BoxService;
import org.SouthMillion.dto.item.Box.BoxInfoDTO;
import org.SouthMillion.dto.item.Box.BoxOperationDTO;
import org.SouthMillion.dto.item.Box.BoxRecordDTO;
import org.SouthMillion.dto.item.Box.BoxSetDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/box")
public class BoxController {

    @Autowired
    private BoxService boxService;

    @PostMapping("/open")
    public ResponseEntity<BoxRecordDTO> openBox(@RequestBody BoxOperationDTO dto, @RequestParam Long userId) {
        return ResponseEntity.ok(boxService.openBox(userId, dto));
    }

    @PostMapping("/setting")
    public ResponseEntity<Void> saveSetting(@RequestBody BoxSetDTO setting, @RequestParam Long userId) {
        boxService.saveBoxSetting(userId, setting);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/setting")
    public ResponseEntity<BoxSetDTO> getSetting(@RequestParam Long userId) {
        return ResponseEntity.ok(boxService.getBoxSetting(userId));
    }

    @PostMapping("/sell")
    public ResponseEntity<BoxRecordDTO> sellBox(@RequestParam Long userId, @RequestParam Integer boxId) {
        return ResponseEntity.ok(boxService.sellBoxItem(userId, boxId));
    }

    @GetMapping("/last")
    public ResponseEntity<BoxRecordDTO> getLastBox(@RequestParam Long userId) {
        return ResponseEntity.ok(boxService.getLastBoxInfo(userId));
    }

    @PostMapping("/handle-request")
    public BoxInfoDTO handleBoxRequest(@RequestParam Long userId,
                                                @RequestParam int reqType,
                                                @RequestParam int param) {
        BoxInfoDTO dto = boxService.handleBoxRequest(userId, reqType, param);
        return dto;
    }
}
