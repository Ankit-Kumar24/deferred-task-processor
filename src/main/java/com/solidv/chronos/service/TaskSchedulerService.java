package com.solidv.chronos.service;

import com.solidv.chronos.entity.DelayedTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerService {

    private final TaskLifecycleService lifecycleService;
    private final TaskExecutor taskExecutor;

    @Scheduled(fixedDelay = 5000)
    public void pollAndProcessTasks() {
        log.debug("Polling for pending tasks...");
        List<DelayedTask> claimedTasks = lifecycleService.claimPendingTasks(10);

        if (!claimedTasks.isEmpty()) {
            log.info("Claimed {} tasks for processing", claimedTasks.size());
            for (DelayedTask task : claimedTasks) {
                log.info("Executing task ID={}", task.getId());
                boolean success = taskExecutor.execute(task);

                if (success) {
                    lifecycleService.markTaskCompleted(task);
                    log.info("Task ID={} completed successfully", task.getId());
                } else {
                    lifecycleService.markTaskFailedOrRetry(task);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverStuckTasks() {
        lifecycleService.recoverStuckTasks(Duration.ofMinutes(2));
    }
}
