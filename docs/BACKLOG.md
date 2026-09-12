# Product backlog

Build for general task management across industries. Work through one feature
at a time: agree on behavior, explain the design, implement, and verify.

## Current
- File attachments: local backend implemented on codex/todo-attachments.
  Review the backend flow together; frontend UI remains pending the design discussion.
  Replaceable storage backend later; comment attachments and file versions are deferred.

## Next
- Subtasks: independent assignee, status and due date; distinct from checklist steps.
- Cookie/JWT TTL mismatch.
- Username/name mapping.
- Avatar URL population.
- Frontend session restoration through the existing /api/auth/me endpoint,
  plus intentional responses for missing/disabled users.
- Sprint completion: decide rollover versus manual triage.
- Activity/audit logging.
- Display-ID regex routing guard.

## Agreed product direction
- Workspace settings provide defaults; individual projects can use different workflows.
- Copy workspace defaults when creating a project. Later changes affect new
  projects only; existing projects retain their configuration.
- Optional sprints, story points, time estimates and custom roles.
- Admin-configurable workflows for any industry.
- Custom fields eventually: field definitions, task values, validation and editors.
- Shared workspace/project configuration is separate from personal view, theme
  and notification preferences.
- Fully usable without AI. Future AI-assisted setup proposes workflows, fields
  and enabled features from a user's description, for review before applying.
  AI uses the same configuration capabilities as manual setup.
- Database-per-workspace migration remains deferred.

## Completed
- Todo dates/estimates/assignments, checklist, rich-text description.
- Comments and threaded replies.
- Backend /api/auth/me exists; frontend integration is tracked above.
