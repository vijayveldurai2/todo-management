package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.TodoDto;

import java.util.List;
import java.util.UUID;

public interface TodoService {
    TodoDto addTodo(TodoDto todoDto);
    TodoDto getTodoById(UUID id);
    List<TodoDto> getAllTodos();
    TodoDto updateTodo(UUID id, TodoDto todoDto);
    void deleteTodo(UUID id);
}
