package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardRepository extends JpaRepository<Board, UUID> {
    List<Board> findByOwner_Id(UUID ownerId);
}
