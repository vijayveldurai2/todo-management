# Implementation Plan: Wire Todo to Status + Sprint Assignment (with Addendums)

Wire `Todo` to the new `Status` and `SprintBoard` models, resolving the historical NOT NULL constraints and unpopulated fields (`project_id`, `column_id`), introducing dedicated status and sprint assignment endpoints, updating board detail views to group todos into columns by status, **removing redundant `completed`/`completed_date` fields in favor of derived `isDone`**, and enforcing **consistent DB-backed authorization & query conflict precedence rules**.

## User Review Required

> [!IMPORTANT]
> - `todos` table schema is updated to:
>   - Drop direct `column_id` and `board_id` references.
>   - Drop redundant `completed` and `completed_date` columns.
>   - Add `status_id` (`NOT NULL`, FK to `statuses`).
>   - Add `sprint_id` (nullable, FK to `sprint_boards`).
> - Completion is no longer an independent, stored, or client-writable field. Instead, `TodoDto` exposes a derived read-only `Boolean isDone` computed from `status.category == StatusCategory.DONE`.
> - **Consistent DB-backed Authorization**: All todo methods (`createTodo`, `getTodos`, `getTodoById`, `updateTodo`, `deleteTodo`, `updateTodoStatus`, `assignSprint`, and `removeSprintAssignment`) use the locked-in `getProjectAndValidateAccess` pattern checking `workspaceMemberRepository` for `SUPER_ADMIN` and `projectMemberRepository` for project membership, throwing `ForbiddenException` if neither applies.
> - **Conflict precedence for query parameters**: In `GET /api/workspaces/{wsSlug}/projects/{pSlug}/todos?sprintId={id}&backlogOnly=true`, `sprintId` takes precedence if both are present (`backlogOnly` is ignored).

## Proposed Changes

### Database Migration

#### [NEW] [V8__todo_status_and_sprint.sql](file:///c:/Projects/Vijay/random/todo-management/src/main/resources/db/migration/V8__todo_status_and_sprint.sql)
- Drops foreign keys `fk_todos_column` and `fk_todos_board`.
- Drops columns `column_id`, `board_id`, `completed`, and `completed_date`.
- Adds `status_id` (`char(36) NOT NULL`, FK to `statuses.id` ON DELETE RESTRICT).
- Adds `sprint_id` (`char(36) DEFAULT NULL`, FK to `sprint_boards.id` ON DELETE SET NULL).

#### [MODIFY] [final.sql](file:///c:/Projects/Vijay/random/todo-management/src/main/resources/db/migration/final.sql)
- Updates consolidated schema to reflect the updated `todos` table definition without `completed`, `completed_date`, `board_id`, and `column_id`.

---

### Entity Layer

#### [MODIFY] [Todo.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/entity/Todo.java)
- Remove `board` and `column` fields.
- Remove `completed` and `completedDate` fields.
- Add `status` (`@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "status_id", nullable = false)`) pointing to `Status`.
- Add `sprint` (`@ManyToOne(fetch = FetchType.LAZY)`, `@JoinColumn(name = "sprint_id")`) pointing to `SprintBoard`.
- Retain `project`, `displayId`, `title`, `description`, `priority`, `tags`, `createdDate`, `modifiedDate`, and `dueDate`.

---

### DTO Layer

#### [NEW] [TodoCreateRequest.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoCreateRequest.java)
- Request body for creating a todo: `title` (required), `description`, `priority`, `statusId` (optional), `sprintId` (optional), `dueDate`, `tagNames`. (No `completed` or `completedDate`).

#### [NEW] [TodoUpdateRequest.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoUpdateRequest.java)
- Request body for general updates: `title`, `description`, `priority`, `dueDate`, `tagNames`. (No `statusId`, `sprintId`, `completed`, or `completedDate`).

#### [NEW] [TodoStatusUpdateRequest.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoStatusUpdateRequest.java)
- Request body for status change endpoint: `statusId` (required UUID).

#### [MODIFY] [TodoDto.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoDto.java)
- Remove `boardId`, `columnId`, `completed`, and `completedDate`.
- Add `statusId`, `status` (`StatusDto`), `sprintId`, and computed/derived `Boolean isDone` (true when `status.category == DONE`, false otherwise).

#### [MODIFY] [BoardColumnDto.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/BoardColumnDto.java)
- Add `private List<TodoDto> todos = new ArrayList<>();` to hold mapped todos in board details.

---

### Repository Layer

#### [MODIFY] [TodoRepository.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/repository/TodoRepository.java)
- Add query methods:
  - `List<Todo> findByProject_Id(UUID projectId);`
  - `List<Todo> findByProject_IdAndSprint_Id(UUID projectId, UUID sprintId);`
  - `List<Todo> findByProject_IdAndSprintIsNull(UUID projectId);`
  - `Optional<Todo> findByIdAndProject_Id(UUID id, UUID projectId);`

