package com.vijay.todo_management.service;

import com.vijay.todo_management.dto.BoardDto;

import java.util.List;

public interface BoardService {
    BoardDto addBoard(BoardDto boardDto);
    BoardDto getBoardById(Long id);
    List<BoardDto> getAllBoards();
    BoardDto updateBoard(Long id, BoardDto boardDto);
    void deleteBoard(Long id);
}