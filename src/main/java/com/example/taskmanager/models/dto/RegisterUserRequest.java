package com.example.taskmanager.models.dto;


import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterUserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 12, message = "Name must be between 3 and 12 characters")
    private String name;

    @NotBlank(message = "Surname is required")
    @Size(min = 3, max = 10, message = "Surname must be between 3 and 10 characters")
    private String surname;

    @Email
    @NotBlank(message = "Email is required")
    @Size(min = 3, max = 30, message = "Your emails must be between 3 and 30 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 10, message = "Your password must be between 6 and 10 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, one number and one special character"
    )

    private String password;

    public RegisterUserRequest() {

    }

    public RegisterUserRequest(String name, String surname, String email, String password) {
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.password = password;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSurname() {
        return surname;
    }
    public void setSurname(String surname) {
        this.surname = surname;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
