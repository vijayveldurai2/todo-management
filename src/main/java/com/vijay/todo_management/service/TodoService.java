package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.TodoDto;

import java.util.List;

public interface TodoService {
    TodoDto addTodo(TodoDto todoDto);
    TodoDto getTodoById(Long id);
    List<TodoDto> getAllTodos();
    TodoDto updateTodo(Long id, TodoDto todoDto);
    void deleteTodo(Long id);

}
