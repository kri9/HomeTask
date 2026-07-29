package com.kicenko.taskmanagementapi.service;

import com.kicenko.taskmanagementapi.dto.PageResponse;
import com.kicenko.taskmanagementapi.dto.TaskRequest;
import com.kicenko.taskmanagementapi.dto.TaskResponse;
import com.kicenko.taskmanagementapi.exception.TaskNotFoundException;
import com.kicenko.taskmanagementapi.model.Task;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import com.kicenko.taskmanagementapi.repository.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = new Task(
                null,
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                null,
                null
        );

        return toResponse(taskRepository.save(task));
    }

    public PageResponse<TaskResponse> getTasks(
            TaskStatus status,
            TaskPriority priority,
            Pageable pageable
    ) {
        Page<Task> tasks;

        if (status != null && priority != null) {
            tasks = taskRepository.findByStatusAndPriority(
                    status,
                    priority,
                    pageable
            );
        } else if (status != null) {
            tasks = taskRepository.findByStatus(status, pageable);
        } else if (priority != null) {
            tasks = taskRepository.findByPriority(priority, pageable);
        } else {
            tasks = taskRepository.findAll(pageable);
        }

        Page<TaskResponse> responses = tasks.map(this::toResponse);

        return PageResponse.from(responses);
    }

    public TaskResponse getTaskById(String id) {
        return toResponse(findTaskById(id));
    }

    public TaskResponse updateTask(String id, TaskRequest request) {
        Task task = findTaskById(id);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());

        return toResponse(taskRepository.save(task));
    }

    public void deleteTask(String id) {
        taskRepository.delete(findTaskById(id));
    }

    private Task findTaskById(String id) {
        return taskRepository.findById(id)
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