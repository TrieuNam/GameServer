package com.SouthMillion.globalserver_service.service.impl;

import org.SouthMillion.dto.globalserver.AdvertisementDTO;

import java.util.List;

public interface AdvertisementService {
    List<AdvertisementDTO> getAdvertisementList(Long userId);

    void fetchAd(Long userId, Integer seq);
}