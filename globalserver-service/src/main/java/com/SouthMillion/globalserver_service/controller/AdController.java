package com.SouthMillion.globalserver_service.controller;

import com.SouthMillion.globalserver_service.service.impl.AdvertisementService;
import org.SouthMillion.dto.globalserver.AdvertisementDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ad")
public class AdController {
    @Autowired
    private AdvertisementService adService;

    // Lấy tất cả quảng cáo của user (có thể 1 hoặc nhiều, tuỳ bảng)
    @GetMapping("/info/{userId}")
    public List<AdvertisementDTO> info(@PathVariable Long userId) {
        return adService.getAdvertisementList(userId);
    }

    // Nhận thưởng quảng cáo (nếu nhiều quảng cáo thì truyền thêm seq)
    @PostMapping("/fetch/{userId}/{seq}")
    public void fetch(@PathVariable Long userId, @PathVariable Integer seq) {
        adService.fetchAd(userId, seq);
    }
}