package com.SouthMillion.user_service.service;

import org.springframework.stereotype.Service;

@Service
public interface UserResourceService {
    void deductResource(Long userId, Integer itemId, Integer amount);
    void addItem(Long userId, Integer itemId, Integer amount);
    boolean hasEnough(Long userId, Integer itemId, Integer amount);
}
