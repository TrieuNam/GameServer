package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.model.UserTask;
import com.SouthMillion.task_service.repository.UserTaskRepository;

import org.SouthMillion.dto.task.TaskClaimRequest;
import org.SouthMillion.dto.task.TaskProgressDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service

public class TaskService {
    @Autowired
    private  UserTaskRepository userTaskRepo;
    @Autowired
    private  StringRedisTemplate redisTemplate;

    private String getCacheKey(Long userId) {
        return "task:user:" + userId;
    }

    // Lấy tiến độ nhiệm vụ từ cache, nếu không có thì lấy DB rồi cache lại
    public List<TaskProgressDto> getTaskProgress(Long userId) {
        String cacheKey = getCacheKey(userId);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(cached, new com.fasterxml.jackson.core.type.TypeReference<List<TaskProgressDto>>() {});
            } catch (Exception e) {
                // Nếu lỗi, bỏ qua, load lại từ DB
            }
        }
        // Lấy DB
        List<UserTask> tasks = userTaskRepo.findByUserId(userId);
        List<TaskProgressDto> result = tasks.stream().map(t -> {
            TaskProgressDto dto = new TaskProgressDto();
            dto.setUserId(t.getUserId());
            dto.setTaskDefId(t.getTaskDefId());
            dto.setProgress(t.getProgress());
            dto.setStatus(t.getStatus());
            dto.setUpdateTime(t.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
        // Ghi cache
        try {
            redisTemplate.opsForValue().set(cacheKey,
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
        } catch (Exception e) {}
        return result;
    }

    // Nhận thưởng nhiệm vụ
    public TaskProgressDto claimTask(TaskClaimRequest req) {
        Long userId = req.getUserId();
        Long taskDefId = req.getTaskDefId();

        UserTask userTask = null;

        // Nếu taskDefId == null, tự tìm nhiệm vụ hợp lệ đầu tiên cho user
        if (taskDefId == null) {
            List<UserTask> list = userTaskRepo.findByUserId(userId);
            // Lấy nhiệm vụ trạng thái "done" (chưa claim)
            userTask = list.stream()
                    .filter(t -> t.getStatus() == 1)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No claimable task!"));
        } else {
            userTask = userTaskRepo.findByUserIdAndTaskDefId(userId, taskDefId)
                    .orElseThrow(() -> new RuntimeException("Task not found!"));
            if (userTask.getStatus() != 1) throw new RuntimeException("Task not completed!");
        }

        userTask.setStatus(2); // đã nhận thưởng
        userTask.setUpdateTime(System.currentTimeMillis());

        // **Ghi vào DB trước!**
        userTaskRepo.save(userTask);

        // Sau khi DB thành công mới update Redis
        refreshCache(userTask.getUserId());

        // Trả về tiến độ mới
        TaskProgressDto dto = new TaskProgressDto();
        dto.setUserId(userTask.getUserId());
        dto.setTaskDefId(userTask.getTaskDefId());
        dto.setProgress(userTask.getProgress());
        dto.setStatus(userTask.getStatus());
        dto.setUpdateTime(userTask.getUpdateTime());
        return dto;
    }

    // Sync lại cache (sau mỗi lần update)
    public void refreshCache(Long userId) {
        List<UserTask> tasks = userTaskRepo.findByUserId(userId);
        List<TaskProgressDto> result = tasks.stream().map(t -> {
            TaskProgressDto dto = new TaskProgressDto();
            dto.setUserId(t.getUserId());
            dto.setTaskDefId(t.getTaskDefId());
            dto.setProgress(t.getProgress());
            dto.setStatus(t.getStatus());
            dto.setUpdateTime(t.getUpdateTime());
            return dto;
        }).collect(Collectors.toList());
        try {
            redisTemplate.opsForValue().set(getCacheKey(userId),
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result));
        } catch (Exception e) {}
    }
}
