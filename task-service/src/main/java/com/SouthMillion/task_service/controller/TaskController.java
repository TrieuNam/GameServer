package com.SouthMillion.task_service.controller;

import com.SouthMillion.task_service.service.TaskBatchService;
import com.SouthMillion.task_service.service.TaskService;
import com.SouthMillion.task_service.dto.TaskActionDto;
import com.SouthMillion.task_service.dto.TaskResultDto;
import com.SouthMillion.task_service.model.Task;
import com.SouthMillion.task_service.repository.TaskRepository;
import org.SouthMillion.dto.TaskDto;
import org.SouthMillion.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskService taskService;

    @GetMapping("/{id}")
    public TaskDto getTaskById(@PathVariable Long id) {
        Task task = taskRepository.findById(id).orElseThrow();
        UserDto user = taskService.getUser(task.getUserId());
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setName(task.getName());
        dto.setStatus(task.getStatus());
        dto.setUserId(user.getId());
        return dto;
    }

    @Autowired
    private TaskBatchService batchService;

    @PostMapping("/batch")
    public List<TaskResultDto> handleBatch(
            @RequestBody List<TaskActionDto> actions,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User user) {
        return batchService.handleBatch(actions, user);
    }
}