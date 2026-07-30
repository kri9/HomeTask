package com.kicenko.taskmanagementapi.repository;

import com.kicenko.taskmanagementapi.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TaskRepository extends MongoRepository<Task, String> {
    Optional<Task> findByIdAndUserId(String id, String userId);
}
