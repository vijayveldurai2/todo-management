package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.UserDto;
import com.vijay.todo_management.entity.User;
import com.vijay.todo_management.enums.Plan;
import com.vijay.todo_management.enums.Role;
import com.vijay.todo_management.repository.UserRepository;
import com.vijay.todo_management.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        user.setEmail(dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setIsActive(true);
        user.setRole(Role.USER);
        user.setPlan(Plan.FREE);
        user.setEmailVerifiedAt(LocalDateTime.now());
        return user;
    }

    @Override
    public UserDto addUser(UserDto userDto) {
        User user = mapToEntity(userDto);
        return mapToDto(userRepository.save(user));
    }

    @Override
    public UserDto getUserById(UUID id) {
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
    public UserDto updateUser(UUID id, UserDto userDto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        existing.setUsername(userDto.getUsername());
        existing.setEmail(userDto.getEmail());
        existing.setName(userDto.getName());
        return mapToDto(userRepository.save(existing));
    }

    @Override
    public void deleteUser(UUID id) {
        userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        userRepository.deleteById(id);
    }
}
