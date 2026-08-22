package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.BoardDto;

import java.util.List;
import java.util.UUID;

public interface BoardService {
    BoardDto addBoard(BoardDto boardDto);
    BoardDto getBoardById(UUID id);
    List<BoardDto> getAllBoards();
    BoardDto updateBoard(UUID id, BoardDto boardDto);
    void deleteBoard(UUID id);
}
