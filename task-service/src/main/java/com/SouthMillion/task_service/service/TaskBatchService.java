package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.dto.TaskActionDto;
import com.SouthMillion.task_service.dto.TaskResultDto;
import com.SouthMillion.task_service.model.Task;
import com.SouthMillion.task_service.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskBatchService {

    private final TaskRepository taskRepository;

    public List<TaskResultDto> handleBatch(List<TaskActionDto> actions, UserDetails user) {
        List<TaskResultDto> results = new ArrayList<>();
        Long userId = getUserId(user);

        for (TaskActionDto action : actions) {
            TaskResultDto result = new TaskResultDto();
            result.setType(action.getType());
            result.setTaskId(action.getTaskId());

            try {
                switch (action.getType()) {
                    case "COMPLETE_TASK" -> results.add(completeTask(action, userId));
                    case "CLAIM_REWARD"  -> results.add(claimReward(action, userId));
                    default -> {
                        result.setSuccess(false);
                        result.setMessage("Unknown action type: " + action.getType());
                        results.add(result);
                    }
                }
            } catch (Exception ex) {
                result.setSuccess(false);
                result.setMessage("Exception: " + ex.getMessage());
                results.add(result);
            }
        }
        return results;
    }

    private TaskResultDto completeTask(TaskActionDto action, Long userId) {
        TaskResultDto result = new TaskResultDto();
        result.setType("COMPLETE_TASK");
        result.setTaskId(action.getTaskId());

        Optional<Task> taskOpt = taskRepository.findById(action.getTaskId());
        if (taskOpt.isEmpty() || !Objects.equals(taskOpt.get().getUserId(), userId)) {
            result.setSuccess(false);
            result.setMessage("Task not found or not owned by user");
            return result;
        }

        Task task = taskOpt.get();
        if ("COMPLETED".equals(task.getStatus())) {
            result.setSuccess(false);
            result.setMessage("Task already completed");
        } else {
            task.setStatus("COMPLETED");
            taskRepository.save(task);
            result.setSuccess(true);
            result.setMessage("Task completed");
            result.setData(Map.of("reward", 100)); // trả về thưởng nếu cần
        }
        return result;
    }

    private TaskResultDto claimReward(TaskActionDto action, Long userId) {
        TaskResultDto result = new TaskResultDto();
        result.setType("CLAIM_REWARD");
        result.setTaskId(action.getTaskId());

        Optional<Task> taskOpt = taskRepository.findById(action.getTaskId());
        if (taskOpt.isEmpty() || !Objects.equals(taskOpt.get().getUserId(), userId)) {
            result.setSuccess(false);
            result.setMessage("Task not found or not owned by user");
            return result;
        }

        Task task = taskOpt.get();
        if ("REWARDED".equals(task.getStatus())) {
            result.setSuccess(false);
            result.setMessage("Reward already claimed");
        } else if ("COMPLETED".equals(task.getStatus())) {
            task.setStatus("REWARDED");
            taskRepository.save(task);
            result.setSuccess(true);
            result.setMessage("Reward claimed");
            result.setData(Map.of("reward", 100));
        } else {
            result.setSuccess(false);
            result.setMessage("Task not completed yet");
        }
        return result;
    }

    private Long getUserId(UserDetails user) {
        // Nếu username chính là userId (ví dụ khi sinh JWT set subject là userId)
        try {
            return Long.valueOf(user.getUsername());
        } catch (NumberFormatException e) {
            // Nếu không, phải truy vấn userId qua UserService hoặc custom UserDetails
            return null;
        }
    }
}
