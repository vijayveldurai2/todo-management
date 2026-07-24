package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByOwnerId(Long ownerId);
}