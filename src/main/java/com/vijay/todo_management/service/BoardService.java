package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.*;

import java.util.List;
import java.util.UUID;

public interface BoardService {

    BoardDto createBoard(String workspaceSlug, String projectSlug, BoardCreateRequest request, UUID userId);

    List<BoardDto> getBoards(String workspaceSlug, String projectSlug, UUID userId);

    BoardDto getBoardById(String workspaceSlug, String projectSlug, UUID boardId, UUID userId);

    BoardDto updateBoard(String workspaceSlug, String projectSlug, UUID boardId, BoardUpdateRequest request, UUID userId);

    void deleteBoard(String workspaceSlug, String projectSlug, UUID boardId, UUID userId);

    BoardColumnDto createColumn(String workspaceSlug, String projectSlug, UUID boardId, BoardColumnCreateRequest request, UUID userId);

    BoardColumnDto updateColumn(String workspaceSlug, String projectSlug, UUID boardId, UUID columnId, BoardColumnUpdateRequest request, UUID userId);

    BoardColumnDto reorderColumn(String workspaceSlug, String projectSlug, UUID boardId, UUID columnId, ColumnReorderRequest request, UUID userId);

    void deleteColumn(String workspaceSlug, String projectSlug, UUID boardId, UUID columnId, UUID userId);
}
