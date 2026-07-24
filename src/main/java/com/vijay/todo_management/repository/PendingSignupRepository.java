package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.PendingSignup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {
    Optional<PendingSignup> findByEmail(String email);
    Optional<PendingSignup> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
