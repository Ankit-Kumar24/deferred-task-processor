package com.solidv.chronos.service;

import com.solidv.chronos.config.ChronosProperties;
import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;
import com.solidv.chronos.repository.DelayedTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskLifecycleService {

    private final DelayedTaskRepository repository;
    private final ChronosProperties properties;

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
            long backoffSeconds = (long) (properties.retry().backoffBaseSeconds()
                    * Math.pow(properties.retry().backoffMultiplier(), retryCount - 1));
            task.setExecuteAt(LocalDateTime.now().plusSeconds(backoffSeconds));
        }

        repository.save(task);
    }

    @Transactional
    public void recoverStuckTasks(Duration timeout) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(timeout);
        List<DelayedTask> stuckTasks = repository.findByStatusAndUpdatedAtLessThan(
                TaskStatus.PROCESSING, cutoff);

        for (DelayedTask task : stuckTasks) {
            Duration stuckDuration = Duration.between(task.getUpdatedAt(), now);
            log.warn("Recovering stuck task ID={} after being stuck for {}", task.getId(), stuckDuration);
            markTaskFailedOrRetry(task);
        }
    }
}
