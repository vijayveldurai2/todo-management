package com.vijay.todo_management.service.impl;

import com.vijay.todo_management.dto.*;
import com.vijay.todo_management.entity.*;
import com.vijay.todo_management.enums.BoardType;
import com.vijay.todo_management.enums.SprintStatus;
import com.vijay.todo_management.exception.BadRequestException;
import com.vijay.todo_management.exception.ForbiddenException;
import com.vijay.todo_management.exception.ResourceNotFoundException;
import com.vijay.todo_management.repository.*;
import com.vijay.todo_management.service.BoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoardServiceImpl implements BoardService {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardColumnRepository boardColumnRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private StatusDto mapStatusToDto(Status status) {
        if (status == null) return null;
        StatusDto dto = new StatusDto();
        dto.setId(status.getId());
        dto.setProjectId(status.getProject().getId());
        dto.setName(status.getName());
        dto.setCategory(status.getCategory());
        dto.setPosition(status.getPosition());
        dto.setCreatedAt(status.getCreatedAt());
        dto.setUpdatedAt(status.getUpdatedAt());
        return dto;
    }

    private BoardColumnDto mapColumnToDto(BoardColumn column) {
        BoardColumnDto dto = new BoardColumnDto();
        dto.setId(column.getId());
        dto.setBoardId(column.getBoard().getId());
        dto.setName(column.getName());
        dto.setPosition(column.getPosition());
        dto.setPrimaryStatusId(column.getPrimaryStatus().getId());
        dto.setPrimaryStatus(mapStatusToDto(column.getPrimaryStatus()));

        if (column.getAdditionalStatuses() != null) {
            dto.setAdditionalStatuses(
                    column.getAdditionalStatuses().stream()
                            .map(this::mapStatusToDto)
                            .sorted(Comparator.comparingInt(StatusDto::getPosition))
                            .collect(Collectors.toList())
            );
        } else {
            dto.setAdditionalStatuses(new ArrayList<>());
        }

        dto.setCreatedAt(column.getCreatedAt());
        dto.setUpdatedAt(column.getUpdatedAt());
        return dto;
    }

    private BoardDto mapBoardToDto(Board board, boolean includeColumns) {
        BoardDto dto = new BoardDto();
        dto.setId(board.getId());
        dto.setProjectId(board.getProject().getId());
        dto.setName(board.getName());
        dto.setBoardType(board.getBoardType());
        dto.setCreatedAt(board.getCreatedAt());
        dto.setUpdatedAt(board.getUpdatedAt());

        if (board instanceof SprintBoard sprintBoard) {
            dto.setSprintStartDate(sprintBoard.getSprintStartDate());
            dto.setSprintEndDate(sprintBoard.getSprintEndDate());
            dto.setSprintGoal(sprintBoard.getSprintGoal());
            dto.setSprintStatus(sprintBoard.getSprintStatus());
        }

        if (includeColumns) {
            List<BoardColumn> columns = boardColumnRepository.findByBoard_IdOrderByPositionAsc(board.getId());
            dto.setColumns(columns.stream().map(this::mapColumnToDto).collect(Collectors.toList()));
        }

        return dto;
    }

    private Project getProjectAndValidateAccess(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = projectRepository.findByWorkspace_SlugAndSlug(workspaceSlug, projectSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectSlug + " in workspace: " + workspaceSlug));

        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        boolean isProjectMember = projectMemberRepository.existsByProject_IdAndUser_Id(project.getId(), userId);

        if (!isSuperAdmin && !isProjectMember) {
            throw new ForbiddenException("Access denied: caller is not a member of this project or workspace");
        }

        return project;
    }

    private void validateProjectAdmin(Project project, UUID userId) {
        boolean isSuperAdmin = workspaceMemberRepository.findByWorkspace_IdAndUser_Id(project.getWorkspace().getId(), userId)
                .map(wm -> wm.getRole() == WorkspaceMember.Role.SUPER_ADMIN)
                .orElse(false);

        if (isSuperAdmin) {
            return;
        }

        boolean isProjectAdmin = projectMemberRepository.findByProject_IdAndUser_Id(project.getId(), userId)
                .map(pm -> pm.getProjectRole().isAdmin())
                .orElse(false);

        if (!isProjectAdmin) {
            throw new ForbiddenException("Caller must be a project Admin or workspace SUPER_ADMIN");
        }
    }

    @Override
    @Transactional
    public BoardDto createBoard(String workspaceSlug, String projectSlug, BoardCreateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        if (request.getBoardType() == null) {
            throw new BadRequestException("boardType is required");
        }

        Board board;
        if (request.getBoardType() == BoardType.SPRINT) {
            SprintBoard sprintBoard = new SprintBoard();
            sprintBoard.setProject(project);
            sprintBoard.setName(request.getName().trim());
            sprintBoard.setSprintStartDate(request.getSprintStartDate());
            sprintBoard.setSprintEndDate(request.getSprintEndDate());
            sprintBoard.setSprintGoal(request.getSprintGoal());
            sprintBoard.setSprintStatus(request.getSprintStatus() != null ? request.getSprintStatus() : SprintStatus.PLANNING);
            board = boardRepository.save(sprintBoard);
        } else {
            KanbanBoard kanbanBoard = new KanbanBoard();
            kanbanBoard.setProject(project);
            kanbanBoard.setName(request.getName().trim());
            board = boardRepository.save(kanbanBoard);
        }

        // Auto-create 3 BoardColumns pointing at default project statuses in the same transaction
        List<Status> projectStatuses = statusRepository.findByProject_IdOrderByPositionAsc(project.getId());
        List<BoardColumn> initialColumns = new ArrayList<>();

        if (!projectStatuses.isEmpty()) {
            for (int i = 0; i < Math.min(3, projectStatuses.size()); i++) {
                Status status = projectStatuses.get(i);
                BoardColumn column = new BoardColumn();
                column.setBoard(board);
                column.setName(status.getName());
                column.setPosition(i);
                column.setPrimaryStatus(status);
                initialColumns.add(column);
            }
            boardColumnRepository.saveAll(initialColumns);
        }

        return mapBoardToDto(board, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardDto> getBoards(String workspaceSlug, String projectSlug, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        return boardRepository.findByProject_Id(project.getId())
                .stream()
                .map(b -> mapBoardToDto(b, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BoardDto getBoardById(String workspaceSlug, String projectSlug, UUID boardId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        Board board = boardRepository.findByIdAndProject_Id(boardId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId + " in this project"));

        return mapBoardToDto(board, true);
    }

    @Override
    @Transactional
    public BoardDto updateBoard(String workspaceSlug, String projectSlug, UUID boardId, BoardUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Board board = boardRepository.findByIdAndProject_Id(boardId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId + " in this project"));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            board.setName(request.getName().trim());
        }

        if (board instanceof SprintBoard sprintBoard) {
            if (request.getSprintStartDate() != null) {
                sprintBoard.setSprintStartDate(request.getSprintStartDate());
            }
            if (request.getSprintEndDate() != null) {
                sprintBoard.setSprintEndDate(request.getSprintEndDate());
            }
            if (request.getSprintGoal() != null) {
                sprintBoard.setSprintGoal(request.getSprintGoal());
            }
            if (request.getSprintStatus() != null) {
                sprintBoard.setSprintStatus(request.getSprintStatus());
            }
        }

        Board saved = boardRepository.save(board);
        return mapBoardToDto(saved, true);
    }

    @Override
    @Transactional
    public void deleteBoard(String workspaceSlug, String projectSlug, UUID boardId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Board board = boardRepository.findByIdAndProject_Id(boardId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId + " in this project"));

        List<BoardColumn> columns = boardColumnRepository.findByBoard_IdOrderByPositionAsc(board.getId());
        boardColumnRepository.deleteAll(columns);
        boardRepository.delete(board);
    }

    @Override
    @Transactional
    public BoardColumnDto createColumn(String workspaceSlug, String projectSlug, UUID boardId, BoardColumnCreateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Board board = boardRepository.findByIdAndProject_Id(boardId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId + " in this project"));

        if (request.getPrimaryStatusId() == null) {
            throw new BadRequestException("primaryStatusId is required");
        }

        Status primaryStatus = statusRepository.findByIdAndProject_Id(request.getPrimaryStatusId(), project.getId())
                .orElseThrow(() -> new BadRequestException("Primary status not found or does not belong to this project: " + request.getPrimaryStatusId()));

        Set<Status> additionalStatuses = new HashSet<>();
        if (request.getAdditionalStatusIds() != null && !request.getAdditionalStatusIds().isEmpty()) {
            if (request.getAdditionalStatusIds().contains(request.getPrimaryStatusId())) {
                throw new BadRequestException("Primary status cannot also be included in additional statuses");
            }

            for (UUID addId : request.getAdditionalStatusIds()) {
                Status addStatus = statusRepository.findByIdAndProject_Id(addId, project.getId())
                        .orElseThrow(() -> new BadRequestException("Additional status not found or does not belong to this project: " + addId));
                additionalStatuses.add(addStatus);
            }
        }

        int currentCount = (int) boardColumnRepository.countByBoard_Id(board.getId());
        int targetPosition = currentCount;

        if (request.getPosition() != null && request.getPosition() >= 0 && request.getPosition() < currentCount) {
            targetPosition = request.getPosition();
            List<BoardColumn> toShift = boardColumnRepository.findByBoardIdAndPositionGreaterThanEqual(board.getId(), targetPosition);
            for (BoardColumn col : toShift) {
                col.setPosition(col.getPosition() + 1);
            }
            boardColumnRepository.saveAll(toShift);
        }

        BoardColumn column = new BoardColumn();
        column.setBoard(board);
        column.setName(request.getName().trim());
        column.setPosition(targetPosition);
        column.setPrimaryStatus(primaryStatus);
        column.setAdditionalStatuses(additionalStatuses);

        BoardColumn saved = boardColumnRepository.save(column);
        return mapColumnToDto(saved);
    }

    @Override
    @Transactional
    public BoardColumnDto updateColumn(String workspaceSlug, String projectSlug, UUID boardId, UUID columnId, BoardColumnUpdateRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Board board = boardRepository.findByIdAndProject_Id(boardId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId + " in this project"));

        BoardColumn column = boardColumnRepository.findByIdAndBoard_Id(columnId, board.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn not found with id: " + columnId + " on this board"));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            column.setName(request.getName().trim());
        }

        UUID effectivePrimaryId = column.getPrimaryStatus().getId();
        if (request.getPrimaryStatusId() != null) {
            Status newPrimary = statusRepository.findByIdAndProject_Id(request.getPrimaryStatusId(), project.getId())
                    .orElseThrow(() -> new BadRequestException("Primary status not found or does not belong to this project: " + request.getPrimaryStatusId()));
            column.setPrimaryStatus(newPrimary);
            effectivePrimaryId = newPrimary.getId();
        }

        if (request.getAdditionalStatusIds() != null) {
            if (request.getAdditionalStatusIds().contains(effectivePrimaryId)) {
                throw new BadRequestException("Primary status cannot also be included in additional statuses");
            }

            Set<Status> additionalStatuses = new HashSet<>();
            for (UUID addId : request.getAdditionalStatusIds()) {
                Status addStatus = statusRepository.findByIdAndProject_Id(addId, project.getId())
                        .orElseThrow(() -> new BadRequestException("Additional status not found or does not belong to this project: " + addId));
                additionalStatuses.add(addStatus);
            }
            column.setAdditionalStatuses(additionalStatuses);
        }

        BoardColumn saved = boardColumnRepository.save(column);
        return mapColumnToDto(saved);
    }

    @Override
    @Transactional
    public BoardColumnDto reorderColumn(String workspaceSlug, String projectSlug, UUID boardId, UUID columnId, ColumnReorderRequest request, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Board board = boardRepository.findByIdAndProject_Id(boardId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId + " in this project"));

        BoardColumn column = boardColumnRepository.findByIdAndBoard_Id(columnId, board.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn not found with id: " + columnId + " on this board"));

        if (request.getPosition() == null) {
            throw new BadRequestException("Position is required for reordering");
        }

        int oldPos = column.getPosition();
        int newPos = request.getPosition();
        int currentCount = (int) boardColumnRepository.countByBoard_Id(board.getId());

        if (newPos < 0) newPos = 0;
        if (newPos >= currentCount) newPos = currentCount - 1;

        if (oldPos != newPos) {
            if (oldPos < newPos) {
                List<BoardColumn> between = boardColumnRepository.findByBoardIdAndPositionBetween(board.getId(), oldPos + 1, newPos);
                for (BoardColumn col : between) {
                    col.setPosition(col.getPosition() - 1);
                }
                boardColumnRepository.saveAll(between);
            } else {
                List<BoardColumn> between = boardColumnRepository.findByBoardIdAndPositionBetween(board.getId(), newPos, oldPos - 1);
                for (BoardColumn col : between) {
                    col.setPosition(col.getPosition() + 1);
                }
                boardColumnRepository.saveAll(between);
            }
            column.setPosition(newPos);
        }

        BoardColumn saved = boardColumnRepository.save(column);
        return mapColumnToDto(saved);
    }

    @Override
    @Transactional
    public void deleteColumn(String workspaceSlug, String projectSlug, UUID boardId, UUID columnId, UUID userId) {
        Project project = getProjectAndValidateAccess(workspaceSlug, projectSlug, userId);
        validateProjectAdmin(project, userId);

        Board board = boardRepository.findByIdAndProject_Id(boardId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Board not found with id: " + boardId + " in this project"));

        BoardColumn column = boardColumnRepository.findByIdAndBoard_Id(columnId, board.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BoardColumn not found with id: " + columnId + " on this board"));

        int deletedPosition = column.getPosition();
        boardColumnRepository.delete(column);

        // Shift subsequent positions -1
        List<BoardColumn> subsequent = boardColumnRepository.findByBoardIdAndPositionGreaterThanEqual(board.getId(), deletedPosition + 1);
        for (BoardColumn col : subsequent) {
            col.setPosition(col.getPosition() - 1);
        }
        boardColumnRepository.saveAll(subsequent);
    }
}