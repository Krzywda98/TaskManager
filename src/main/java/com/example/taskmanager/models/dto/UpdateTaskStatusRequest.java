package com.example.taskmanager.models.dto;

import com.example.taskmanager.models.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {
}
