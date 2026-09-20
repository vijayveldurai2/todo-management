package com.vijay.todo_management.controller;
import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.TodoService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SubtaskControllerTest {
    TodoService service; MockMvc mvc;
    UUID user=UUID.randomUUID(), parent=UUID.randomUUID(), child=UUID.randomUUID();
    String base="/api/workspaces/ws/projects/p/todos";
    @BeforeEach void setup() {
        service=mock(TodoService.class); var controller=new TodoController();
        ReflectionTestUtils.setField(controller,"todoService",service);
        mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().id(user).active(true).build(),null,List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void displayIdResolutionUsesAuthenticatedIdentity() throws Exception {
        mvc.perform(get(base + "/resolve/ord-12")).andExpect(status().isOk());
        verify(service).resolveTodo("ws", "p", "ord-12", user);
    }
    @Test void createWithParentAndRichDescriptionRoundTrips() throws Exception {
        when(service.createTodo(eq("ws"),eq("p"),any(),eq(user))).thenAnswer(i->{
            TodoCreateRequest request=i.getArgument(2);
            TodoDto result=new TodoDto(); result.setId(child); result.setParentTodoId(request.getParentTodoId());
            result.setDescriptionJson(request.getDescriptionJson()); return result;
        });
        mvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content(
                "{\"title\":\"Child\",\"parentTodoId\":\""+parent+"\",\"descriptionJson\":{\"type\":\"doc\",\"content\":[{\"type\":\"text\",\"text\":\"Hello\"}]},\"descriptionPlainText\":\"Hello\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.parentTodoId").value(parent.toString()))
                .andExpect(jsonPath("$.descriptionJson.type").value("doc"));
    }
    @Test void listAndPromoteUseAuthenticatedIdentity() throws Exception {
        mvc.perform(get(base+"/"+parent+"/subtasks")).andExpect(status().isOk());
        mvc.perform(delete(base+"/"+child+"/parent")).andExpect(status().isOk());
        verify(service).getSubtasks("ws","p",parent,user);
        verify(service).promoteSubtask("ws","p",child,user);
    }
    @Test void parentDeleteGuardMapsTo409() throws Exception {
        doThrow(new ResourceConflictException("Todo has subtasks")).when(service).deleteTodo("ws","p",parent,user);
        mvc.perform(delete(base+"/"+parent)).andExpect(status().isConflict());
    }
    @Test void patchIgnoresReparentingAndKeepsDescriptionPresence() throws Exception {
        mvc.perform(patch(base+"/"+child).contentType(MediaType.APPLICATION_JSON)
                .content("{\"parentTodoId\":\""+child+"\",\"descriptionJson\":null,\"descriptionPlainText\":null}"))
                .andExpect(status().isOk());
        verify(service).updateTodo(eq("ws"),eq("p"),eq(child),argThat(r->r.isDescriptionUpdateRequested()
                && r.isDescriptionJsonPresent() && r.isDescriptionPlainTextPresent()),eq(user));
        assertThrows(NoSuchMethodException.class,()->TodoUpdateRequest.class.getMethod("setParentTodoId",UUID.class));
    }
}
