# API Documentation for Frontend Implementation

This document contains a comprehensive list of all backend API endpoints, their required queries/parameters, request bodies, and dummy response data to facilitate frontend development.

---

## 1. Auth API

### 1.1 Signup
- **Endpoint**: `POST /api/auth/signup`
- **Request Body**:
  ```json
  {
    "name": "John Doe",
    "email": "john@example.com",
    "password": "securepassword123"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "message": "User registered successfully. Please check your email to verify.",
    "userId": "123e4567-e89b-12d3-a456-426614174000"
  }
  ```

### 1.2 Verify Email (GET)
- **Endpoint**: `GET /api/auth/verify`
- **Query Params**: `?token=<verification-token>`
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Email verified successfully."
  }
  ```

### 1.3 Verify Email (POST)
- **Endpoint**: `POST /api/auth/verify`
- **Request Body**:
  ```json
  {
    "token": "verification-token-string"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "success": true,
    "message": "Email verified successfully."
  }
  ```

### 1.4 Login
- **Endpoint**: `POST /api/auth/login`
- **Request Body**:
  ```json
  {
    "email": "john@example.com",
    "password": "securepassword123"
  }
  ```
- **Response**: `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "John Doe",
      "email": "john@example.com"
    }
  }
  ```

---

## 2. Workspaces API

### 2.1 Create Workspace
- **Endpoint**: `POST /api/workspaces`
- **Query Params**: `?creatorId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "name": "Acme Corp",
    "slug": "acme-corp",
    "description": "Main workspace for Acme Corp"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": "223e4567-e89b-12d3-a456-426614174001",
    "name": "Acme Corp",
    "slug": "acme-corp",
    "description": "Main workspace for Acme Corp",
    "createdAt": "2023-10-01T12:00:00Z"
  }
  ```

### 2.2 Get Current User's Workspaces
- **Endpoint**: `GET /api/workspaces`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "223e4567-e89b-12d3-a456-426614174001",
      "name": "Acme Corp",
      "slug": "acme-corp",
      "description": "Main workspace for Acme Corp"
    }
  ]
  ```

### 2.3 Get Workspace By ID
- **Endpoint**: `GET /api/workspaces/{workspaceId}`
- **Response**: `200 OK` (Same format as Create Workspace response)

### 2.4 Get Workspace By Slug
- **Endpoint**: `GET /api/workspaces/slug/{slug}`
- **Response**: `200 OK` (Same format as Create Workspace response)

### 2.5 Update Workspace
- **Endpoint**: `PUT /api/workspaces/{workspaceId}`
- **Request Body**:
  ```json
  {
    "name": "Acme Corp Updated",
    "description": "Updated description"
  }
  ```
- **Response**: `200 OK` (Returns updated workspace)

### 2.6 Delete Workspace
- **Endpoint**: `DELETE /api/workspaces/{workspaceId}`
- **Response**: `204 No Content`

---

## 3. Workspace Members API

### 3.1 Get Workspace Members
- **Endpoint**: `GET /api/workspaces/{workspaceId}/members`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "323e4567-e89b-12d3-a456-426614174002",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "workspaceId": "223e4567-e89b-12d3-a456-426614174001",
      "role": "ADMIN",
      "userEmail": "john@example.com",
      "userName": "John Doe"
    }
  ]
  ```

### 3.2 Change Member Role
- **Endpoint**: `PUT /api/workspaces/{workspaceId}/members/{userId}/role`
- **Request Body**:
  ```json
  {
    "role": "MEMBER"
  }
  ```
- **Response**: `200 OK` (Returns updated WorkspaceMemberDto)

### 3.3 Remove Member
- **Endpoint**: `DELETE /api/workspaces/{workspaceId}/members/{userId}`
- **Response**: `204 No Content`

---

## 4. Workspace Invites API

### 4.1 Create Invite
- **Endpoint**: `POST /api/workspaces/{workspaceId}/invites`
- **Query Params**: `?inviterId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "email": "jane@example.com",
    "role": "MEMBER"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": "423e4567-e89b-12d3-a456-426614174003",
    "workspaceId": "223e4567-e89b-12d3-a456-426614174001",
    "inviterId": "123e4567-e89b-12d3-a456-426614174000",
    "inviteeEmail": "jane@example.com",
    "role": "MEMBER",
    "status": "PENDING"
  }
  ```

### 4.2 Get Pending Invites (Admin view)
- **Endpoint**: `GET /api/workspaces/{workspaceId}/invites`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "423e4567-e89b-12d3-a456-426614174003",
      "inviteeEmail": "jane@example.com",
      "status": "PENDING"
    }
  ]
  ```

