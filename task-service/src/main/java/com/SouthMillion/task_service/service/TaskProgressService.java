package com.SouthMillion.task_service.service;

import com.SouthMillion.task_service.model.TaskProgress;
import com.SouthMillion.task_service.repository.TaskProgressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskProgressService {
    private final TaskProgressRepository repo;

    @Transactional
    public void increaseProgress(String userId, String taskCode, int targetId, int addAmount) {
        TaskProgress progress = repo.findByUserIdAndTaskCodeAndTargetId(userId, taskCode, targetId)
                .orElse(TaskProgress.builder()
                        .userId(userId)
                        .taskCode(taskCode)
                        .targetId(targetId)
                        .progress(0)
                        .targetAmount(10) // TODO: load config từ DB/file nếu có
                        .finished(false)
                        .build());

        if (!progress.isFinished()) {
            progress.setProgress(progress.getProgress() + addAmount);
            if (progress.getProgress() >= progress.getTargetAmount()) {
                progress.setFinished(true);
            }
            repo.save(progress);
        }
    }
}