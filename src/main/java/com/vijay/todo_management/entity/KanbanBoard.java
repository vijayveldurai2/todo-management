package com.vijay.todo_management.entity;

import com.vijay.todo_management.enums.BoardType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "kanban_boards")
@DiscriminatorValue("KANBAN")
public class KanbanBoard extends Board {

    @Override
    public BoardType getBoardType() {
        return BoardType.KANBAN;
    }
}
