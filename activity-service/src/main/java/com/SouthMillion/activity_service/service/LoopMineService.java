package com.SouthMillion.activity_service.service;

import com.SouthMillion.activity_service.entity.LoopMine;
import com.SouthMillion.activity_service.repository.LoopMineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoopMineService {

    @Autowired
    private LoopMineRepository loopMineRepository;

    public Page<LoopMine> getBattleLogs(Long roleId, int page, int size) {
        // enforce result cap
        if (size > 50) size = 50;
        return loopMineRepository.findByRoleId(roleId, PageRequest.of(page, size));
    }
}