### 4.3 Get My Pending Invites (User view)
- **Endpoint**: `GET /api/workspaces/invites/mine`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `200 OK` (List of WorkspaceInviteDto)

### 4.4 Accept Invite
- **Endpoint**: `POST /api/workspaces/invites/{inviteId}/accept`
- **Query Params**: `?acceptingUserId=<user-uuid>`
- **Response**: `200 OK` (Returns WorkspaceMemberDto)

### 4.5 Decline Invite
- **Endpoint**: `POST /api/workspaces/invites/{inviteId}/decline`
- **Query Params**: `?decliningUserId=<user-uuid>`
- **Response**: `204 No Content`

### 4.6 Revoke Invite
- **Endpoint**: `DELETE /api/workspaces/invites/{inviteId}`
- **Response**: `204 No Content`

### 4.7 Accept/Decline by Token (Email Links)
- **Endpoint**: `POST /api/workspaces/invites/accept-by-token` (`?token=...&acceptingUserId=...`)
- **Endpoint**: `POST /api/workspaces/invites/decline-by-token` (`?token=...`)

---

## 5. Projects API

### 5.1 Create Project
- **Endpoint**: `POST /api/workspaces/{workspaceSlug}/projects`
- **Query Params**: `?userId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "name": "Website Redesign",
    "slug": "website-redesign",
    "description": "Revamping the corporate website"
  }
  ```
- **Response**: `201 Created`
  ```json
  {
    "id": "523e4567-e89b-12d3-a456-426614174004",
    "workspaceId": "223e4567-e89b-12d3-a456-426614174001",
    "name": "Website Redesign",
    "slug": "website-redesign",
    "description": "Revamping the corporate website"
  }
  ```

### 5.2 Get Workspace Projects
- **Endpoint**: `GET /api/workspaces/{workspaceSlug}/projects`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `200 OK` (List of ProjectDto)

### 5.3 Get Project by Slug
- **Endpoint**: `GET /api/workspaces/{workspaceSlug}/projects/{projectSlug}`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `200 OK` (Returns ProjectDto)

### 5.4 Update Project
- **Endpoint**: `PATCH /api/workspaces/{workspaceSlug}/projects/{projectSlug}`
- **Query Params**: `?userId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "description": "Updated project description"
  }
  ```
- **Response**: `200 OK` (Returns updated ProjectDto)

### 5.5 Archive/Delete Project
- **Endpoint**: `DELETE /api/workspaces/{workspaceSlug}/projects/{projectSlug}`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `204 No Content`

---

## 6. Project Members API

### 6.1 Get Project Members
- **Endpoint**: `GET /api/projects/{projectSlug}/members`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "623e4567-e89b-12d3-a456-426614174005",
      "userId": "123e4567-e89b-12d3-a456-426614174000",
      "projectId": "523e4567-e89b-12d3-a456-426614174004",
      "roleId": "723e4567-e89b-12d3-a456-426614174006"
    }
  ]
  ```

### 6.2 Add Member
- **Endpoint**: `POST /api/projects/{projectSlug}/members`
- **Query Params**: `?userId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "userId": "111e4567-e89b-12d3-a456-426614174111",
    "roleId": "723e4567-e89b-12d3-a456-426614174006"
  }
  ```
- **Response**: `201 Created` (Returns ProjectMemberDto)

### 6.3 Change Member Role
- **Endpoint**: `PATCH /api/projects/{projectSlug}/members/{targetUserId}`
- **Query Params**: `?userId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "roleId": "new-role-uuid"
  }
  ```
- **Response**: `200 OK` (Returns ProjectMemberDto)

### 6.4 Remove Member
- **Endpoint**: `DELETE /api/projects/{projectSlug}/members/{targetUserId}`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `204 No Content`

---

## 7. Project Roles API

### 7.1 Get Project Roles
- **Endpoint**: `GET /api/projects/{projectSlug}/roles`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "723e4567-e89b-12d3-a456-426614174006",
      "projectId": "523e4567-e89b-12d3-a456-426614174004",
      "name": "Developer",
      "permissions": ["READ_TASK", "WRITE_TASK"]
    }
  ]
  ```

