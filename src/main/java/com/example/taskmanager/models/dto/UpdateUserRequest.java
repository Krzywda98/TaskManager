package com.example.taskmanager.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(min = 3, max = 12) String name,
        @NotBlank @Size(min = 3, max = 10) String surname,
        @NotBlank @Email @Size(min = 3, max = 50) String email,
        @Size(min = 6, max = 10)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$",
                message = "Password must contain uppercase, lowercase, a number and a special character")
        String password) {
}
