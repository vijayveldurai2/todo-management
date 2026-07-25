package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.UserDto;

import java.util.List;
import java.util.UUID;

public interface UserService {
    UserDto addUser(UserDto userDto);
    UserDto getUserById(UUID id);
    List<UserDto> getAllUsers();
    UserDto updateUser(UUID id, UserDto userDto);
    void deleteUser(UUID id);
}
