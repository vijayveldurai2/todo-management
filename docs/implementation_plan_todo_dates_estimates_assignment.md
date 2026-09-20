# Implementation Plan: Todo Card — Dates, Estimates & Customizable Assignment

Adds `start_date_time`, `end_date_time`, `estimated_time`, `remaining_time` to `Todo`,
introduces `TodoRole` and `TodoAssignment` entities with full CRUD endpoints, and
wires multi-person assignment with primary-assignment logic.

Next version is **V10**.

---

## Confirmed Decisions ✅

| Decision | Resolution |
|---|---|
| `dueDate` → `endDateTime` | ✅ Removed. Frontend not yet implemented. |
| Time unit for `estimatedTime`/`remainingTime` | ✅ `DECIMAL(8,2)` decimal hours |
| `storyPoints` | ✅ **Added** — separate `SMALLINT` nullable field (integer, e.g. 1/2/3/5/8) |
| Start > end validation | ✅ Reject 400 if `startDateTime` is after `endDateTime`; null is allowed on either side |

---

## Proposed Changes

### Database Migration

#### [NEW] `V10__todo_dates_estimates_assignment.sql`
```sql
-- 1. Add date/estimate/story-points columns to todos
ALTER TABLE `todos`
  ADD COLUMN `start_date_time` datetime       DEFAULT NULL AFTER `due_date`,
  ADD COLUMN `end_date_time`   datetime       DEFAULT NULL AFTER `start_date_time`,
  ADD COLUMN `estimated_time`  decimal(8,2)   DEFAULT NULL AFTER `end_date_time`,
  ADD COLUMN `remaining_time`  decimal(8,2)   DEFAULT NULL AFTER `estimated_time`,
  ADD COLUMN `story_points`    smallint       DEFAULT NULL AFTER `remaining_time`;

-- Note: due_date column is retained in DB (nullable). Application layer stops
-- writing to it; a follow-up cleanup migration can drop it later.

-- 2. todo_roles — project-scoped, admin-customisable role labels
CREATE TABLE `todo_roles` (
  `id`         char(36)     NOT NULL,
  `project_id` char(36)     NOT NULL,
  `name`       varchar(100) NOT NULL,
  `created_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_roles_project_name` (`project_id`, `name`),
  CONSTRAINT `fk_todo_roles_project`
    FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. todo_assignments — many-to-many with role + primary flag
CREATE TABLE `todo_assignments` (
  `id`           char(36)   NOT NULL,
  `todo_id`      char(36)   NOT NULL,
  `user_id`      char(36)   NOT NULL,
  `todo_role_id` char(36)   NOT NULL,
  `is_primary`   tinyint(1) NOT NULL DEFAULT 0,
  `created_at`   datetime   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_todo_assignments_todo_user_role` (`todo_id`, `user_id`, `todo_role_id`),
  KEY `fk_ta_todo`      (`todo_id`),
  KEY `fk_ta_user`      (`user_id`),
  KEY `fk_ta_role`      (`todo_role_id`),
  CONSTRAINT `fk_ta_todo`
    FOREIGN KEY (`todo_id`)      REFERENCES `todos`      (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ta_user`
    FOREIGN KEY (`user_id`)      REFERENCES `users`      (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ta_role`
    FOREIGN KEY (`todo_role_id`) REFERENCES `todo_roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

---

### Entity Layer

#### [MODIFY] [`Todo.java`](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/entity/Todo.java)
- Add `startDateTime` (`@Column(name="start_date_time")`, nullable `LocalDateTime`)
- Add `endDateTime` (`@Column(name="end_date_time")`, nullable `LocalDateTime`) — replaces `dueDate`
- Add `estimatedTime` (`@Column(name="estimated_time")`, nullable `BigDecimal`)
- Add `remainingTime` (`@Column(name="remaining_time")`, nullable `BigDecimal`)
- Add `storyPoints` (`@Column(name="story_points")`, nullable `Integer`)
- Keep `dueDate` field in entity (DB column retained, stop writing to it)
- Add `@OneToMany(mappedBy="todo", cascade=ALL, orphanRemoval=true) List<TodoAssignment> assignments`

#### [NEW] [`TodoRole.java`](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/entity/TodoRole.java)
Maps `todo_roles` table. Fields: `id`, `project` (ManyToOne), `name`, `createdAt`, `updatedAt`.

#### [NEW] [`TodoAssignment.java`](file:///c:/Projects/Vijay/random/todo-management/src/main/java/com/vijay/todo_management/entity/TodoAssignment.java)
Maps `todo_assignments` table. Fields: `id`, `todo` (ManyToOne), `user` (ManyToOne),
`todoRole` (ManyToOne), `isPrimary` (boolean default false), `createdAt`.

---

### Repository Layer

#### [NEW] `TodoRoleRepository.java`
- `findByProject_Id(UUID projectId): List<TodoRole>`
- `findByIdAndProject_Id(UUID id, UUID projectId): Optional<TodoRole>`
- `existsByProject_IdAndNameIgnoreCase(UUID projectId, String name): boolean`

#### [NEW] `TodoAssignmentRepository.java`
- `findByTodo_Id(UUID todoId): List<TodoAssignment>`
- `findByTodo_IdAndIsPrimaryTrue(UUID todoId): Optional<TodoAssignment>`
- `existsByTodoRole_Id(UUID todoRoleId): boolean`
- `findByIdAndTodo_Id(UUID id, UUID todoId): Optional<TodoAssignment>`
- `countByTodo_Id(UUID todoId): long`

---

### DTO Layer

#### [NEW] `TodoRoleDto.java`
Fields: `id`, `projectId`, `name`, `createdAt`, `updatedAt`

#### [NEW] `TodoRoleCreateRequest.java`
Fields: `name` (@NotBlank)

#### [NEW] `TodoRoleUpdateRequest.java`
Fields: `name`

#### [NEW] `TodoAssignmentDto.java`
Fields: `id`, `todoId`, `userId`, `userDisplayName`, `userAvatarUrl`, `userEmail`, `todoRoleId`, `todoRoleName`, `isPrimary`, `createdAt`

#### [NEW] `TodoAssignmentRequest.java`
Fields: `userId` (UUID, required), `todoRoleId` (UUID, required), `isPrimary` (Boolean, optional — defaults to false unless it's the first assignment)

#### [MODIFY] `TodoDto.java`
- Add `startDateTime`, `endDateTime`, `estimatedTime` (BigDecimal), `remainingTime` (BigDecimal), `storyPoints` (Integer)
- Remove `dueDate`
- Add `assignments: List<TodoAssignmentDto>`

#### [MODIFY] `TodoCreateRequest.java`
- Add `startDateTime`, `endDateTime`, `estimatedTime`, `remainingTime`, `storyPoints`
- Remove `dueDate`

#### [MODIFY] `TodoUpdateRequest.java`
- Add `startDateTime`, `endDateTime`, `estimatedTime`, `remainingTime`, `storyPoints`
- Remove `dueDate`

---

### Service Layer

#### [NEW] `TodoRoleService.java` + `TodoRoleServiceImpl.java`
- `listRoles(workspaceSlug, projectSlug, userId): List<TodoRoleDto>`
- `createRole(workspaceSlug, projectSlug, request, userId): TodoRoleDto`
  — reject 409 if name already exists for project
- `updateRole(workspaceSlug, projectSlug, roleId, request, userId): TodoRoleDto`
- `deleteRole(workspaceSlug, projectSlug, roleId, userId): void`
  — reject 409 if any `TodoAssignment` references this role

All methods: caller must be project member OR workspace SUPER_ADMIN.

#### [MODIFY] `TodoService.java` + `TodoServiceImpl.java`
- `createTodo`: add `startDateTime`, `endDateTime`, `estimatedTime`, `remainingTime`, `storyPoints` mapping; validate start < end; drop `dueDate`
- `updateTodo`: same field additions; validate start < end; drop `dueDate`
- `mapToDto`: add new fields + `assignments` list
- `addAssignment(workspaceSlug, projectSlug, todoId, request, userId): TodoAssignmentDto`
  — if first assignment → force `isPrimary = true`
  — if `isPrimary = true` requested → unset previous primary in same `@Transactional`
  — unique constraint violation → 409 ConflictException
- `removeAssignment(workspaceSlug, projectSlug, todoId, assignmentId, userId): void`
- `setPrimaryAssignment(workspaceSlug, projectSlug, todoId, assignmentId, userId): TodoAssignmentDto`
  — unsets current primary, sets new one — same `@Transactional`

---

### Controller Layer

#### [NEW] `TodoRoleController.java`
Base: `/api/workspaces/{workspaceSlug}/projects/{projectSlug}/todo-roles`
- `GET /` → `listRoles`
- `POST /` → `createRole` (201)
- `PATCH /{roleId}` → `updateRole`
- `DELETE /{roleId}` → `deleteRole` (204)

#### [MODIFY] `TodoController.java`
Add assignment endpoints under `/todos/{todoId}/assignments`:
- `POST /` → `addAssignment` (201)
- `DELETE /{assignmentId}` → `removeAssignment` (204)
- `PATCH /{assignmentId}/primary` → `setPrimaryAssignment`

---

### `final.sql` update
Add `todo_roles` and `todo_assignments` table definitions + new `todos` columns to the consolidated schema file.

---

## Verification Plan

### Automated
- `./mvnw compile` — must be clean
- Existing tests must still pass

### Manual
1. Create a `TodoRole` for a project → 201
2. Create a todo → `addAssignment` (first one) → verify `isPrimary=true` even if not requested
3. Add a second assignment as primary → verify first flipped to `isPrimary=false`
4. Attempt duplicate `(todoId, userId, roleId)` → 409
5. Attempt same user, different role → succeeds (two rows)
6. Delete a `TodoRole` in use → 409
7. PATCH todo with `startDateTime > endDateTime` → 400
8. PATCH todo general update with `estimatedTime` → accepted; assignment fields → ignored (no-op, not error)
