package com.vijay.todo_management.controller;
import com.vijay.todo_management.dto.CommentUpdateRequest;
import com.vijay.todo_management.exception.GlobalExceptionHandler;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.CommentService;
import org.junit.jupiter.api.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentControllerTest {
    CommentService service;
    MockMvc mvc;
    UUID user = UUID.randomUUID(), todo = UUID.randomUUID(), comment = UUID.randomUUID();
    String path;
    @BeforeEach void setup() {
        service = mock(CommentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CommentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        var principal = UserPrincipal.builder().id(user).active(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal,null,java.util.List.of()));
        path = "/api/workspaces/ws/projects/p/todos/" + todo + "/comments";
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void transactionConflictReturns409() throws Exception {
        when(service.edit(eq("ws"),eq("p"),eq(todo),eq(comment),any(),eq(user)))
                .thenThrow(new OptimisticLockingFailureException("stale"));
        mvc.perform(patch(path + "/" + comment).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentPlainText\":\"hello\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }
    @Test void deleteUsesSecurityPrincipalAndReturns204() throws Exception {
        mvc.perform(delete(path + "/" + comment).param("userId",UUID.randomUUID().toString()))
                .andExpect(status().isNoContent());
        verify(service).delete("ws","p",todo,comment,user);
    }
    @Test void patchIgnoresParentId() throws Exception {
        mvc.perform(patch(path + "/" + comment).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentPlainText\":\"hello\",\"parentCommentId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
        verify(service).edit(eq("ws"),eq("p"),eq(todo),eq(comment),any(CommentUpdateRequest.class),eq(user));
    }
    @Test void createDeserializesStructuredJson() throws Exception {
        when(service.create(eq("ws"),eq("p"),eq(todo),any(),eq(user))).thenAnswer(i -> {
            com.vijay.todo_management.dto.CommentCreateRequest r = i.getArgument(3);
            return new com.vijay.todo_management.dto.CommentDto(comment,todo,user,"Author",null,null,
                    r.getContentJson(),r.getContentPlainText(),false,null,null);
        });
        mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"contentPlainText\":\"hello\",\"contentJson\":{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentJson.type").value("doc"))
                .andExpect(jsonPath("$.isDeleted").value(false));
        verify(service).create(eq("ws"),eq("p"),eq(todo),
                argThat(r -> "doc".equals(r.getContentJson().path("type").asText())),eq(user));
    }
}
