package com.solidv.chronos.service;

import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;
import com.solidv.chronos.repository.DelayedTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerService {

    private final TaskClaimService claimService;
    private final TaskExecutor taskExecutor;
    private final DelayedTaskRepository taskRepository;

    @Scheduled(fixedDelay = 5000)
    public void pollAndProcessTasks() {
        log.debug("Polling for pending tasks...");
        List<DelayedTask> claimedTasks = claimService.claimPendingTasks(10);

        if (!claimedTasks.isEmpty()) {
            log.info("Claimed {} tasks for processing", claimedTasks.size());
            for (DelayedTask task : claimedTasks) {
                log.info("Executing task ID={}", task.getId());
                boolean success = taskExecutor.execute(task);

                if (success) {
                    task.setStatus(TaskStatus.DONE);
                    log.info("Task ID={} completed successfully", task.getId());
                } else {
                    int updatedRetryCount = task.getRetryCount() + 1;
                    task.setRetryCount(updatedRetryCount);

                    if (updatedRetryCount >= task.getMaxRetries()) {
                        task.setStatus(TaskStatus.FAILED);
                        log.warn("Task ID={} failed and reached max retries ({}/{}). Marked as FAILED.",
                                task.getId(), updatedRetryCount, task.getMaxRetries());
                    } else {
                        task.setStatus(TaskStatus.PENDING);
                        log.info("Task ID={} failed. Retry count incremented to {}/{}. Re-queued as PENDING.",
                                task.getId(), updatedRetryCount, task.getMaxRetries());
                    }
                }
                taskRepository.save(task);
            }
        }
    }
}
