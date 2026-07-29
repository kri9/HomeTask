package com.kicenko.taskmanagementapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableMongoAuditing
@SpringBootApplication
public class TaskManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskManagementApiApplication.class, args);
    }

}
