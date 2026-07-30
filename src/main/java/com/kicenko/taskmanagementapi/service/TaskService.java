package com.kicenko.taskmanagementapi.service;

import com.kicenko.taskmanagementapi.dto.TaskRequest;
import com.kicenko.taskmanagementapi.dto.TaskResponse;
import com.kicenko.taskmanagementapi.exception.TaskNotFoundException;
import com.kicenko.taskmanagementapi.model.Task;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import com.kicenko.taskmanagementapi.repository.TaskRepository;
import com.kicenko.taskmanagementapi.repository.TaskSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskSearchRepository taskSearchRepository;

    public TaskResponse createTask(
            String userId,
            TaskRequest request
    ) {
        Task task = Task.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .status(request.status())
                .priority(request.priority())
                .build();

        return toResponse(taskRepository.save(task));
    }

    public List<TaskResponse> getTasks(
            String userId,
            TaskStatus status,
            TaskPriority priority,
            Pageable pageable
    ) {
        return taskSearchRepository
                .search(userId, status, priority, pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTaskById(
            String userId,
            String id
    ) {
        return toResponse(findTaskById(userId, id));
    }

    public TaskResponse updateTask(
            String userId,
            String id,
            TaskRequest request
    ) {
        Task task = findTaskById(userId, id);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());

        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(String userId, String id) {
        taskRepository.delete(findTaskById(userId, id));
    }

    private Task findTaskById(String userId, String id) {
        return taskRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}