package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.SprintBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SprintBoardRepository extends JpaRepository<SprintBoard, UUID> {
}
