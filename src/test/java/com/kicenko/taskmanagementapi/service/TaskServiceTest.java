package com.kicenko.taskmanagementapi.service;

import com.kicenko.taskmanagementapi.dto.TaskRequest;
import com.kicenko.taskmanagementapi.dto.TaskResponse;
import com.kicenko.taskmanagementapi.exception.TaskNotFoundException;
import com.kicenko.taskmanagementapi.model.Task;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import com.kicenko.taskmanagementapi.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskShouldSaveAndReturnTask() {
        Instant createdAt = Instant.parse("2026-07-29T16:00:00Z");

        TaskRequest request = new TaskRequest(
                "Learn Spring Boot",
                "Build REST API",
                TaskStatus.TODO,
                TaskPriority.MEDIUM
        );

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> {
                    Task task = invocation.getArgument(0);
                    task.setId("task-1");
                    task.setCreatedAt(createdAt);
                    return task;
                });

        TaskResponse response = taskService.createTask(request);

        assertAll(
                () -> assertEquals("task-1", response.id()),
                () -> assertEquals("Learn Spring Boot", response.title()),
                () -> assertEquals("Build REST API", response.description()),
                () -> assertEquals(TaskPriority.MEDIUM, response.priority()),
                () -> assertEquals(TaskStatus.TODO, response.status()),
                () -> assertEquals(createdAt, response.createdAt())
        );

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void getTaskByIdShouldThrowExceptionWhenTaskDoesNotExist() {
        when(taskRepository.findById("missing-id"))
                .thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.getTaskById("missing-id")
        );

        assertEquals(
                "Task not found with id: missing-id",
                exception.getMessage()
        );

        verify(taskRepository).findById("missing-id");
        verifyNoMoreInteractions(taskRepository);
    }

    @Test
    void updateTaskShouldUpdateExistingTask() {
        Instant createdAt = Instant.parse("2026-07-29T16:00:00Z");

        Task existingTask = new Task(
                "task-1",
                "Old title",
                "Old description",
                TaskStatus.TODO,
                TaskPriority.LOW,
                createdAt,
                createdAt
        );

        TaskRequest request = new TaskRequest(
                "Updated title",
                "Updated description",
                TaskStatus.DONE,
                TaskPriority.HIGH
        );

        when(taskRepository.findById("task-1"))
                .thenReturn(Optional.of(existingTask));

        when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTask("task-1", request);

        assertAll(
                () -> assertEquals("task-1", response.id()),
                () -> assertEquals("Updated title", response.title()),
                () -> assertEquals("Updated description", response.description()),
                () -> assertEquals(TaskStatus.DONE, response.status()),
                () -> assertEquals(TaskPriority.HIGH, response.priority()),
                () -> assertEquals(createdAt, response.createdAt())
        );

        verify(taskRepository).findById("task-1");
        verify(taskRepository).save(existingTask);
    }
}