package com.solidv.chronos.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateTaskRequest(
        @NotBlank(message = "Payload must not be blank")
        String payload,

        @NotNull(message = "ExecuteAt must not be null")
        @Future(message = "ExecuteAt must be in the future")
        LocalDateTime executeAt
) {
}