---

### Service Layer

#### [MODIFY] [TodoService.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/service/TodoService.java) & [TodoServiceImpl.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/service/impl/TodoServiceImpl.java)
- Implement project-scoped Todo CRUD & operations using standard `getProjectAndValidateAccess` helper:
  - `createTodo`:
    - Validates project membership / super admin access via `getProjectAndValidateAccess`.
    - Sets `project` on todo.
    - Generates `displayId` atomically from `project.getDisplayIdSeq() + 1` and `project.getPrefixCode()`.
    - If `statusId` is provided, validates that it belongs to this project; otherwise defaults to the project's lowest-position `NOT_STARTED` status (or lowest position status).
    - If `sprintId` is provided, validates it is a `SprintBoard` in the same project.
  - `getTodos(workspaceSlug, projectSlug, sprintId, backlogOnly, userId)`:
    - Precedence rule: if `sprintId != null`, fetch `findByProject_IdAndSprint_Id` (ignoring `backlogOnly`).
    - Else if `Boolean.TRUE.equals(backlogOnly)`, fetch `findByProject_IdAndSprintIsNull`.
    - Else fetch `findByProject_Id`.
  - `getTodoById`, `updateTodo`, `deleteTodo`.
  - `updateTodoStatus`:
    - Validates caller is a project member or super admin via `getProjectAndValidateAccess`.
    - Validates target `statusId` exists in the same project.
    - Updates `todo.status`.
  - `assignSprint`:
    - Validates caller is a project member or super admin via `getProjectAndValidateAccess`.
    - Validates `sprintId` is a `SprintBoard` in the project.
    - Overwrites `todo.sprint`.
  - `removeSprintAssignment`:
    - Validates caller is a project member or super admin via `getProjectAndValidateAccess`.
    - Validates that `todo.sprint` is not null and its ID matches `sprintId`. If not, throws `BadRequestException`.
    - Sets `todo.sprint = null`.
  - `mapToDto`:
    - Computes `isDone = (todo.getStatus() != null && todo.getStatus().getCategory() == StatusCategory.DONE)`.

#### [MODIFY] [BoardServiceImpl.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/service/impl/BoardServiceImpl.java)
- In `getBoardById`:
  - Fetch todos for the board context:
    - If `board instanceof SprintBoard sprintBoard`: fetch `todoRepository.findByProject_IdAndSprint_Id(project.getId(), sprintBoard.getId())`.
    - Else (Kanban): fetch `todoRepository.findByProject_Id(project.getId())`.
  - Group and map todos into columns matching column `primaryStatus.id` or `additionalStatuses` IDs.

---

### Controller Layer

#### [MODIFY] [TodoController.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/controller/TodoController.java)
- Endpoints:
  - `POST /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos` -> Create Todo
  - `GET /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos` -> List Todos (with `@RequestParam(required = false) UUID sprintId`, `@RequestParam(required = false) Boolean backlogOnly`, documenting `sprintId` precedence)
  - `GET /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}` -> Get Todo
  - `PATCH /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}` -> Update Todo (general fields)
  - `DELETE /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}` -> Delete Todo
  - `PATCH /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}/status` -> Update Status
  - `POST /api/workspaces/{workspaceSlug}/projects/{projectSlug}/sprints/{sprintId}/todos/{todoId}` -> Assign Sprint
  - `DELETE /api/workspaces/{workspaceSlug}/projects/{projectSlug}/sprints/{sprintId}/todos/{todoId}` -> Remove Sprint Assignment

---

## Verification Plan

### Automated Tests
- [NEW] `TodoServiceImplTest.java`:
  - Default status selection when `statusId` not specified
  - `project_id` and `display_id` population
  - Cross-project `statusId` rejection (400)
  - Explicit authorization failure (403) on all methods when non-member user calls
  - Workspace `SUPER_ADMIN` authorization bypass succeeds on all methods
  - Sprint assignment and reassignment
  - Sprint removal verification and rejection when not currently assigned
  - Conflict precedence test: calling `getTodos` with both `sprintId` AND `backlogOnly = true` asserts `sprintId` query is used and `backlogOnly` is ignored
  - `isDone` computation: `true` for `StatusCategory.DONE`, `false` for `NOT_STARTED` / `IN_PROGRESS`
- [NEW] `TodoControllerTest.java`:
  - Status endpoint HTTP 200 / error responses
  - Sprint assignment and removal endpoint responses
  - General update endpoint validation
- Update `BoardServiceImplTest.java`:
  - Kanban board grouping with todos across columns
  - Sprint board grouping filtering strictly by `sprintId`
- Run `./mvnw test` to ensure all tests pass.

### Manual Verification
- Compile codebase with `./mvnw compile`.
