# Knowledge Transfer (KT) & Developer Documentation

Welcome to the project developer guide! This document provides an architectural overview, feature breakdown, reusable component guidelines, and a step-by-step guide on replacing the local mock store with a real production backend.

---

## 1. Executive Summary & Tech Stack

This application is a **Jira/Linear-style project management web application** built with React, TypeScript, Redux Toolkit, and Tailwind CSS. It supports multi-view task management, workspace-level hierarchy, keyboard shortcuts, and dark mode customization.

### Technology Stack
- **Framework & Build System**: React 18, Vite, TypeScript
- **State Management**: Redux Toolkit (`@reduxjs/toolkit`, `react-redux`)
- **Routing**: React Router v6 (`react-router-dom`)
- **Styling & UI**: Tailwind CSS, Google Material Symbols, Custom CSS variables
- **Drag & Drop**: `@hello-pangea/dnd`
- **Utilities**: `date-fns` for calendar/sprint date calculations

---

## 2. Directory & Architecture Overview

```text
src/
├── components/
│   ├── common/           # Shared UI elements (Header, Sidebar, Search)
│   ├── layouts/          # Top-level workspace layout shell
│   ├── modals/           # Modal dialogs (Create Task, Create Board, Shortcuts, Settings)
│   └── views/            # Main content views (Kanban, Sprint, TaskList, Calendar)
├── hooks/                # Custom React hooks (e.g. useKeyboardShortcuts)
├── services/             # API abstraction & data persistence layer
│   ├── apiService.ts     # Main API service interface used by UI components
│   ├── backendStore.ts   # In-memory + localStorage mock persistence engine
│   └── slugService.ts    # Slug generation & display ID formatting
├── store/                # Redux slices (tasks, projects, boards, UI, theme, auth)
├── types/                # Shared TypeScript interfaces & types
└── App.tsx               # Main routing entry point
```

---

## 3. Key Core Features

### 1. Multi-View Board Engine
- **Kanban Board (`KanbanBoardView.tsx`)**: Drag-and-drop workflow status columns (To Do, In Progress, Review, Done).
- **Sprint Board (`SprintBoardView.tsx`)**: Active sprint tracking with velocity metrics, story point totals, and progress indicators.
- **Task List View (`TaskListView.tsx`)**: Dense table view sorted by priority, status, assignee, and category.
- **Calendar View (`CalendarView.tsx`)**: Date-grid task distribution for visualizing upcoming deadlines.

### 2. Hierarchical Route Structure
Routes follow an intuitive human-readable hierarchy:
```text
/                                                -> Redirects to default workspace
/:workspaceSlug                                  -> Workspace overview
/:workspaceSlug/:projectSlug                     -> Project details
/:workspaceSlug/:projectSlug/:boardSlug          -> Active board view
/:workspaceSlug/:projectSlug/:boardSlug/todo/:id -> Task details modal/view
```

### 3. Global Keyboard Shortcuts System
Powered by `useKeyboardShortcuts.ts` and `KeyboardShortcutsModal.tsx`:
- `C` or `N`: Quick-create new task modal
- `P`: Create new project modal
- `B`: Create new board modal
- `1` / `K`: Switch to Kanban Board
- `2` / `T`: Switch to Task List
- `3` / `S`: Switch to Sprint Board
- `4` / `A`: Switch to Calendar View
- `0` / `W`: Navigate to Workspace Overview
- `?` or `Cmd+/`: Open Keyboard Shortcuts modal
- `Esc`: Close any open modal or panel

---

## 4. Backend Integration Guide (Replacing Mock Data)

The app is decoupled from the data layer using `apiService.ts`. All frontend components dispatch Redux async thunks or invoke `apiService` methods rather than directly reading local memory.

### Current Setup
- `backendStore.ts`: Simulates database CRUD operations, auto-generates task display IDs (e.g., `WEB-101`), and persists state to `localStorage`.
- `apiService.ts`: Wraps `backendStore` methods in asynchronous Promise resolutions to mimic real HTTP/REST API behavior.

### Steps to Connect Your Real Backend API

1. **Set Up Environment Variables**:
   Define your backend base URL in `.env.example` and `.env`:
   ```env
   VITE_API_BASE_URL=https://api.yourdomain.com/v1
   ```

2. **Modify `src/services/apiService.ts`**:
   Replace local `backendStore` method calls with `fetch` or `axios` instances. Example migration pattern:

   ```typescript
   // src/services/apiService.ts
   const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

   export const apiService = {
     // Fetch workspace by slug
     async getWorkspace(slug: string): Promise<Workspace> {
       const response = await fetch(`${BASE_URL}/workspaces/${slug}`);
       if (!response.ok) throw new Error('Workspace not found');
       return response.json();
     },

     // Create a new task
     async createTask(taskData: Partial<Task>): Promise<Task> {
       const response = await fetch(`${BASE_URL}/tasks`, {
         method: 'POST',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify(taskData),
       });
       if (!response.ok) throw new Error('Failed to create task');
       return response.json();
     },

     // Update task status (e.g. drag & drop move)
     async updateTaskStatus(taskId: string, status: TaskStatus): Promise<Task> {
       const response = await fetch(`${BASE_URL}/tasks/${taskId}/status`, {
         method: 'PATCH',
         headers: { 'Content-Type': 'application/json' },
         body: JSON.stringify({ status }),
       });
       if (!response.ok) throw new Error('Failed to update task status');
       return response.json();
     },
   };
   ```

3. **Data Schemas & Interfaces**:
   Ensure your backend response objects match the TypeScript definitions in `src/types/index.ts`:
   - `Task`: `id`, `display_id`, `title`, `description`, `status`, `priority`, `assignee`, `storyPoints`, `category`, `dueDate`, `project_id`, `board_id`
   - `Project`: `id`, `name`, `slug`, `key`, `description`, `workspace_id`
   - `Board`: `id`, `name`, `slug`, `type`, `project_id`

---

## 5. Reusable Component Reference

| Component | Path | Description | Props / Usage |
|---|---|---|---|
| **Header** | `src/components/common/Header.tsx` | Top navigation bar with search input, theme toggle, view switcher, and user profile | No required props; reads from Redux store |
| **Sidebar** | `src/components/common/Sidebar.tsx` | Workspace & project switcher, board list, and view navigation links | Accepts `workspaceSlug`, `projectSlug`, `boardSlug` |
| **CreateTaskModal** | `src/components/modals/CreateTaskModal.tsx` | Dialog for creating tasks with title, category, priority, points, and status | Controlled via Redux `isCreateTaskModalOpen` |
| **CreateBoardModal** | `src/components/modals/CreateBoardModal.tsx` | Modal to create Kanban, Sprint, or List boards under projects | Controlled via Redux `isCreateBoardModalOpen` |
| **KeyboardShortcutsModal** | `src/components/modals/KeyboardShortcutsModal.tsx` | Helper modal displaying active system and navigation shortcuts | Controlled via Redux `isShortcutsModalOpen` |
| **SettingsModal** | `src/components/modals/SettingsModal.tsx` | App preferences, color theme choices, and account info | Controlled via Redux `isSettingsModalOpen` |

---

## 6. Developer Workflow Commands

- `npm run dev`: Starts local Vite development server on port `3000`.
- `npm run lint`: Runs TypeScript type checks (`tsc --noEmit`).
- `npm run build`: Bundles production assets into `dist/`.

---

If you have any questions about component extension or API endpoint mapping, consult `src/types/index.ts` for type contracts or open `src/services/apiService.ts` to view the full backend interface list.
