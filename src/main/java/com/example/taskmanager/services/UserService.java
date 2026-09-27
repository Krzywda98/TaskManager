package com.example.taskmanager.services;

import com.example.taskmanager.exceptions.EmailAlreadyExistException;
import com.example.taskmanager.exceptions.UserNotFoundException;
import com.example.taskmanager.models.User;
import com.example.taskmanager.models.dto.RegisterUserRequest;
import com.example.taskmanager.models.dto.UpdateUserRequest;
import com.example.taskmanager.models.dto.UserResponse;
import com.example.taskmanager.models.enums.Role;
import com.example.taskmanager.repositories.TaskRepository;
import com.example.taskmanager.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public UserService(UserRepository userRepository, TaskRepository taskRepository,
                       PasswordEncoder passwordEncoder, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    public List<UserResponse> getAllUsers() {
        currentUser.requireAdmin(currentUser.get());
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    public UserResponse getUserById(Long id) {
        currentUser.requireSelfOrAdmin(currentUser.get(), id);
        return UserResponse.from(findUser(id));
    }

    public UserResponse getUserByEmail(String email) {
        User actor = currentUser.get();
        if (!currentUser.isAdmin(actor) && !actor.getEmail().equals(email)) {
            throw new AccessDeniedException("Access denied");
        }
        return UserResponse.from(userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found")));
    }

    @Transactional
    public UserResponse createUser(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistException("Email already exists");
        }
        User user = new User();
        user.setName(request.getName());
        user.setSurname(request.getSurname());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        currentUser.requireSelfOrAdmin(currentUser.get(), id);
        User user = findUser(id);
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new EmailAlreadyExistException("Email already exists");
        }
        user.setName(request.name());
        user.setSurname(request.surname());
        user.setEmail(request.email());
        if (request.password() != null) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse updateRole(Long id, Role role) {
        currentUser.requireAdmin(currentUser.get());
        User user = findUser(id);
        user.setRole(role);
        return UserResponse.from(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        currentUser.requireSelfOrAdmin(currentUser.get(), id);
        User user = findUser(id);
        taskRepository.deleteByOwner_Id(id);
        userRepository.delete(user);
        userRepository.flush();
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
