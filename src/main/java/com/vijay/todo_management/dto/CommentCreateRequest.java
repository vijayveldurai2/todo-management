package com.vijay.todo_management.dto;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Getter @Setter
public class CommentCreateRequest {
    private JsonNode contentJson;
    private String contentPlainText;
    private UUID parentCommentId;
}
