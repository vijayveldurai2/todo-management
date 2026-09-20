# Implementation Plan: Auth `/me` Endpoint + Security Infrastructure

> **Status**: Partially implemented. Security infrastructure, login, logout, and Redis
> session allowlist are all complete. The only remaining item is `GET /api/auth/me`.

---

## Background

The frontend needs `GET /api/auth/me` on app startup to:
- Validate the stored JWT is still valid with the backend
- Hydrate the current user's profile into app state

---

## What's Already Done ✅

| Feature | File |
|---|---|
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter.java` |
| `SecurityFilterChain` (CSRF, CORS, whitelist) | `config/SecurityConfig.java` |
| `POST /api/auth/login` (with Redis session) | `AuthController`, `AuthServiceImpl` |
| `POST /api/auth/logout` | `AuthController`, `AuthServiceImpl` |
| `POST /api/auth/logout-all` | `AuthController`, `AuthServiceImpl` |
| Redis allowlist (`RedisSessionService`) | `security/RedisSessionService.java` |
| `CurrentUserProvider` helper | `security/CurrentUserProvider.java` |

---

## Remaining Work

### 1. `AuthService` interface
Add: `UserDto me(UUID userId)`

### 2. `AuthServiceImpl`
Implement `me()`:
- Fetch user by ID from `UserRepository`
- Throw `404` if not found or `isActive == false`
- Map to `UserDto` (reuse existing `mapToDto`)

### 3. `AuthController`
Add `GET /api/auth/me`:
- Read principal from `CurrentUserProvider.requireCurrentUserPrincipal()`
- Call `authService.me(principal.getId())`
- Return `200 UserDto`

### 4. `api_list.md`
Document the new endpoint under Auth API section.

---

## Verification

1. Login → get token
2. `GET /api/auth/me` with `Authorization: Bearer <token>` → `200 OK` + user profile
3. `GET /api/auth/me` with no token → `401 Unauthorized`
4. `GET /api/auth/me` after logout → `401 Unauthorized` (token invalidated in Redis)
