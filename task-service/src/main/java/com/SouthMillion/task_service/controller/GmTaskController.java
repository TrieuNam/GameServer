package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.GmTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gm")
public class GmTaskController {
    @Autowired
    private GmTaskService gmTaskService;

    @PostMapping("/finish")
    public void finishTask(@RequestParam String userId, @RequestParam int taskId) {
        gmTaskService.finishTask(userId, taskId);
    }
}