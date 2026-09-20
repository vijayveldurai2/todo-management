package com.vijay.todo_management.mapper;

import com.vijay.todo_management.dto.TodoDto;
import com.vijay.todo_management.entity.Todo;
import com.vijay.todo_management.repository.ChecklistStatsProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TodoMapperTest {

    private TodoMapper todoMapper;

    @BeforeEach
    void setUp() {
        todoMapper = new TodoMapper();
    }

    @Test
    void applyChecklistStats_zeroItems_setsZeroCountsAndNullPercentage() {
        TodoDto dto = new TodoDto();
        todoMapper.applyChecklistStats(dto, 0L, 0L);

        assertEquals(0, dto.getChecklistTotalCount());
        assertEquals(0, dto.getChecklistCompletedCount());
        assertNull(dto.getChecklistProgressPercentage(), "Progress percentage must be null when total items is 0");
    }

    @Test
    void applyChecklistStats_allCompleted_calculates100Percent() {
        TodoDto dto = new TodoDto();
        todoMapper.applyChecklistStats(dto, 5L, 5L);

        assertEquals(5, dto.getChecklistTotalCount());
        assertEquals(5, dto.getChecklistCompletedCount());
        assertEquals(100, dto.getChecklistProgressPercentage());
    }

    @Test
    void applyChecklistStats_noneCompleted_calculates0Percent() {
        TodoDto dto = new TodoDto();
        todoMapper.applyChecklistStats(dto, 4L, 0L);

        assertEquals(4, dto.getChecklistTotalCount());
        assertEquals(0, dto.getChecklistCompletedCount());
        assertEquals(0, dto.getChecklistProgressPercentage());
    }

    @Test
    void applyChecklistStats_fractional_roundsProperly() {
        // 1 out of 3 = 33.33% -> 33
        TodoDto dto1 = new TodoDto();
        todoMapper.applyChecklistStats(dto1, 3L, 1L);
        assertEquals(33, dto1.getChecklistProgressPercentage());

        // 2 out of 3 = 66.67% -> 67
        TodoDto dto2 = new TodoDto();
        todoMapper.applyChecklistStats(dto2, 3L, 2L);
        assertEquals(67, dto2.getChecklistProgressPercentage());

        // 1 out of 6 = 16.67% -> 17
        TodoDto dto3 = new TodoDto();
        todoMapper.applyChecklistStats(dto3, 6L, 1L);
        assertEquals(17, dto3.getChecklistProgressPercentage());
    }

    @Test
    void mapToDto_withStatsProjection_mapsAllFieldsCorrectly() {
        UUID todoId = UUID.randomUUID();
        Todo todo = new Todo();
        todo.setId(todoId);
        todo.setTitle("Test Title");
        todo.setDisplayId("WR-1");

        ChecklistStatsProjection stats = new ChecklistStatsProjection() {
            @Override
            public UUID getTodoId() { return todoId; }
            @Override
            public long getTotalCount() { return 4L; }
            @Override
            public long getCompletedCount() { return 2L; }
        };

        TodoDto dto = todoMapper.mapToDto(todo, stats);

        assertNotNull(dto);
        assertEquals(todoId, dto.getId());
        assertEquals("Test Title", dto.getTitle());
        assertEquals(4, dto.getChecklistTotalCount());
        assertEquals(2, dto.getChecklistCompletedCount());
        assertEquals(50, dto.getChecklistProgressPercentage());
    }

    @Test
    void mapToDtoList_batchStatsMap_matchesEachTodoCorrectly() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Todo todo1 = new Todo();
        todo1.setId(id1);
        Todo todo2 = new Todo();
        todo2.setId(id2);

        Map<UUID, ChecklistStatsProjection> statsMap = new HashMap<>();
        statsMap.put(id1, new ChecklistStatsProjection() {
            @Override
            public UUID getTodoId() { return id1; }
            @Override
            public long getTotalCount() { return 2L; }
            @Override
            public long getCompletedCount() { return 1L; }
        });

        List<TodoDto> dtos = todoMapper.mapToDtoList(List.of(todo1, todo2), statsMap);

        assertEquals(2, dtos.size());
        assertEquals(50, dtos.get(0).getChecklistProgressPercentage());
        assertEquals(0, dtos.get(1).getChecklistTotalCount());
        assertNull(dtos.get(1).getChecklistProgressPercentage());
    }
}
