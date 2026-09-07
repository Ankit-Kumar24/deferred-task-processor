package com.solidv.chronos.dto;

import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String payload,
        LocalDateTime executeAt,
        TaskStatus status,
        int retryCount,
        int maxRetries,
        LocalDateTime createdAt
) {
    public static TaskResponse fromEntity(DelayedTask task) {
        return new TaskResponse(
                task.getId(),
                task.getPayload(),
                task.getExecuteAt(),
                task.getStatus(),
                task.getRetryCount(),
                task.getMaxRetries(),
                task.getCreatedAt()
        );
    }
}
