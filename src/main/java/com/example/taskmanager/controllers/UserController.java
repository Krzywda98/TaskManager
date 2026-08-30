package com.example.taskmanager.controllers;


import com.example.taskmanager.models.User;
import com.example.taskmanager.services.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping
    public Optional<User> getUserById(@RequestParam Long id){
        return userService.getUserById(id);
    }

    @GetMapping
    public Optional<User> getUserByEmail(@RequestParam String email){
        return userService.getUserByEmail(email);

    }

    @PostMapping
    public void createUser(@RequestBody User user) {
        userService.createUser(user);
    }

    @PutMapping("/{id}")
    public void updateUser(@PathVariable Long id, @RequestBody User user) {
        userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
    }
}
