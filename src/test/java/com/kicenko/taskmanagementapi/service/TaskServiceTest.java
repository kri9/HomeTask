package com.kicenko.taskmanagementapi.service;

import com.kicenko.taskmanagementapi.dto.TaskRequest;
import com.kicenko.taskmanagementapi.dto.TaskResponse;
import com.kicenko.taskmanagementapi.exception.TaskNotFoundException;
import com.kicenko.taskmanagementapi.model.Task;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import com.kicenko.taskmanagementapi.repository.TaskRepository;
import com.kicenko.taskmanagementapi.repository.TaskSearchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private static final String USER_ID = "user-1";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskSearchRepository taskSearchRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskShouldAssignOwnerAndSaveTask() {
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

        TaskResponse response = taskService.createTask(USER_ID, request);

        ArgumentCaptor<Task> taskCaptor =
                ArgumentCaptor.forClass(Task.class);

        verify(taskRepository).save(taskCaptor.capture());

        assertAll(
                () -> assertEquals(
                        USER_ID,
                        taskCaptor.getValue().getUserId()
                ),
                () -> assertEquals("task-1", response.id()),
                () -> assertEquals(
                        "Learn Spring Boot",
                        response.title()
                ),
                () -> assertEquals(
                        TaskPriority.MEDIUM,
                        response.priority()
                ),
                () -> assertEquals(TaskStatus.TODO, response.status()),
                () -> assertEquals(createdAt, response.createdAt())
        );
    }

    @Test
    void getTaskByIdShouldSearchByTaskIdAndUserId() {
        when(taskRepository.findByIdAndUserId(
                "missing-id",
                USER_ID
        )).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(
                TaskNotFoundException.class,
                () -> taskService.getTaskById(
                        USER_ID,
                        "missing-id"
                )
        );

        assertEquals(
                "Task not found with id: missing-id",
                exception.getMessage()
        );

        verify(taskRepository).findByIdAndUserId(
                "missing-id",
                USER_ID
        );
    }

    @Test
    void updateTaskShouldUpdateOwnedTask() {
        Instant createdAt = Instant.parse("2026-07-29T16:00:00Z");

        Task existingTask = Task.builder()
                .id("task-1")
                .userId(USER_ID)
                .title("Old title")
                .description("Old description")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();

        TaskRequest request = new TaskRequest(
                "Updated title",
                "Updated description",
                TaskStatus.DONE,
                TaskPriority.HIGH
        );

        when(taskRepository.findByIdAndUserId(
                "task-1",
                USER_ID
        )).thenReturn(Optional.of(existingTask));

        when(taskRepository.save(existingTask))
                .thenReturn(existingTask);

        TaskResponse response = taskService.updateTask(
                USER_ID,
                "task-1",
                request
        );

        assertAll(
                () -> assertEquals("task-1", response.id()),
                () -> assertEquals("Updated title", response.title()),
                () -> assertEquals(
                        "Updated description",
                        response.description()
                ),
                () -> assertEquals(TaskStatus.DONE, response.status()),
                () -> assertEquals(
                        TaskPriority.HIGH,
                        response.priority()
                )
        );

        verify(taskRepository).findByIdAndUserId(
                "task-1",
                USER_ID
        );
        verify(taskRepository).save(existingTask);
    }
}