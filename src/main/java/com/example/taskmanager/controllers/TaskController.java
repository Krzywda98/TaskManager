package com.example.taskmanager.controllers;



import com.example.taskmanager.models.Task;
import com.example.taskmanager.services.TaskService;
import org.springframework.web.bind.annotation.*;

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

   @GetMapping("/{id}")
    public Task getTasksById(@RequestParam Long id){
       return taskService.getTaskById(id);
   }

   @PostMapping
    public Task createTask(@RequestBody Task task) {
       return taskService.createTask(task);
   }

   @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable Long id){
        taskService.deleteTask(id);
   }

   @PutMapping("/{id}")
    public Task updateTask(@PathVariable Long id, @RequestBody Task task) {
       return taskService.updateTask(id, task);
   }
}
