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
public class TaskClaimService {

    private final DelayedTaskRepository repository;

    @Transactional
    public List<DelayedTask> claimPendingTasks(int batchSize) {
        List<DelayedTask> tasks = repository.findTasksToClaim(LocalDateTime.now(), batchSize);
        
        tasks.forEach(task -> {
            task.setStatus(TaskStatus.PROCESSING);
        });

        return repository.saveAll(tasks);
    }
}
