package com.example.taskmanager.models.dto;

import com.example.taskmanager.models.Task;
import com.example.taskmanager.models.enums.TaskPriority;
import com.example.taskmanager.models.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(Long id, String title, String description, TaskStatus status,
                           TaskPriority priority, LocalDate deadline, LocalDateTime createdAt,
                           LocalDateTime updatedAt, Long ownerId) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getPriority(), task.getDeadline(), task.getCreatedAt(),
                task.getUpdatedAt(), task.getOwner() == null ? null : task.getOwner().getId());
    }
}
