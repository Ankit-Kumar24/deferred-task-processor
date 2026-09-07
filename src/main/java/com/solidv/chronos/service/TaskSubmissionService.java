package com.solidv.chronos.service;

import com.solidv.chronos.dto.CreateTaskRequest;
import com.solidv.chronos.dto.TaskResponse;
import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;
import com.solidv.chronos.exception.TaskNotFoundException;
import com.solidv.chronos.repository.DelayedTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskSubmissionService {

    private final DelayedTaskRepository taskRepository;

    public TaskResponse createTask(CreateTaskRequest request) {
        DelayedTask task = DelayedTask.builder()
                .payload(request.payload())
                .executeAt(request.executeAt())
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .maxRetries(3)
                .build();

        DelayedTask saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved);
    }

    public TaskResponse getTaskById(Long id) {
        DelayedTask task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskResponse.fromEntity(task);
    }
}
