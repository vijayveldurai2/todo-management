package com.vijay.todo_management.entity;

import com.vijay.todo_management.enums.BoardType;
import com.vijay.todo_management.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sprint_boards")
@DiscriminatorValue("SPRINT")
public class SprintBoard extends Board {

    @Column(name = "sprint_start_date")
    private LocalDate sprintStartDate;

    @Column(name = "sprint_end_date")
    private LocalDate sprintEndDate;

    @Column(name = "sprint_goal", length = 1000)
    private String sprintGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "sprint_status", nullable = false, length = 30)
    private SprintStatus sprintStatus = SprintStatus.PLANNING;

    @Override
    public BoardType getBoardType() {
        return BoardType.SPRINT;
    }
}
