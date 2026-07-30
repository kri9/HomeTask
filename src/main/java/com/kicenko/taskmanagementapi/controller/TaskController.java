package com.kicenko.taskmanagementapi.controller;

import com.kicenko.taskmanagementapi.dto.TaskRequest;
import com.kicenko.taskmanagementapi.dto.TaskResponse;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import com.kicenko.taskmanagementapi.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskRequest request
    ) {
        TaskResponse createdTask = taskService.createTask(
                jwt.getSubject(),
                request
        );

        URI location = URI.create("/api/tasks/" + createdTask.id());

        return ResponseEntity.created(location).body(createdTask);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                taskService.getTasks(
                        jwt.getSubject(),
                        status,
                        priority,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        return ResponseEntity.ok(
                taskService.getTaskById(jwt.getSubject(), id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody TaskRequest request
    ) {
        return ResponseEntity.ok(
                taskService.updateTask(
                        jwt.getSubject(),
                        id,
                        request
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id
    ) {
        taskService.deleteTask(jwt.getSubject(), id);

        return ResponseEntity.noContent().build();
    }
}