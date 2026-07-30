package com.kicenko.taskmanagementapi.repository;

import com.kicenko.taskmanagementapi.model.Task;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSearchRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private TaskSearchRepository taskSearchRepository;

    @Test
    void searchShouldBuildQueryFromProvidedFiltersAndPageable() {
        when(mongoTemplate.find(any(Query.class), eq(Task.class)))
                .thenReturn(List.of());

        PageRequest pageable = PageRequest.of(
                1,
                5,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        taskSearchRepository.search(
                "user-1",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                pageable
        );

        ArgumentCaptor<Query> queryCaptor =
                ArgumentCaptor.forClass(Query.class);

        verify(mongoTemplate).find(queryCaptor.capture(), eq(Task.class));

        Query query = queryCaptor.getValue();

        assertEquals("user-1", query.getQueryObject().get("userId"));
        assertEquals(TaskStatus.TODO, query.getQueryObject().get("status"));
        assertEquals(TaskPriority.HIGH, query.getQueryObject().get("priority"));
        assertEquals(5, query.getLimit());
        assertEquals(5, query.getSkip());
        assertEquals(-1, query.getSortObject().get("createdAt"));
    }
}
