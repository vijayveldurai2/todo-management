package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.BoardDto;
import com.vijay.todo_management.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @PostMapping
    public ResponseEntity<BoardDto> addBoard(@RequestBody BoardDto boardDto) {
        return new ResponseEntity<>(boardService.addBoard(boardDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BoardDto>> getAllBoards() {
        return ResponseEntity.ok(boardService.getAllBoards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BoardDto> getBoardById(@PathVariable UUID id) {
        return ResponseEntity.ok(boardService.getBoardById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BoardDto> updateBoard(@PathVariable UUID id, @RequestBody BoardDto boardDto) {
        return ResponseEntity.ok(boardService.updateBoard(id, boardDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable UUID id) {
        boardService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }
}
