package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.enums.BoardType;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.BoardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardService boardService;

    @InjectMocks
    private BoardController boardController;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("user@example.com")
                .username("user")
                .jti("jti-1")
                .active(true)
                .build();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateBoard_ReturnsCreated() {
        BoardCreateRequest req = new BoardCreateRequest("Sprint Board", BoardType.SPRINT, null, null, "Sprint Goal", null);
        BoardDto dto = new BoardDto();
        dto.setId(UUID.randomUUID());
        dto.setName("Sprint Board");
        dto.setBoardType(BoardType.SPRINT);

        when(boardService.createBoard("ws-1", "proj-1", req, userId)).thenReturn(dto);

        ResponseEntity<BoardDto> response = boardController.createBoard("ws-1", "proj-1", req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Sprint Board", response.getBody().getName());
        verify(boardService).createBoard("ws-1", "proj-1", req, userId);
    }

    @Test
    void testGetBoardById_ReturnsOkWithColumns() {
        UUID boardId = UUID.randomUUID();
        BoardDto dto = new BoardDto();
        dto.setId(boardId);
        dto.setName("Main Board");
        dto.setBoardType(BoardType.KANBAN);
        dto.setColumns(new ArrayList<>());

        when(boardService.getBoardById("ws-1", "proj-1", boardId, userId)).thenReturn(dto);

        ResponseEntity<BoardDto> response = boardController.getBoardById("ws-1", "proj-1", boardId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Main Board", response.getBody().getName());
        verify(boardService).getBoardById("ws-1", "proj-1", boardId, userId);
    }

    @Test
    void testCreateColumn_ReturnsCreated() {
        UUID boardId = UUID.randomUUID();
        UUID primaryStatusId = UUID.randomUUID();
        BoardColumnCreateRequest req = new BoardColumnCreateRequest("In QA", primaryStatusId, null, null);

        BoardColumnDto columnDto = new BoardColumnDto();
        columnDto.setId(UUID.randomUUID());
        columnDto.setName("In QA");
        columnDto.setPrimaryStatusId(primaryStatusId);

        when(boardService.createColumn("ws-1", "proj-1", boardId, req, userId)).thenReturn(columnDto);

        ResponseEntity<BoardColumnDto> response = boardController.createColumn("ws-1", "proj-1", boardId, req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("In QA", response.getBody().getName());
        verify(boardService).createColumn("ws-1", "proj-1", boardId, req, userId);
    }

    @Test
    void testReorderColumn_ReturnsOk() {
        UUID boardId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        ColumnReorderRequest req = new ColumnReorderRequest(0);

        BoardColumnDto columnDto = new BoardColumnDto();
        columnDto.setId(columnId);
        columnDto.setPosition(0);

        when(boardService.reorderColumn("ws-1", "proj-1", boardId, columnId, req, userId)).thenReturn(columnDto);

        ResponseEntity<BoardColumnDto> response = boardController.reorderColumn("ws-1", "proj-1", boardId, columnId, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getPosition());
        verify(boardService).reorderColumn("ws-1", "proj-1", boardId, columnId, req, userId);
    }

    @Test
    void testDeleteColumn_ReturnsNoContent() {
        UUID boardId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        ResponseEntity<Void> response = boardController.deleteColumn("ws-1", "proj-1", boardId, columnId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(boardService).deleteColumn("ws-1", "proj-1", boardId, columnId, userId);
    }
}
