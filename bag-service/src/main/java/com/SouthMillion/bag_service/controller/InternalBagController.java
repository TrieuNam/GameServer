package com.SouthMillion.bag_service.controller;

import com.SouthMillion.bag_service.service.BagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.bag.BagDTOs;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/bag")
@RequiredArgsConstructor
public class InternalBagController {
    private final BagService svc;

    @PostMapping("/add")
    public BagDTOs.AddItemResp add(@Valid @RequestBody BagDTOs.AddItemReq req) {
        return svc.addItems(req);
    }

    @PostMapping("/consume")
    public BagDTOs.OkResp consume(@Valid @RequestBody BagDTOs.ConsumeReq req) {
        return svc.consume(req);
    }
}