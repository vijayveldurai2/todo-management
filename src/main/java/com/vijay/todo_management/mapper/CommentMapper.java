package com.vijay.todo_management.mapper;
import com.vijay.todo_management.dto.CommentDto;
import com.vijay.todo_management.entity.Comment;
import org.springframework.stereotype.Component;
@Component
public class CommentMapper {
    private static final tools.jackson.databind.json.JsonMapper HTTP_JSON =
            tools.jackson.databind.json.JsonMapper.builder().build();
    public CommentDto toDto(Comment c) {
        var author = c.getAuthor();
        return new CommentDto(c.getId(), c.getTodo().getId(), author.getId(),
                author.getName() == null || author.getName().isBlank() ? author.getUsername() : author.getName(),
                author.getAvatarUrl(), c.getParentComment() == null ? null : c.getParentComment().getId(),
                c.isDeleted() ? null : HTTP_JSON.readTree(c.getContentJson().toString()),
                c.isDeleted() ? "[comment deleted]" : c.getContentPlainText(),
                c.isDeleted(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
