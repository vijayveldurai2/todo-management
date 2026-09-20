# Walkthrough: Wire Todo to Status + Sprint Assignment

Successfully wired `Todo` to the project `Status` and `SprintBoard` models, resolved the long-standing unpopulated `project_id` / `column_id` constraints, removed redundant `completed`/`completed_date` fields in favor of derived `isDone`, added dedicated status and sprint assignment endpoints, and implemented column-based grouping in board detail views.

## Changes Made

### 1. Database Schema & Migrations
- **[V8__todo_status_and_sprint.sql](file:///c:/Projects/Vijay/random/todo-management/src/main/resources/db/migration/V8__todo_status_and_sprint.sql)**:
  - Dropped foreign keys `fk_todos_column` and `fk_v5_todos_board`.
  - Dropped columns `column_id`, `board_id`, `completed`, and `completed_date`.
  - Added `status_id` (`char(36) NOT NULL`, FK to `statuses.id` `ON DELETE RESTRICT`).
  - Added `sprint_id` (`char(36) DEFAULT NULL`, FK to `sprint_boards.id` `ON DELETE SET NULL`).
- **[V9__todo_status_and_sprint.sql](file:///c:/Projects/Vijay/random/todo-management/src/main/resources/db/migration/V9__todo_status_and_sprint.sql)**:
  - Synchronization marker for environments where V9 was recorded.
- **[final.sql](file:///c:/Projects/Vijay/random/todo-management/src/main/resources/db/migration/final.sql)**:
  - Updated consolidated schema to reflect the updated `todos` table definition.

### 2. Entity & DTO Layer
- **[Todo.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/entity/Todo.java)**:
  - Replaced `board` and `column` with `status` (`@ManyToOne(fetch = FetchType.LAZY)`) and `sprint` (`@ManyToOne(fetch = FetchType.LAZY)`).
  - Removed `completed` and `completedDate`.
  - Guaranteed `project` and `displayId` (e.g. `WR-1`) are set on persistence.
- **[TodoCreateRequest.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoCreateRequest.java)**:
  - Created dedicated DTO with `title` (required), `description`, `priority`, optional `statusId` (defaults to project's lowest-position `NOT_STARTED` status), optional `sprintId`, `dueDate`, `tagNames`.
- **[TodoUpdateRequest.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoUpdateRequest.java)**:
  - General todo update DTO (excludes status and sprint mutation).
- **[TodoStatusUpdateRequest.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoStatusUpdateRequest.java)**:
  - Dedicated DTO for status drag-and-drop moves with `@NotNull UUID statusId`.
- **[TodoDto.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/TodoDto.java)**:
  - Added `statusId`, `status` (`StatusDto`), `sprintId`, and computed read-only `Boolean isDone` (`status.category == StatusCategory.DONE`).
- **[BoardColumnDto.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/dto/BoardColumnDto.java)**:
  - Added `List<TodoDto> todos` to hold column-mapped tasks in board detail responses.

### 3. Repository Layer
- **[TodoRepository.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/repository/TodoRepository.java)**:
  - Added queries `findByProject_Id`, `findByProject_IdAndSprint_Id`, `findByProject_IdAndSprintIsNull`, `findByIdAndProject_Id`.

### 4. Service Layer
- **[TodoServiceImpl.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/service/impl/TodoServiceImpl.java)**:
  - Implemented project-scoped CRUD using locked-in DB-backed access validation (`workspaceMemberRepository` for `SUPER_ADMIN` and `projectMemberRepository` for project members).
  - Default status resolution on creation (selects lowest position `NOT_STARTED` status if not specified).
  - Atomic incrementing of `project.displayIdSeq` and formatting `prefixCode-seq` (e.g. `WR-1`).
  - Implemented query precedence rule: `sprintId` takes precedence over `backlogOnly`.
  - Implemented `updateTodoStatus`, `assignSprint`, and `removeSprintAssignment` (with validation that the todo is currently assigned to the specified sprint before clearing).
- **[BoardServiceImpl.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/service/impl/BoardServiceImpl.java)**:
  - Updated `getBoardById` to group todos into columns based on `primaryStatus` and `additionalStatuses` matching:
    - **Kanban boards**: fetches all project todos.
    - **Sprint boards**: filters strictly by `sprint_id`.

### 5. Controller Layer
- **[TodoController.java](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/controller/TodoController.java)**:
  - `POST /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos`
  - `GET /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos?sprintId=...&backlogOnly=...`
  - `GET /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}`
  - `PATCH /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}`
  - `DELETE /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}`
  - `PATCH /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}/status`
  - `POST /api/workspaces/{workspaceSlug}/projects/{projectSlug}/sprints/{sprintId}/todos/{todoId}`
  - `DELETE /api/workspaces/{workspaceSlug}/projects/{projectSlug}/sprints/{sprintId}/todos/{todoId}`

---

## Verification Results

### Automated Tests
Run `./mvnw clean test`:
```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.controller.BoardControllerTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.controller.StatusControllerTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.controller.TodoControllerTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.service.BoardServiceImplTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.service.ProjectServiceImplTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.service.StatusServiceImplTest
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.service.TodoServiceImplTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.vijay.todo_management.TodoManagementApplicationTests
[INFO] 
[INFO] Results:
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

All 52 unit and integration tests passed with 100% success rate.
