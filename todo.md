# Todo List & Action Items

## Change Password Functionality

### 1. Overview
Allow authenticated users to securely update their password from the application settings/profile view. Changing the password requires verification of the user's current password, validation of the new password against security policies, and session invalidation across other active devices.

---

### 2. Backend Implementation (`todo-management`)

#### API Contract
- **Endpoint**: `POST /api/auth/change-password`
- **Security**: Requires Bearer JWT token (`Authorization: Bearer <jwt>`)
- **Request Body**:
  ```json
  {
    "currentPassword": "OldSecurePassword123!",
    "newPassword": "NewSecurePassword456@",
    "confirmPassword": "NewSecurePassword456@"
  }
  ```
- **Responses**:
  - `200 OK`:
    ```json
    {
      "message": "Password changed successfully."
    }
    ```
  - `400 Bad Request`:
    - Current password incorrect (`"Current password does not match"`)
    - New password identical to old password (`"New password cannot be the same as the current password"`)
    - Password mismatch (`"New password and confirmation do not match"`)
    - Password policy violation (`"Password must be at least 8 characters long and include numbers and special characters"`)
  - `401 Unauthorized`: Missing or invalid JWT token.

#### Backend Tasks Checklist
- [ ] **DTO**:
  - [ ] Create `ChangePasswordRequest` with `@NotBlank`, `@Size(min = 8)`, and validation annotations.
- [ ] **Service Layer (`AuthService` / `AuthServiceImpl`)**:
  - [ ] Add `void changePassword(UUID userId, ChangePasswordRequest request)`.
  - [ ] Retrieve authenticated `User` by `userId`.
  - [ ] Verify `currentPassword` against `user.getPassword()` using `passwordEncoder.matches()`.
  - [ ] Verify `newPassword` != `currentPassword`.
  - [ ] Verify `newPassword` == `confirmPassword`.
  - [ ] Hash `newPassword` using `passwordEncoder.encode()`.
  - [ ] Update `user.setPassword(...)` and persist via `UserRepository`.
  - [ ] Invalidate user's other sessions / refresh tokens via `redisSessionService`.
- [ ] **Controller Layer (`AuthController`)**:
  - [ ] Add `@PostMapping("/change-password")` handling `ChangePasswordRequest`.
  - [ ] Extract user ID using `CurrentUserProvider.requireCurrentUserPrincipal().getId()`.
- [ ] **Documentation**:
  - [ ] Document endpoint in `api_list.md` under `## 1. Auth API`.
- [ ] **Automated Testing**:
  - [ ] Unit tests in `AuthServiceImplTest`:
    - Successful password change.
    - Rejection on invalid current password.
    - Rejection when new password equals current password.
    - Rejection when new and confirmation passwords differ.
  - [ ] Controller test in `AuthControllerTest` / mockMvc with authorized and unauthorized headers.

---

### 3. Frontend Implementation (`todo-management-frontend`)

- [ ] **RTK Query API Layer**:
  - [ ] Add `changePassword` mutation in `src/services/api.ts` (or `src/features/auth/authApi.ts`).
- [ ] **UI Component**:
  - [ ] Add Change Password form in Settings / Account view.
  - [ ] Inputs: Current Password, New Password, Confirm New Password with toggleable visibility.
  - [ ] Client-side validation: minimum length, password confirmation matching.
  - [ ] Success notification toast upon completion.
  - [ ] Inline error banner displaying backend validation errors (e.g. incorrect current password).
- [ ] **Session Handling**:
  - [ ] Handle re-authentication or token refresh if active sessions are invalidated upon password change.

---

### 4. Cross-Repository Synchronization
- [ ] Update `../todo-management-frontend/api_list.md` to keep the API contract synchronized.
- [ ] Update `docs/BACKLOG.md` status once implementation begins.
