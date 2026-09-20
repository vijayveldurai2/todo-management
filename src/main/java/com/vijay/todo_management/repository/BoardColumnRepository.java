package com.vijay.todo_management.repository;

import com.vijay.todo_management.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    List<BoardColumn> findByBoard_IdOrderByPositionAsc(UUID boardId);

    Optional<BoardColumn> findByIdAndBoard_Id(UUID id, UUID boardId);

    long countByBoard_Id(UUID boardId);

    boolean existsByPrimaryStatus_Id(UUID statusId);

    @Query("SELECT COUNT(c) > 0 FROM BoardColumn c JOIN c.additionalStatuses s WHERE s.id = :statusId")
    boolean existsByAdditionalStatuses_Id(@Param("statusId") UUID statusId);

    @Query("SELECT COALESCE(MAX(c.position), -1) FROM BoardColumn c WHERE c.board.id = :boardId")
    int findMaxPositionByBoardId(@Param("boardId") UUID boardId);

    @Query("SELECT c FROM BoardColumn c WHERE c.board.id = :boardId AND c.position >= :position ORDER BY c.position ASC")
    List<BoardColumn> findByBoardIdAndPositionGreaterThanEqual(@Param("boardId") UUID boardId, @Param("position") int position);

    @Query("SELECT c FROM BoardColumn c WHERE c.board.id = :boardId AND c.position BETWEEN :start AND :end ORDER BY c.position ASC")
    List<BoardColumn> findByBoardIdAndPositionBetween(@Param("boardId") UUID boardId, @Param("start") int start, @Param("end") int end);

    void deleteByBoard_Id(UUID boardId);
}
