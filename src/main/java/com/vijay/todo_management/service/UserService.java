package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.UserDto;

import java.util.List;

public interface UserService {
    UserDto addUser(UserDto userDto);
    UserDto getUserById(Long id);
    List<UserDto> getAllUsers();
    UserDto updateUser(Long id, UserDto userDto);
    void deleteUser(Long id);
}