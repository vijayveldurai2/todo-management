package com.vijay.todo_management.controller;

import com.vijay.todo_management.dto.StatusCreateRequest;
import com.vijay.todo_management.dto.StatusDto;
import com.vijay.todo_management.enums.StatusCategory;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.StatusService;
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
class StatusControllerTest {

    @Mock
    private StatusService statusService;

    @InjectMocks
    private StatusController statusController;

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
    void testGetStatuses_DerivesUserIdFromSecurityContext() {
        StatusDto dto = new StatusDto(UUID.randomUUID(), UUID.randomUUID(), "To Do", StatusCategory.NOT_STARTED, 0, LocalDateTime.now(), LocalDateTime.now());
        when(statusService.getStatuses("ws-1", "proj-1", userId)).thenReturn(List.of(dto));

        ResponseEntity<List<StatusDto>> response = statusController.getStatuses("ws-1", "proj-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(statusService).getStatuses("ws-1", "proj-1", userId);
    }

    @Test
    void testCreateStatus_ReturnsCreated() {
        StatusCreateRequest req = new StatusCreateRequest("In Review", StatusCategory.IN_PROGRESS, 1);
        StatusDto dto = new StatusDto(UUID.randomUUID(), UUID.randomUUID(), "In Review", StatusCategory.IN_PROGRESS, 1, LocalDateTime.now(), LocalDateTime.now());
        when(statusService.createStatus("ws-1", "proj-1", req, userId)).thenReturn(dto);

        ResponseEntity<StatusDto> response = statusController.createStatus("ws-1", "proj-1", req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("In Review", response.getBody().getName());
        verify(statusService).createStatus("ws-1", "proj-1", req, userId);
    }

    @Test
    void testDeleteStatus_ReturnsNoContent() {
        UUID statusId = UUID.randomUUID();

        ResponseEntity<Void> response = statusController.deleteStatus("ws-1", "proj-1", statusId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(statusService).deleteStatus("ws-1", "proj-1", statusId, userId);
    }
}
