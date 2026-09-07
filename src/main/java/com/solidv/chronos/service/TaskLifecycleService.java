package com.solidv.chronos.service;

import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;
import com.solidv.chronos.repository.DelayedTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskLifecycleService {

    private final DelayedTaskRepository repository;

    @Transactional
    public List<DelayedTask> claimPendingTasks(int batchSize) {
        List<DelayedTask> tasks = repository.findTasksToClaim(LocalDateTime.now(), batchSize);

        tasks.forEach(task -> {
            task.setStatus(TaskStatus.PROCESSING);
        });

        return repository.saveAll(tasks);
    }

    public void markTaskCompleted(DelayedTask task) {
        task.setStatus(TaskStatus.DONE);
        repository.save(task);
    }

    public void markTaskFailedOrRetry(DelayedTask task) {
        int retryCount = task.getRetryCount() + 1;
        task.setRetryCount(retryCount);

        if (retryCount >= task.getMaxRetries()) {
            task.setStatus(TaskStatus.FAILED);
        } else {
            task.setStatus(TaskStatus.PENDING);
            long backoffSeconds = 30L * (1L << (retryCount - 1));
            task.setExecuteAt(LocalDateTime.now().plusSeconds(backoffSeconds));
        }

        repository.save(task);
    }
}
