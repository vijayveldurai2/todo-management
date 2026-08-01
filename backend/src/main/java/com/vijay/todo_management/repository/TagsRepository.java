package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Tags;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TagsRepository extends JpaRepository<Tags, UUID> {
    Optional<Tags> findByName(String name);
}
