package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.ChecklistItemCreateRequest;
import com.vijay.todo_management.dto.ChecklistItemDto;
import com.vijay.todo_management.dto.ChecklistItemUpdateRequest;
import com.vijay.todo_management.dto.ItemReorderRequest;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.ChecklistItemService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistItemControllerTest {

    @Mock
    private ChecklistItemService checklistItemService;

    @InjectMocks
    private ChecklistItemController checklistItemController;

    private UUID userId;
    private UUID todoId;
    private UUID itemId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        todoId = UUID.randomUUID();
        itemId = UUID.randomUUID();

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
    void testGetChecklistItems_ReturnsOk() {
        ChecklistItemDto itemDto = new ChecklistItemDto(itemId, todoId, "Test item", false, 0, LocalDateTime.now(), LocalDateTime.now());
        when(checklistItemService.getChecklistItems("test-ws", "test-proj", todoId, userId))
                .thenReturn(List.of(itemDto));

        ResponseEntity<List<ChecklistItemDto>> res = checklistItemController.getChecklistItems("test-ws", "test-proj", todoId);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(1, res.getBody().size());
        assertEquals("Test item", res.getBody().get(0).getText());
    }

    @Test
    void testAddItem_ReturnsCreated() {
        ChecklistItemCreateRequest req = new ChecklistItemCreateRequest("Buy groceries", 0);
        ChecklistItemDto itemDto = new ChecklistItemDto(itemId, todoId, "Buy groceries", false, 0, LocalDateTime.now(), LocalDateTime.now());
        when(checklistItemService.addItem("test-ws", "test-proj", todoId, req, userId))
                .thenReturn(itemDto);

        ResponseEntity<ChecklistItemDto> res = checklistItemController.addItem("test-ws", "test-proj", todoId, req);

        assertEquals(HttpStatus.CREATED, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(itemId, res.getBody().getId());
        assertEquals("Buy groceries", res.getBody().getText());
    }

    @Test
    void testUpdateItem_ReturnsOk() {
        ChecklistItemUpdateRequest req = new ChecklistItemUpdateRequest("Updated text", true);
        ChecklistItemDto itemDto = new ChecklistItemDto(itemId, todoId, "Updated text", true, 0, LocalDateTime.now(), LocalDateTime.now());
        when(checklistItemService.updateItem("test-ws", "test-proj", todoId, itemId, req, userId))
                .thenReturn(itemDto);

        ResponseEntity<ChecklistItemDto> res = checklistItemController.updateItem("test-ws", "test-proj", todoId, itemId, req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals("Updated text", res.getBody().getText());
        assertEquals(true, res.getBody().isChecked());
    }

    @Test
    void testReorderItem_ReturnsOk() {
        ItemReorderRequest req = new ItemReorderRequest(2);
        ChecklistItemDto itemDto = new ChecklistItemDto(itemId, todoId, "Test item", false, 2, LocalDateTime.now(), LocalDateTime.now());
        when(checklistItemService.reorderItem("test-ws", "test-proj", todoId, itemId, 2, userId))
                .thenReturn(itemDto);

        ResponseEntity<ChecklistItemDto> res = checklistItemController.reorderItem("test-ws", "test-proj", todoId, itemId, req);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertNotNull(res.getBody());
        assertEquals(2, res.getBody().getPosition());
    }

    @Test
    void testDeleteItem_ReturnsNoContent() {
        doNothing().when(checklistItemService).deleteItem("test-ws", "test-proj", todoId, itemId, userId);

        ResponseEntity<Void> res = checklistItemController.deleteItem("test-ws", "test-proj", todoId, itemId);

        assertEquals(HttpStatus.NO_CONTENT, res.getStatusCode());
        verify(checklistItemService).deleteItem("test-ws", "test-proj", todoId, itemId, userId);
    }
}
