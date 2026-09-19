# Agent Guidelines & Linked Repositories

## Linked Project Overview
This repository (`todo-management`) is the **Spring Boot REST API backend** for the companion frontend application **`todo-management-frontend`**.
The two repositories are intentionally kept separate (distinct Git remotes and histories — do NOT merge or combine repositories).

- **Backend Repository**: `todo-management` (`c:/Projects/Vijay/random/todo-management`)
  - **Tech Stack**: Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security with JWT, PostgreSQL / H2, Maven
  - **Git Remote**: `git@github.com:vijayveldurai2/todo-management.git`
- **Frontend Repository**: `todo-management-frontend` (`c:/Projects/Vijay/random/todo-management-frontend` or `../todo-management-frontend`)
  - **Tech Stack**: React 18, Vite, TypeScript, Redux Toolkit (RTK Query), Tailwind CSS, React Router v6
  - **Git Remote**: `git@github.com:vijayveldurai2/todo-management-frontend.git`
  - **Dev Server**: `http://localhost:5173` (proxies or connects to `/api`)

---

## Shared Context & Cross-Repository References

Whenever designing, implementing, updating, or debugging backend endpoints, DTOs, authentication, and business logic, you **MUST** consult and align with the corresponding specifications and types in the frontend repository:

| Context Area | Backend File | Linked Frontend File |
| :--- | :--- | :--- |
| **API Contract & Schemas** | [`api_list.md`](file:///c:/Projects/Vijay/random/todo-management/api_list.md) | [`api_list.md`](file:///c:/Projects/Vijay/random/todo-management-frontend/api_list.md) |
| **Auth & Token Spec** | [`implementation_plan_auth_me_endpoint.md`](file:///c:/Projects/Vijay/random/todo-management/docs/implementation_plan_auth_me_endpoint.md) | [`AUTH_INTEGRATION.md`](file:///c:/Projects/Vijay/random/todo-management-frontend/AUTH_INTEGRATION.md) |
| **URL Routing & Slugs** | [`URL_ROUTING.md`](file:///c:/Projects/Vijay/random/todo-management/docs/URL_ROUTING.md) | [`BR-Routing-Slugs.md`](file:///c:/Projects/Vijay/random/todo-management-frontend/BR-Routing-Slugs.md), [`URL_ROUTING.md`](file:///c:/Projects/Vijay/random/todo-management-frontend/URL_ROUTING.md) |
| **Business Requirements** | [`BACKLOG.md`](file:///c:/Projects/Vijay/random/todo-management/docs/BACKLOG.md) | [`BRD-todo-management.md`](file:///c:/Projects/Vijay/random/todo-management-frontend/BRD-todo-management.md) |
| **Frontend Architecture** | N/A | [`KNOWLEDGE_TRANSFER.md`](file:///c:/Projects/Vijay/random/todo-management-frontend/KNOWLEDGE_TRANSFER.md) |
| **Frontend API Layer** | `src/main/java/com/vijay/todo_management/controller/` | [`src/services/api.ts`](file:///c:/Projects/Vijay/random/todo-management-frontend/src/services/api.ts) & [`src/features/`](file:///c:/Projects/Vijay/random/todo-management-frontend/src/features/) |

---

## Cross-Repository Coordination Rules

1. **Repository Boundary**:
   - Never commit or stage frontend files into the backend git repository, and vice versa.
   - All backend code stays within `todo-management/`.
   - All frontend code stays within `todo-management-frontend/`.

2. **API Contract Fidelity**:
   - All response payloads must use standard JSON `camelCase` property naming matching what the frontend expects.
   - When modifying an existing endpoint or adding a new one, verify the endpoint path, HTTP method, query params, request body, and response shape against `../todo-management-frontend/api_list.md` and frontend RTK Query endpoints.
   - Maintain uniform error response structure (e.g. `{ "message": "...", "status": 400, "errors": [...] }`) so the frontend RTK Query error interceptors can parse errors cleanly.

3. **Authentication & Session**:
   - The frontend expects standard Bearer token authentication: `Authorization: Bearer <jwt_token>`.
   - The frontend handles token expiration on `401 Unauthorized`.
   - The `/api/auth/me` endpoint must return the active user's profile and roles.

4. **CORS Configuration**:
   - Ensure backend `SecurityConfig` and CORS filters permit `http://localhost:5173` (Vite dev server) with allowed methods (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`) and credentials/headers (`Authorization`, `Content-Type`).

5. **Multi-Root IDE Workspace**:
   - To view and edit both repositories side-by-side in Antigravity IDE without merging git branches or repositories, open [`todo-management.code-workspace`](file:///c:/Projects/Vijay/random/todo-management.code-workspace).

6. **Git Branching & Protected Branches Policy**:
   - **NEVER merge branches into `dev`, `stage`, `staging`, `main`, or `master`**.
   - Merges into base branches must strictly happen via Pull Requests (PRs) created by the user with descriptions.
   - All agent work must be committed and kept on dedicated feature branches (e.g., `feat/...`, `fix/...`).

7. **Application Execution Policy**:
   - **DO NOT keep dev servers or applications running in the background**.
   - You may compile, build, lint, and run automated tests, but do NOT launch or leave running applications/servers. The user runs and monitors the applications themselves.
