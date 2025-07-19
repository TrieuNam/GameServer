package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.client.ConfigFeignClient;
import com.SouthMillion.task_service.client.UserServiceFeignClient;
import com.SouthMillion.task_service.repository.TaskRepository;
import org.SouthMillion.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {
    @Autowired
    private UserServiceFeignClient userFeign;
    @Autowired
    private ConfigFeignClient configFeign;

    @Autowired
    private TaskRepository repo;

    @Cacheable("pagodaConfig")
    public Object getPagodaConfig() {
        return configFeign.getConfig("gumo_pagoda");
    }

    public UserDto getUser(Long userId) {
        return userFeign.getUserById(userId);
    }


}
