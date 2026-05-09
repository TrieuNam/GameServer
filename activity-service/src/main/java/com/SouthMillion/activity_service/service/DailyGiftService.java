package com.SouthMillion.activity_service.service;

import com.SouthMillion.activity_service.entity.DailyGift;
import com.SouthMillion.activity_service.repository.DailyGiftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class DailyGiftService {
    @Autowired
    private DailyGiftRepository dailyGiftRepository;

    public CompletableFuture<Optional<DailyGift>> getDailyGift(Long roleId) {
        return dailyGiftRepository.findByRoleIdAsync(roleId);
    }
}