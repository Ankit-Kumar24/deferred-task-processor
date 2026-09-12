package com.solidv.chronos.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record CreateTaskRequest(
        @NotNull(message = "Payload must not be null")
        JsonNode payload,

        @NotNull(message = "ExecuteAt must not be null")
        @Future(message = "ExecuteAt must be in the future")
        Instant executeAt
) {
}
