package com.solidv.chronos.service;

import com.solidv.chronos.config.ChronosProperties;
import com.solidv.chronos.dto.CreateTaskRequest;
import com.solidv.chronos.dto.TaskResponse;
import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;
import com.solidv.chronos.exception.TaskNotFoundException;
import com.solidv.chronos.repository.DelayedTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class TaskSubmissionService {

    private final DelayedTaskRepository taskRepository;
    private final ChronosProperties properties;
    private final JsonMapper objectMapper;

    public TaskResponse createTask(CreateTaskRequest request) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(request.payload());
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize task payload", e);
        }

        DelayedTask task = DelayedTask.builder()
                .payload(payload)
                .executeAt(request.executeAt())
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .maxRetries(properties.retry().maxRetries())
                .build();

        DelayedTask saved = taskRepository.save(task);
        return TaskResponse.fromEntity(saved, objectMapper);
    }

    public TaskResponse getTaskById(Long id) {
        DelayedTask task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskResponse.fromEntity(task, objectMapper);
    }
}
