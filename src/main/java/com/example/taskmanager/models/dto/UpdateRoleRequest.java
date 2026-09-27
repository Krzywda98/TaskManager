package com.example.taskmanager.models.dto;

import com.example.taskmanager.models.enums.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {
}
