package com.SouthMillion.user_service.service.impl;

import com.SouthMillion.user_service.enity.UserItemEntity;
import com.SouthMillion.user_service.enity.UserItemKey;
import com.SouthMillion.user_service.repository.UserItemRepository;
import com.SouthMillion.user_service.service.UserResourceService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserResourceServiceImpl implements UserResourceService {

    @Autowired
    private UserItemRepository userItemRepo;

    /**
     * Trừ tài nguyên khỏi user (số lượng không âm, ném exception nếu không đủ)
     */
    @Transactional
    @Override
    public void deductResource(Long userId, Integer itemId, Integer amount) {
        UserItemKey key = new UserItemKey(userId, itemId);
        UserItemEntity entity = userItemRepo.findById(key)
                .orElseThrow(() -> new RuntimeException("User không có vật phẩm này!"));

        if (entity.getCount() < amount) {
            throw new RuntimeException("Không đủ số lượng để trừ!");
        }

        entity.setCount(entity.getCount() - amount);
        userItemRepo.save(entity);
    }

    /**
     * Cộng tài nguyên cho user
     */
    @Transactional
    @Override
    public void addItem(Long userId, Integer itemId, Integer amount) {
        UserItemKey key = new UserItemKey(userId, itemId);
        UserItemEntity entity = userItemRepo.findById(key)
                .orElse(null);

        if (entity == null) {
            entity = new UserItemEntity();
            entity.setId(key);
            entity.setCount(amount);
        } else {
            entity.setCount(entity.getCount() + amount);
        }
        userItemRepo.save(entity);
    }

    /**
     * Kiểm tra user có đủ số lượng tài nguyên hay không
     */
    @Override
    public boolean hasEnough(Long userId, Integer itemId, Integer amount) {
        UserItemKey key = new UserItemKey(userId, itemId);
        return userItemRepo.findById(key)
                .map(e -> e.getCount() >= amount)
                .orElse(false);
    }
}
