package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.model.TaskProgress;
import com.SouthMillion.task_service.repository.TaskProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GmTaskService {
    @Autowired
    private TaskProgressRepository taskProgressRepository;

    public void finishTask(String userId, int taskId) {
        TaskProgress task = taskProgressRepository.findByUserIdAndTargetId(userId, taskId)
                .orElse(new TaskProgress().builder().userId(userId).targetId(taskId).finished(false).build());
        task.setFinished(true);
        // Update progress nếu có logic
        taskProgressRepository.save(task);
    }
}
