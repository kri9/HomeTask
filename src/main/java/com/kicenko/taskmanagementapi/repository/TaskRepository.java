package com.kicenko.taskmanagementapi.repository;

import com.kicenko.taskmanagementapi.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskRepository extends MongoRepository<Task, String> {
}