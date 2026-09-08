package com.example.taskmanager.models.dto;


import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterUserRequest {

    @NotBlank
    @Size(min = 3, max = 10)
    private String name;

    @NotBlank
    @Size(min = 3, max = 10)
    private String surname;

    @Email
    @NotBlank
    @Size(min = 3, max = 50)
    private String email;

    @NotBlank
    @Size(min = 6, max = 100)
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
