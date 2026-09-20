package com.vijay.todo_management.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vijay.todo_management.controller.ChecklistItemController;
import com.vijay.todo_management.controller.TodoRoleController;
import com.vijay.todo_management.dto.ChecklistItemCreateRequest;
import com.vijay.todo_management.dto.TodoRoleCreateRequest;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.ChecklistItemService;
import com.vijay.todo_management.service.TodoRoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ValidationActivationTest {

    private MockMvc mockMvcTodoRole;
    private MockMvc mockMvcChecklistItem;

    @Mock
    private TodoRoleService todoRoleService;

    @Mock
    private ChecklistItemService checklistItemService;

    @InjectMocks
    private TodoRoleController todoRoleController;

    @InjectMocks
    private ChecklistItemController checklistItemController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvcTodoRole = MockMvcBuilders.standaloneSetup(todoRoleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvcChecklistItem = MockMvcBuilders.standaloneSetup(checklistItemController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email("test@example.com")
                .username("testuser")
                .jti("jti-test")
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
    void testBlankTodoRoleName_FailsValidationWith400AndFieldErrors() throws Exception {
        TodoRoleCreateRequest invalidReq = new TodoRoleCreateRequest("");

        mockMvcTodoRole.perform(post("/api/workspaces/my-ws/projects/my-proj/todo-roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Role name is required"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void testInvalidChecklistItem_FailsValidationWithMultipleFieldErrors() throws Exception {
        ChecklistItemCreateRequest invalidReq = new ChecklistItemCreateRequest("", -5);
        UUID todoId = UUID.randomUUID();

        mockMvcChecklistItem.perform(post("/api/workspaces/my-ws/projects/my-proj/todos/{todoId}/checklist-items", todoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.fieldErrors.text").value("Text is required"))
                .andExpect(jsonPath("$.fieldErrors.position").value("Position must be greater than or equal to 0"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void testMalformedJson_ReturnsStandardized400Payload() throws Exception {
        String malformedJson = "{ \"text\": ";
        UUID todoId = UUID.randomUUID();

        mockMvcChecklistItem.perform(post("/api/workspaces/my-ws/projects/my-proj/todos/{todoId}/checklist-items", todoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed JSON request payload"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
