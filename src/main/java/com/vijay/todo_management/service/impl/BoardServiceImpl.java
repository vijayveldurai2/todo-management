package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.BoardDto;
import com.vijay.todo_management.entity.Board;
import com.vijay.todo_management.entity.Project;
import com.vijay.todo_management.repository.BoardRepository;
import com.vijay.todo_management.repository.ProjectRepository;
import com.vijay.todo_management.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private BoardDto mapToDto(Board board) {
        BoardDto dto = new BoardDto();
        dto.setId(board.getId());
        dto.setProjectId(board.getProject().getId());
        dto.setName(board.getName());
        dto.setSlug(board.getSlug());
        dto.setDescription(board.getDescription());
        dto.setType(board.getType());
        dto.setPosition(board.getPosition());
        return dto;
    }

    private Board mapToEntity(BoardDto dto) {
        Board board = new Board();
        board.setName(dto.getName());
        board.setSlug(dto.getSlug());
        board.setDescription(dto.getDescription());
        board.setType(dto.getType() != null ? dto.getType() : "BOARD");
        board.setPosition(dto.getPosition());

        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + dto.getProjectId()));
        board.setProject(project);

        return board;
    }

    @Override
    public BoardDto addBoard(BoardDto boardDto) {
        return mapToDto(boardRepository.save(mapToEntity(boardDto)));
    }

    @Override
    public BoardDto getBoardById(UUID id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + id));
        return mapToDto(board);
    }

    @Override
    public List<BoardDto> getAllBoards() {
        return boardRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public BoardDto updateBoard(UUID id, BoardDto boardDto) {
        Board existing = boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + id));
        existing.setName(boardDto.getName());
        existing.setSlug(boardDto.getSlug());
        existing.setDescription(boardDto.getDescription());
        existing.setType(boardDto.getType());
        existing.setPosition(boardDto.getPosition());

        return mapToDto(boardRepository.save(existing));
    }

    @Override
    public void deleteBoard(UUID id) {
        boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + id));
        boardRepository.deleteById(id);
    }
}