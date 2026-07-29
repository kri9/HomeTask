package com.kicenko.taskmanagementapi.repository;

import com.kicenko.taskmanagementapi.model.Task;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class TaskSearchRepository {

    private final MongoTemplate mongoTemplate;

    public List<Task> search(
            TaskStatus status,
            TaskPriority priority,
            Pageable pageable
    ) {
        Query query = new Query();

        Stream.of(
                        Optional.ofNullable(status)
                                .map(value -> Criteria.where("status").is(value)),
                        Optional.ofNullable(priority)
                                .map(value -> Criteria.where("priority").is(value))
                )
                .flatMap(Optional::stream)
                .forEach(query::addCriteria);

        query.with(pageable);

        return mongoTemplate.find(query, Task.class);
    }
}
