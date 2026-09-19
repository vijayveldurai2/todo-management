package com.vijay.todo_management.repository;

import java.util.UUID;

public interface ChecklistStatsProjection {
    UUID getTodoId();
    long getTotalCount();
    long getCompletedCount();
}
