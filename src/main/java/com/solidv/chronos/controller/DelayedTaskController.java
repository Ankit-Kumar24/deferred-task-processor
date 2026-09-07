package com.solidv.chronos.controller;

import com.solidv.chronos.dto.CreateTaskRequest;
import com.solidv.chronos.dto.TaskResponse;
import com.solidv.chronos.service.TaskSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class DelayedTaskController {

    private final TaskSubmissionService taskSubmissionService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskSubmissionService.createTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        TaskResponse response = taskSubmissionService.getTaskById(id);
        return ResponseEntity.ok(response);
    }
}
