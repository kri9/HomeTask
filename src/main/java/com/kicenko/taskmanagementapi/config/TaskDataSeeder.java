package com.kicenko.taskmanagementapi.config;

import com.kicenko.taskmanagementapi.model.Task;
import com.kicenko.taskmanagementapi.model.TaskPriority;
import com.kicenko.taskmanagementapi.model.TaskStatus;
import com.kicenko.taskmanagementapi.repository.TaskRepository;
import com.kicenko.taskmanagementapi.user.User;
import com.kicenko.taskmanagementapi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class TaskDataSeeder implements ApplicationRunner {

    private static final String DEMO_EMAIL = "demo@example.com";
    private static final String DEMO_PASSWORD = "password123";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        User demoUser = userRepository
                .findByEmailIgnoreCase(DEMO_EMAIL)
                .orElseGet(this::createDemoUser);

        String userId = demoUser.getId().toString();
        Instant now = Instant.now();

        List<Task> tasks = List.of(
                task(userId, "seed-001", "Prepare interview",
                        TaskStatus.TODO, TaskPriority.HIGH,
                        now.minus(12, ChronoUnit.DAYS)),
                task(userId, "seed-002", "Read Spring documentation",
                        TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM,
                        now.minus(11, ChronoUnit.DAYS)),
                task(userId, "seed-003", "Fix login bug",
                        TaskStatus.DONE, TaskPriority.HIGH,
                        now.minus(10, ChronoUnit.DAYS)),
                task(userId, "seed-004", "Write unit tests",
                        TaskStatus.TODO, TaskPriority.MEDIUM,
                        now.minus(9, ChronoUnit.DAYS)),
                task(userId, "seed-005", "Update README",
                        TaskStatus.IN_PROGRESS, TaskPriority.LOW,
                        now.minus(8, ChronoUnit.DAYS)),
                task(userId, "seed-006", "Configure MongoDB",
                        TaskStatus.DONE, TaskPriority.MEDIUM,
                        now.minus(7, ChronoUnit.DAYS)),
                task(userId, "seed-007", "Add pagination",
                        TaskStatus.TODO, TaskPriority.HIGH,
                        now.minus(6, ChronoUnit.DAYS)),
                task(userId, "seed-008", "Add sorting",
                        TaskStatus.IN_PROGRESS, TaskPriority.LOW,
                        now.minus(5, ChronoUnit.DAYS)),
                task(userId, "seed-009", "Add task filters",
                        TaskStatus.DONE, TaskPriority.LOW,
                        now.minus(4, ChronoUnit.DAYS)),
                task(userId, "seed-010", "Review API responses",
                        TaskStatus.TODO, TaskPriority.LOW,
                        now.minus(3, ChronoUnit.DAYS)),
                task(userId, "seed-011", "Check validation",
                        TaskStatus.IN_PROGRESS, TaskPriority.HIGH,
                        now.minus(2, ChronoUnit.DAYS)),
                task(userId, "seed-012", "Prepare submission",
                        TaskStatus.DONE, TaskPriority.HIGH,
                        now.minus(1, ChronoUnit.DAYS))
        );

        taskRepository.saveAll(tasks);
        log.info(
                "Upserted {} development tasks for {}",
                tasks.size(),
                DEMO_EMAIL
        );
    }

    private User createDemoUser() {
        return userRepository.save(
                User.builder()
                        .email(DEMO_EMAIL)
                        .password(passwordEncoder.encode(DEMO_PASSWORD))
                        .build()
        );
    }

    private Task task(
            String userId,
            String id,
            String title,
            TaskStatus status,
            TaskPriority priority,
            Instant timestamp
    ) {
        return Task.builder()
                .id(id)
                .userId(userId)
                .title(title)
                .description("Generated test task")
                .status(status)
                .priority(priority)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }
}
