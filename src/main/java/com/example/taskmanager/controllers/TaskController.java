package com.example.taskmanager.controllers;



import com.example.taskmanager.models.Task;
import com.example.taskmanager.services.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {
   private final TaskService taskService;

   public TaskController(TaskService taskService) {
       this.taskService = taskService;
   }

   @GetMapping
   public List<Task> getAllTasks() {
       return taskService.getAllTasks();
   }
}
