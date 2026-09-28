package com.example.taskmanager.controllers;

import com.example.taskmanager.models.dto.TaskRequest;
import com.example.taskmanager.models.dto.UpdateTaskStatusRequest;
import com.example.taskmanager.models.dto.TaskResponse;
import com.example.taskmanager.models.dto.TaskPageResponse;
import com.example.taskmanager.models.dto.ErrorResponse;
import com.example.taskmanager.models.enums.TaskStatus;
import com.example.taskmanager.models.enums.TaskPriority;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import com.example.taskmanager.services.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private static final Set<String> SORT_FIELDS = Set.of("id", "title", "deadline", "createdAt", "updatedAt");
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<?> getAllTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        if (page < 0 || size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(new ErrorResponse("page must be >= 0 and size between 1 and 100"));
        }
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])
                || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            return ResponseEntity.badRequest().body(new ErrorResponse(
                    "sort must be field,asc or field,desc; allowed fields: id, title, deadline, createdAt, updatedAt"));
        }
        Sort ordering = Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        if (!parts[0].equals("id")) {
            ordering = ordering.and(Sort.by("id"));
        }
        TaskPageResponse result = taskService.getAllTasks(status, priority, PageRequest.of(page, size, ordering));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(@Valid @RequestBody TaskRequest request) {
        return taskService.createTask(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest request) {
        return taskService.updateTask(id, request);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateTaskStatus(id, request.status());
    }
}
