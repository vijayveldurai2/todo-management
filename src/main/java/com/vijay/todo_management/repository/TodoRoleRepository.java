package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.TodoRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TodoRoleRepository extends JpaRepository<TodoRole, UUID> {

    List<TodoRole> findByProject_IdOrderByNameAsc(UUID projectId);

    Optional<TodoRole> findByIdAndProject_Id(UUID id, UUID projectId);

    boolean existsByProject_IdAndNameIgnoreCase(UUID projectId, String name);
}
