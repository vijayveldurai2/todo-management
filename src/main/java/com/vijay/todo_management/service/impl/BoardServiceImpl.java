package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.BoardDto;
import com.vijay.todo_management.entity.Board;
import com.vijay.todo_management.entity.User;
import com.vijay.todo_management.repository.BoardRepository;
import com.vijay.todo_management.repository.UserRepository;
import com.vijay.todo_management.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private UserRepository userRepository;

    private BoardDto mapToDto(Board board) {
        BoardDto dto = new BoardDto();
        dto.setId(board.getId());
        dto.setName(board.getName());
        dto.setDescription(board.getDescription());
        dto.setOwnerId(board.getOwner().getId());
        dto.setMemberIds(board.getMembers().stream().map(User::getId).collect(Collectors.toSet()));
        return dto;
    }

    private Board mapToEntity(BoardDto dto) {
        Board board = new Board();
        board.setName(dto.getName());
        board.setDescription(dto.getDescription());

        User owner = userRepository.findById(dto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("Owner user not found with id: " + dto.getOwnerId()));
        board.setOwner(owner);

        if (dto.getMemberIds() != null) {
            Set<User> members = dto.getMemberIds().stream()
                    .map(id -> userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Member user not found with id: " + id)))
                    .collect(Collectors.toSet());
            board.setMembers(members);
        }

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
        existing.setDescription(boardDto.getDescription());

        if (boardDto.getMemberIds() != null) {
            Set<User> members = boardDto.getMemberIds().stream()
                    .map(uid -> userRepository.findById(uid)
                            .orElseThrow(() -> new RuntimeException("Member user not found with id: " + uid)))
                    .collect(Collectors.toSet());
            existing.setMembers(members);
        }

        return mapToDto(boardRepository.save(existing));
    }

    @Override
    public void deleteBoard(UUID id) {
        boardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Board not found with id: " + id));
        boardRepository.deleteById(id);
    }
}