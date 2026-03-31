package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.TaskDefinitionProvider;
import lombok.RequiredArgsConstructor;
import org.SouthMillion.api.GenericResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task/config")
@RequiredArgsConstructor
public class TaskConfigAdminController {

    private final TaskDefinitionProvider taskDefinitionProvider;

    @PostMapping("/reload")
    public GenericResult<TaskDefinitionProvider.TaskConfigStatus> reload() {
        return GenericResult.ok(taskDefinitionProvider.manualReload());
    }

    @GetMapping("/status")
    public GenericResult<TaskDefinitionProvider.TaskConfigStatus> status() {
        return GenericResult.ok(taskDefinitionProvider.status());
    }
}

