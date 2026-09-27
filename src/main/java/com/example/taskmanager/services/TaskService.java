package com.example.taskmanager.services;

import com.example.taskmanager.exceptions.TaskNotFoundException;
import com.example.taskmanager.exceptions.UserNotFoundException;
import com.example.taskmanager.models.Task;
import com.example.taskmanager.models.User;
import com.example.taskmanager.models.dto.TaskRequest;
import com.example.taskmanager.models.dto.TaskResponse;
import com.example.taskmanager.repositories.TaskRepository;
import com.example.taskmanager.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, CurrentUser currentUser) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
    }

    public List<TaskResponse> getAllTasks() {
        User actor = currentUser.get();
        List<Task> tasks = currentUser.isAdmin(actor)
                ? taskRepository.findAll() : taskRepository.findByOwner_Id(actor.getId());
        return tasks.stream().map(TaskResponse::from).toList();
    }

    public TaskResponse getTaskById(Long id) {
        return TaskResponse.from(findAccessibleTask(id, currentUser.get()));
    }

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        User actor = currentUser.get();
        Task task = new Task();
        task.setOwner(resolveOwner(request.ownerId(), actor));
        applyFields(task, request);
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.delete(findAccessibleTask(id, currentUser.get()));
        taskRepository.flush();
    }

    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request) {
        User actor = currentUser.get();
        Task task = findAccessibleTask(id, actor);
        if (request.ownerId() != null) {
            task.setOwner(resolveOwner(request.ownerId(), actor));
        }
        applyFields(task, request);
        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    private Task findAccessibleTask(Long id, User actor) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (!currentUser.isAdmin(actor)
                && (task.getOwner() == null || !Objects.equals(task.getOwner().getId(), actor.getId()))) {
            throw new AccessDeniedException("Access denied");
        }
        return task;
    }

    private User resolveOwner(Long ownerId, User actor) {
        if (ownerId == null || Objects.equals(ownerId, actor.getId())) {
            return actor;
        }
        currentUser.requireAdmin(actor);
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new UserNotFoundException("Task owner not found"));
    }

    private void applyFields(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setPriority(request.priority());
        task.setDeadline(request.deadline());
    }
}
