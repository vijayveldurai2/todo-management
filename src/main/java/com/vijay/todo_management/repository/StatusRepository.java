package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StatusRepository extends JpaRepository<Status, UUID> {

    List<Status> findByProject_IdOrderByPositionAsc(UUID projectId);

    List<Status> findByProject_Workspace_SlugAndProject_SlugOrderByPositionAsc(String workspaceSlug, String projectSlug);

    Optional<Status> findByIdAndProject_Workspace_SlugAndProject_Slug(UUID id, String workspaceSlug, String projectSlug);

    Optional<Status> findByIdAndProject_Id(UUID id, UUID projectId);

    boolean existsByProject_IdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProject_IdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    long countByProject_Id(UUID projectId);

    @Query("SELECT COALESCE(MAX(s.position), -1) FROM Status s WHERE s.project.id = :projectId")
    int findMaxPositionByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT s FROM Status s WHERE s.project.id = :projectId AND s.position >= :position ORDER BY s.position ASC")
    List<Status> findByProjectIdAndPositionGreaterThanEqual(@Param("projectId") UUID projectId, @Param("position") int position);

    @Query("SELECT s FROM Status s WHERE s.project.id = :projectId AND s.position BETWEEN :start AND :end ORDER BY s.position ASC")
    List<Status> findByProjectIdAndPositionBetween(@Param("projectId") UUID projectId, @Param("start") int start, @Param("end") int end);
}
