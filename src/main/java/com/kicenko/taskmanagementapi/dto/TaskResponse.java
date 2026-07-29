package com.kicenko.taskmanagementapi.dto;

import com.kicenko.taskmanagementapi.model.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        String id,
        String title,
        String description,
        TaskStatus status,
        Instant createdAt
) {

}
