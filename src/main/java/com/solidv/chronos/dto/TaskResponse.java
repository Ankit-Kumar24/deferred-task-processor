package com.solidv.chronos.dto;

import com.solidv.chronos.entity.DelayedTask;
import com.solidv.chronos.entity.TaskStatus;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        JsonNode payload,
        LocalDateTime executeAt,
        TaskStatus status,
        int retryCount,
        int maxRetries,
        LocalDateTime createdAt
) {
    public static TaskResponse fromEntity(DelayedTask task, JsonMapper objectMapper) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(task.getPayload());
            if (payload == null) {
                payload = objectMapper.getNodeFactory().textNode(task.getPayload());
            }
        } catch (JacksonException e) {
            payload = objectMapper.getNodeFactory().textNode(task.getPayload());
        }

        return new TaskResponse(
                task.getId(),
                payload,
                task.getExecuteAt(),
                task.getStatus(),
                task.getRetryCount(),
                task.getMaxRetries(),
                task.getCreatedAt()
        );
    }
}
