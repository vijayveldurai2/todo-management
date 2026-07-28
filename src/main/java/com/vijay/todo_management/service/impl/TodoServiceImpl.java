package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.TodoDto;
import com.vijay.todo_management.entity.Board;
import com.vijay.todo_management.entity.Tags;
import com.vijay.todo_management.entity.Todo;
import com.vijay.todo_management.enums.Priority;
import com.vijay.todo_management.repository.BoardRepository;
import com.vijay.todo_management.repository.TagsRepository;
import com.vijay.todo_management.repository.TodoRepository;
import com.vijay.todo_management.service.TodoService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TodoServiceImpl implements TodoService {

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private TagsRepository tagsRepository;

    @Autowired
    private BoardRepository boardRepository;

    private TodoDto mapToDto(Todo todo){
        TodoDto dto = new TodoDto();
        dto.setId(todo.getId());
        dto.setTitle(todo.getTitle());
        dto.setDescription(todo.getDescription());
        dto.setCompleted(todo.getCompleted());
        dto.setPriority(todo.getPriority());
        dto.setCreatedDate(todo.getCreatedDate());
        dto.setModifiedDate(todo.getModifiedDate());
        dto.setDueDate(todo.getDueDate());
        dto.setCompletedDate(todo.getCompletedDate());

        Set<String> tagNames = todo.getTags()
                .stream()
                .map(Tags::getName)
                .collect(Collectors.toSet());
        dto.setTagNames(tagNames);
        dto.setBoardId(todo.getBoard().getId());
        return dto;
    }

    private Todo mapToEntity(TodoDto dto){
        Todo todo = new Todo();
        todo.setId(dto.getId());
        todo.setTitle(dto.getTitle());
        todo.setDescription(dto.getDescription());
        todo.setCompleted(dto.getCompleted());
        todo.setPriority(dto.getPriority() != null ? dto.getPriority() : Priority.MEDIUM);
        todo.setCompletedDate(dto.getCompletedDate());
        todo.setDueDate(dto.getDueDate());

        if(dto.getTagNames() != null){
            Set<Tags> tags = dto.getTagNames()
                    .stream()
                    .map(this::findOrCreateTag)
                    .collect(Collectors.toSet());
            todo.setTags(tags);
        }

        Board board = boardRepository.findById(dto.getBoardId())
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + dto.getBoardId()));
        todo.setBoard(board);

        return todo;
    }

    private Tags findOrCreateTag(String name){
        return tagsRepository.findByName(name)
                .orElseGet(() -> {
                   Tags newTag = new Tags();
                   newTag.setName(name);
                   return tagsRepository.save(newTag);
                });
    }

    @Override
    public TodoDto addTodo(TodoDto todoDto){
        Todo todo = mapToEntity(todoDto);
        Todo saved = todoRepository.save(todo);
        return mapToDto(saved);
    }

    @Override
    public TodoDto getTodoById(UUID id){
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
    public TodoDto updateTodo(UUID id, TodoDto todoDto){
        Todo existing = todoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Todo not found with id" + id));
        existing.setTitle(todoDto.getTitle());
        existing.setDescription(todoDto.getDescription());
        existing.setCompleted(todoDto.getCompleted());
        existing.setPriority(todoDto.getPriority());
        existing.setDueDate(todoDto.getDueDate());
        existing.setCompletedDate(todoDto.getCompletedDate());

        if (todoDto.getTagNames() != null){
            Set<Tags> tags = todoDto.getTagNames()
                    .stream()
                    .map(this::findOrCreateTag)
                    .collect(Collectors.toSet());
            existing.setTags(tags);
        }

        return mapToDto(todoRepository.save(existing));
    }

    @Override
    public void deleteTodo(UUID id){
        todoRepository.findById(id).orElseThrow(() -> new RuntimeException("Todo not found with id" + id));
        todoRepository.deleteById(id);
    }

}







































