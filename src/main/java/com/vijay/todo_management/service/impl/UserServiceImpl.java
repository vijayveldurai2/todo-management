package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.UserDto;
import com.vijay.todo_management.entity.User;
import com.vijay.todo_management.repository.UserRepository;
import com.vijay.todo_management.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        // password intentionally excluded from response
        return dto;
    }

    private User mapToEntity(UserDto dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // plaintext for now — will hash once auth is added
        user.setName(dto.getName());
        return user;
    }

    @Override
    public UserDto addUser(UserDto userDto) {
        User user = mapToEntity(userDto);
        return mapToDto(userRepository.save(user));
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return mapToDto(user);
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        existing.setUsername(userDto.getUsername());
        existing.setEmail(userDto.getEmail());
        existing.setName(userDto.getName());
        return mapToDto(userRepository.save(existing));
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        userRepository.deleteById(id);
    }
}