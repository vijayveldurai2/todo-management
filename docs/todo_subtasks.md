# Subtasks

A subtask is a normal Todo with an optional parentTodoId. It has its own display
ID, status, sprint, assignees, scheduling, estimates, tags, checklist, comments,
and attachments. This preserves the distinction between a checklist step and a
separately managed piece of work.

## API

Base: /api/workspaces/{workspaceSlug}/projects/{projectSlug}

- POST /todos: existing create request, now with optional parentTodoId.
  Omit it or send null for a standalone Todo.
- GET /todos/{todoId}/subtasks: immediate children, oldest first with ID tie-break.
- DELETE /todos/{todoId}/parent: promote to standalone; returns 200 TodoDto.
  Already-standalone Todos return 200 without a write.
- All existing Todo responses now include nullable parentTodoId.
- Existing project/sprint/backlog lists still include all matching Todos,
  including subtasks, as flat data. Their filters are unchanged.
- Existing Todo endpoints manage a subtask's other fields and related resources.

Example body to create a subtask:

```json
{
  "title": "Order ingredients",
  "parentTodoId": "00000000-0000-0000-0000-000000000001",
  "priority": "MEDIUM"
}
```

The example parent UUID must be replaced with a real Todo in that project.

## Rules

- Every operation uses the authenticated identity and existing project access
  rules: project member or workspace SUPER_ADMIN.
- Parent must exist in the same project; invalid/cross-project parent on creation
  returns 400. Missing/cross-project list or promotion targets return 404.
- Nested subtasks are supported without recursive response serialization.
- Parent is chosen only on creation. General PATCH ignores parentTodoId;
  reparenting existing Todos is not supported. Promotion only removes a link,
  so these operations cannot introduce cycles.
- Promotion preserves identity, data, and any children of the promoted Todo.
- Fields are independent: no inherited assignees, sprint, estimates, dates,
  priorities, or status. Normal Todo creation defaults apply.
- Completing a parent does not complete its children, and completing children
  does not automatically complete the parent. No estimate rollups.
- Deleting a Todo with any immediate children returns 409, even if they are all
  done. Explicitly delete or promote children first. Deleting a leaf uses the
  existing comment and attachment cleanup.

## Database and HTTP

V16 adds nullable parent_todo_id with a self-reference and ON DELETE RESTRICT.
Existing Todos remain standalone. No recursive cascade is used.
Whole-project/workspace hard deletes must also respect the parent restriction;
bulk tree deletion is not implemented by this feature.

Creating a child locks its parent until commit; deleting a parent locks that
same row. The child-existence check uses a current locking read, so it observes
children committed after a MySQL REPEATABLE_READ snapshot was established.
Ordinary entity updates cannot write parent_todo_id. Promotion uses an explicit
scoped update so an in-flight edit cannot restore a stale parent relationship.

The existing Todo description uses Jackson 2 in persistence. Explicit field
adapters now handle that tree through Spring MVC's Jackson 3 request/response
conversion. The underlying Java and storage types remain unchanged.

## Verification

Service tests cover parent scope, default independence, safe deletion,
promotion, direct-child listing and authorization. HTTP tests cover rich JSON,
parent IDs, ignored reparenting, description clearing and conflict responses.

SubtaskMysqlTest uses an EMPTY disposable schema only:

```text
mvn -Dtest=SubtaskMysqlTest -Dsubtask.test.jdbc.url=jdbc:mysql://127.0.0.1:33328/subtasks_test test
```

It expects an isolated local root account with an empty password. It applies
the actual V16 migration to a pre-V16 fixture and tests the FK, promotion with
descendants, current-read behavior after a concurrent child commit, and a stale
ordinary edit after promotion that must not restore the former parent.

Frontend rendering, reparenting, automatic completion, and aggregate estimates
are deferred. The existing display-ID allocation remains the shared Todo
creation path; subtasks do not introduce a separate numbering scheme.
