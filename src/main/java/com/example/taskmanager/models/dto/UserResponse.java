package com.example.taskmanager.models.dto;

import com.example.taskmanager.models.User;
import com.example.taskmanager.models.enums.Role;
import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String surname, String email,
                           Role role, LocalDateTime joinDate) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getSurname(),
                user.getEmail(), user.getRole(), user.getJoinDate());
    }
}
