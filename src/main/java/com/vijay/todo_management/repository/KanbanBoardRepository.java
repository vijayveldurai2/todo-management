package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.KanbanBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KanbanBoardRepository extends JpaRepository<KanbanBoard, UUID> {
}
