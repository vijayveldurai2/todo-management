package com.vijay.todo_management.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
// Reparenting is deliberately ignored and cannot reach the service.
@JsonIgnoreProperties({"parentCommentId"})
@Getter @Setter
public class CommentUpdateRequest {
    private JsonNode contentJson;
    private String contentPlainText;
}
