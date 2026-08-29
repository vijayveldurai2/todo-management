package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.security.CurrentUserProvider;
import com.vijay.todo_management.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceSlug}/projects/{projectSlug}/boards")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @PostMapping
    public ResponseEntity<BoardDto> createBoard(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @Valid @RequestBody BoardCreateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(boardService.createBoard(workspaceSlug, projectSlug, request, userId), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BoardDto>> getBoards(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(boardService.getBoards(workspaceSlug, projectSlug, userId));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDto> getBoardById(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID boardId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(boardService.getBoardById(workspaceSlug, projectSlug, boardId, userId));
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<BoardDto> updateBoard(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID boardId,
            @RequestBody BoardUpdateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(boardService.updateBoard(workspaceSlug, projectSlug, boardId, request, userId));
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID boardId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        boardService.deleteBoard(workspaceSlug, projectSlug, boardId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{boardId}/columns")
    public ResponseEntity<BoardColumnDto> createColumn(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID boardId,
            @Valid @RequestBody BoardColumnCreateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return new ResponseEntity<>(boardService.createColumn(workspaceSlug, projectSlug, boardId, request, userId), HttpStatus.CREATED);
    }

    @PatchMapping("/{boardId}/columns/{columnId}")
    public ResponseEntity<BoardColumnDto> updateColumn(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID boardId,
            @PathVariable UUID columnId,
            @RequestBody BoardColumnUpdateRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(boardService.updateColumn(workspaceSlug, projectSlug, boardId, columnId, request, userId));
    }

    @PatchMapping("/{boardId}/columns/{columnId}/reorder")
    public ResponseEntity<BoardColumnDto> reorderColumn(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID boardId,
            @PathVariable UUID columnId,
            @Valid @RequestBody ColumnReorderRequest request) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(boardService.reorderColumn(workspaceSlug, projectSlug, boardId, columnId, request, userId));
    }

    @DeleteMapping("/{boardId}/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(
            @PathVariable String workspaceSlug,
            @PathVariable String projectSlug,
            @PathVariable UUID boardId,
            @PathVariable UUID columnId) {
        UUID userId = CurrentUserProvider.getCurrentUserId();
        boardService.deleteColumn(workspaceSlug, projectSlug, boardId, columnId, userId);
        return ResponseEntity.noContent().build();
    }
}
