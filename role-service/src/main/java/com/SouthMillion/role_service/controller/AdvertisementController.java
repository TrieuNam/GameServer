package com.SouthMillion.role_service.controller;

import com.SouthMillion.role_service.service.AdvertisementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.dto.role.advertisment.AdvertisementDTOs;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdvertisementController {

    private final AdvertisementService svc;

    @PostMapping("/claim")
    public AdvertisementDTOs.AdInfo claim(@RequestBody @Valid AdvertisementDTOs.AdFetchReq req) {
        return svc.claim(req);
    }
}