### 7.2 Create Role
- **Endpoint**: `POST /api/projects/{projectSlug}/roles`
- **Query Params**: `?userId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "name": "Viewer",
    "permissions": ["READ_TASK"]
  }
  ```
- **Response**: `201 Created` (Returns ProjectRoleDto)

### 7.3 Update Role
- **Endpoint**: `PATCH /api/projects/{projectSlug}/roles/{roleId}`
- **Query Params**: `?userId=<user-uuid>`
- **Request Body**:
  ```json
  {
    "permissions": ["READ_TASK", "COMMENT_TASK"]
  }
  ```
- **Response**: `200 OK` (Returns ProjectRoleDto)

### 7.4 Delete Role
- **Endpoint**: `DELETE /api/projects/{projectSlug}/roles/{roleId}`
- **Query Params**: `?userId=<user-uuid>`
- **Response**: `204 No Content`

---

## 8. Boards API

### 8.1 Get All Boards
- **Endpoint**: `GET /api/boards`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "823e4567-e89b-12d3-a456-426614174007",
      "name": "Sprint 1 Board",
      "projectId": "523e4567-e89b-12d3-a456-426614174004"
    }
  ]
  ```

### 8.2 Create Board
- **Endpoint**: `POST /api/boards`
- **Request Body**:
  ```json
  {
    "name": "Sprint 2 Board",
    "projectId": "523e4567-e89b-12d3-a456-426614174004"
  }
  ```
- **Response**: `201 Created` (Returns BoardDto)

### 8.3 Get Board by ID
- **Endpoint**: `GET /api/boards/{id}`
- **Response**: `200 OK` (Returns BoardDto)

### 8.4 Update Board
- **Endpoint**: `PUT /api/boards/{id}`
- **Request Body**:
  ```json
  {
    "name": "Sprint 1 Board - Updated"
  }
  ```
- **Response**: `200 OK` (Returns BoardDto)

### 8.5 Delete Board
- **Endpoint**: `DELETE /api/boards/{id}`
- **Response**: `204 No Content`

---

## 9. Todos API

### 9.1 Get All Todos
- **Endpoint**: `GET /api/todos`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "923e4567-e89b-12d3-a456-426614174008",
      "title": "Setup Frontend",
      "description": "Initialize React app with Vite",
      "status": "TODO",
      "boardId": "823e4567-e89b-12d3-a456-426614174007",
      "assigneeId": "123e4567-e89b-12d3-a456-426614174000"
    }
  ]
  ```

### 9.2 Create Todo
- **Endpoint**: `POST /api/todos`
- **Request Body**:
  ```json
  {
    "title": "Create Login Page",
    "description": "Implement authentication UI",
    "status": "TODO",
    "boardId": "823e4567-e89b-12d3-a456-426614174007"
  }
  ```
- **Response**: `201 Created` (Returns TodoDto)

### 9.3 Get Todo by ID
- **Endpoint**: `GET /api/todos/{id}`
- **Response**: `200 OK` (Returns TodoDto)

### 9.4 Update Todo
- **Endpoint**: `PUT /api/todos/{id}`
- **Request Body**:
  ```json
  {
    "title": "Setup Frontend",
    "status": "IN_PROGRESS"
  }
  ```
- **Response**: `200 OK` (Returns TodoDto)

### 9.5 Delete Todo
- **Endpoint**: `DELETE /api/todos/{id}`
- **Response**: `204 No Content`

---

## 10. Users API

### 10.1 Get All Users
- **Endpoint**: `GET /api/users`
- **Response**: `200 OK`
  ```json
  [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "John Doe",
      "email": "john@example.com"
    }
  ]
  ```

### 10.2 Create User
- **Endpoint**: `POST /api/users`
- **Request Body**:
  ```json
  {
    "name": "Jane Smith",
    "email": "jane@example.com"
  }
  ```
- **Response**: `201 Created` (Returns UserDto)

### 10.3 Get User by ID
- **Endpoint**: `GET /api/users/{id}`
- **Response**: `200 OK` (Returns UserDto)

### 10.4 Update User
- **Endpoint**: `PUT /api/users/{id}`
- **Request Body**:
  ```json
  {
    "name": "Johnathan Doe"
  }
  ```
- **Response**: `200 OK` (Returns UserDto)

### 10.5 Delete User
- **Endpoint**: `DELETE /api/users/{id}`
- **Response**: `204 No Content`
