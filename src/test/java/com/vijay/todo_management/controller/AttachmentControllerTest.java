package com.vijay.todo_management.controller;
import com.vijay.todo_management.dto.AttachmentDto;
import com.vijay.todo_management.exception.*;
import com.vijay.todo_management.security.UserPrincipal;
import com.vijay.todo_management.service.AttachmentService;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AttachmentControllerTest {
    AttachmentService service; MockMvc mvc;
    UUID user=UUID.randomUUID(), todo=UUID.randomUUID(), id=UUID.randomUUID();
    String path;
    @BeforeEach void setup() {
        service=mock(AttachmentService.class);
        mvc=MockMvcBuilders.standaloneSetup(new AttachmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                UserPrincipal.builder().id(user).active(true).build(),null,List.of()));
        path="/api/workspaces/ws/projects/p/todos/"+todo+"/attachments";
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void multipartUploadUsesPrincipalAndReturns201() throws Exception {
        mvc.perform(multipart(path).file(new MockMultipartFile("file","hello.txt","text/plain","hello".getBytes())))
                .andExpect(status().isCreated());
        verify(service).upload(eq("ws"),eq("p"),eq(todo),any(),eq(user));
    }
    @Test void missingPartReturns400() throws Exception {
        mvc.perform(multipart(path)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void oversizedUploadReturns413() throws Exception {
        when(service.upload(anyString(),anyString(),any(),any(),any()))
                .thenThrow(new AttachmentTooLargeException("too large"));
        mvc.perform(multipart(path).file(new MockMultipartFile("file","a","text/plain",new byte[]{1})))
                .andExpect(status().is(413));
    }
    @Test void downloadsArePrivateAndNeverRenderedInline() throws Exception {
        var metadata=new AttachmentDto(id,todo,"hello.txt",5,user,"User",null);
        when(service.download("ws","p",todo,id,user)).thenReturn(
                new AttachmentService.Download(metadata,new ByteArrayInputStream("hello".getBytes())));
        mvc.perform(get(path+"/"+id+"/download")).andExpect(status().isOk())
                .andExpect(content().string("hello"))
                .andExpect(header().string("Content-Type","application/octet-stream"))
                .andExpect(header().string("X-Content-Type-Options","nosniff"))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")));
    }
}
