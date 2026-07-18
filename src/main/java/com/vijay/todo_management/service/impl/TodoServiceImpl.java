package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.TodoDto;
import com.vijay.todo_management.entity.Todo;
import com.vijay.todo_management.repository.TodoRepository;
import com.vijay.todo_management.service.TodoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TodoServiceImpl implements TodoService {

    @Autowired
    private TodoRepository todoRepository;

    private TodoDto mapToDto(Todo todo){
        return new TodoDto(todo.getId(), todo.getTitle(), todo.getDescription(), todo.getCompleted());
    }

    private Todo mapToEntity(TodoDto dto){
        Todo todo = new Todo();
        todo.setId(dto.getId());
        todo.setTitle(dto.getTitle());
        todo.setDescription(dto.getDescription());
        todo.setCompleted(dto.getCompleted());
        return todo;
    }

    @Override
    public TodoDto addTodo(TodoDto todoDto){
        Todo todo = mapToEntity(todoDto);
        Todo saved = todoRepository.save(todo);
        return mapToDto(saved);
    }

    @Override
    public TodoDto getTodoById(Long id){
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found with the id" + id));
        return mapToDto(todo);
    }

    @Override
    public List<TodoDto> getAllTodos(){
        return todoRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public TodoDto updateTodo(Long id, TodoDto todoDto){
        Todo existing = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found with id" + id));
        existing.setTitle(todoDto.getTitle());
        existing.setDescription(todoDto.getDescription());
        existing.setCompleted(todoDto.getCompleted());
        return mapToDto(todoRepository.save(existing));
    }

    @Override
    public void deleteTodo(Long id){
        todoRepository.findById(id).orElseThrow(() -> new RuntimeException("Todo not found with id" + id));
        todoRepository.deleteById(id);
    }

}







































