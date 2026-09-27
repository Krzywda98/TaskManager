package com.example.taskmanager.models.dto;

import com.example.taskmanager.models.enums.TaskPriority;
import com.example.taskmanager.models.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 500) String description,
        @NotNull TaskStatus status,
        @NotNull TaskPriority priority,
        LocalDate deadline,
        @Positive Long ownerId) {
}
