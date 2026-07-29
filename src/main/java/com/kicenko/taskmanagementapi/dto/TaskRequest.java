package com.kicenko.taskmanagementapi.dto;

import com.kicenko.taskmanagementapi.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaskRequest (
    @NotBlank(message = "title must not be blank")
    String title,

    String description,

    @NotNull(message = "status must not be null")
    TaskStatus status
) {
}
