---
name: Frontend Context Link
description: Contextual link and cross-repository rules between backend (todo-management) and frontend (todo-management-frontend).
always_on: true
---

# Frontend Context & Cross-Repo Rule

This backend repository (`todo-management`) serves the companion frontend repository located at:
`c:/Projects/Vijay/random/todo-management-frontend` (relative: `../todo-management-frontend`).

## Rules:
1. **No Merging**: Keep git repositories independent. Never commit frontend code into this repo or vice versa.
2. **Context Awareness**: Whenever creating or altering API endpoints, DTOs, or auth behavior:
   - Check `../todo-management-frontend/api_list.md` and `../todo-management-frontend/src/types/` for expected schemas.
   - Maintain JSON camelCase field naming.
   - Retain CORS compatibility with `http://localhost:5173`.
3. **Cross-Reference Documents**:
   - Frontend API List: `../todo-management-frontend/api_list.md`
   - Frontend Auth Specs: `../todo-management-frontend/AUTH_INTEGRATION.md`
   - Frontend Routing: `../todo-management-frontend/BR-Routing-Slugs.md`
   - Frontend KT Guide: `../todo-management-frontend/KNOWLEDGE_TRANSFER.md`
