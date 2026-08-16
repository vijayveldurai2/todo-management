import { Project, Board, Task, User, Workspace, Subtask, Comment, Column, SignupRequest, SignupResponse, LoginRequest, LoginResponse, VerifyResponse } from '../types';
import { backendStore } from './backendStore';

const BASE_URL = ((import.meta as any).env && (import.meta as any).env.VITE_API_BASE_URL) || '';

const fetchJson = async <T>(url: string, options?: RequestInit): Promise<T | null> => {
  try {
    const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;
    const res = await fetch(fullUrl, options);
    if (!res.ok) {
      if (res.status === 404) {
        throw new Error('not_found');
      }
      const errorData = await res.json().catch(() => ({}));
      const message = errorData.message || errorData.error || `HTTP ${res.status}`;
      throw new Error(message);
    }
    return await res.json();
  } catch (e: any) {
    if (e.message === 'not_found' || e.message?.startsWith('HTTP') || e.message) {
      // Re-throw API error messages
      throw e;
    }
    console.warn(`Fetch to ${url} failed, using local store fallback:`, e);
    return null;
  }
};

export const apiService = {
  // Auth API Endpoints
  async signup(data: SignupRequest): Promise<SignupResponse> {
    try {
      const res = await fetch(`${BASE_URL}/api/auth/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.message || errJson.error || `Signup failed with status ${res.status}`);
      }

      return await res.json();
    } catch (e: any) {
      // Re-throw genuine API errors (e.g. status code errors, explicit backend responses)
      if (e.message && !e.message.includes('Failed to fetch') && !e.message.includes('NetworkError')) {
        throw e;
      }
      if (BASE_URL) {
        throw e; // Hard failure when explicit backend URL is configured
      }

      console.warn('Backend server signup unreachable. Simulating signup response for preview mode:', e);
      return {
        message: 'Signup successful, please verify your email',
        email: data.email,
      };
    }
  },

  async loginApi(data: LoginRequest): Promise<LoginResponse> {
    try {
      const res = await fetch(`${BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.message || errJson.error || 'Invalid credentials or user not verified.');
      }

      const responseData: LoginResponse = await res.json();
      if (responseData.accessToken) {
        localStorage.setItem('auth_token', responseData.accessToken);
        localStorage.setItem('auth_user', JSON.stringify(responseData));
      }
      return responseData;
    } catch (e: any) {
      // Re-throw genuine API error responses (400, 401, 403, 500)
      if (e.message && !e.message.includes('Failed to fetch') && !e.message.includes('NetworkError')) {
        throw e;
      }
      if (BASE_URL) {
        throw e; // Hard failure when explicit backend URL is configured
      }

      console.warn('Backend server unreachable. Falling back to local store login for preview mode:', e);
      const isEmail = data.login.includes('@');
      const mockUsername = isEmail ? data.login.split('@')[0] : data.login;
      const mockUser: LoginResponse = {
        accessToken: `jwt_mock_token_${Date.now()}`,
        tokenType: 'Bearer',
        userId: `u-${Date.now()}`,
        email: isEmail ? data.login : `${data.login}@example.com`,
        username: mockUsername,
        role: 'USER',
      };
      localStorage.setItem('auth_token', mockUser.accessToken);
      localStorage.setItem('auth_user', JSON.stringify(mockUser));
      return mockUser;
    }
  },

  async verifyEmail(token: string): Promise<VerifyResponse> {
    try {
      const res = await fetch(`${BASE_URL}/api/auth/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      });

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.message || errJson.error || 'Verification failed. Invalid or expired token.');
      }

      return await res.json();
    } catch (e: any) {
      if (e.message && !e.message.includes('Failed to fetch') && !e.message.includes('NetworkError')) {
        throw e;
      }
      if (BASE_URL) {
        throw e;
      }

      console.warn('Backend verify unreachable. Simulating successful verification for preview mode:', e);
      return {
        message: 'Email verified successfully',
        userId: `u-${Date.now()}`,
        email: 'user@example.com',
        username: 'verified_user',
      };
    }
  },

  async getCurrentUser(): Promise<User | null> {
    const saved = localStorage.getItem('auth_user');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        return {
          id: parsed.userId || 'u-101',
          name: parsed.username || 'Vijay Kumar',
          email: parsed.email || 'vijaykumar.veldurai2@gmail.com',
          username: parsed.username,
          avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80',
          role: parsed.role || 'USER',
          accessToken: parsed.accessToken,
          provider: 'email',
        };
      } catch (err) {
        console.error('Failed to parse saved auth user:', err);
      }
    }

    return null;
  },

  async loginOAuth(provider: 'google' | 'github'): Promise<User> {
    return {
      id: `u-${Date.now()}`,
      name: provider === 'google' ? 'Vijay Kumar (Google)' : 'Vijay Kumar (GitHub)',
      email: 'vijaykumar.veldurai2@gmail.com',
      avatar: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDTOPP6Xfg9jOtaHJcbQZ0JqaIREWx-kV-vgYnDzdGK2hUfaE4vxqoAs79u0vJAyZYqn3eeQPCe-KxO9PLgoqoRYEtD7W2XOafyVmXh5mTTFqcHJ9FHruxk-Yij8tOx3xN9j1q7q-Fnuk4wNYczkIDad5kWKozaZW0g3_1wMDS7BcrmP2Q1Uy8vzX8XFRerFP_xJuE6E3tOGhmzhA3tSjMcHqVhc6gzvzzhpA-fUf-rnK2py5sk4y05uFBpyADjHnksJ-vzES0S5L0m',
      role: 'Product Lead',
      provider,
    };
  },

  async loginEmail(email: string): Promise<User> {
    const displayName = email.split('@')[0].replace(/[\._-]/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()) || 'User';
    return {
      id: `u-${Date.now()}`,
      name: displayName,
      email: email,
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80',
      role: 'Product Lead',
      provider: 'email',
    };
  },

  async logout(): Promise<void> {},

  async resetToDefaults(): Promise<void> {
    backendStore.resetToDefaults();
  },

  // Workspaces
  async getWorkspaces(): Promise<Workspace[]> {
    const user = await this.getCurrentUser();
    if (!user?.id) {
      return backendStore.getWorkspaces();
    }
    return this.getWorkspacesForUser(user.id);
  },

  async getWorkspacesForUser(userId: string): Promise<Workspace[]> {
    const apiRes = await fetchJson<Workspace[]>(`/api/workspaces?userId=${encodeURIComponent(userId)}`);
    return apiRes || backendStore.getWorkspaces();
  },

  async createWorkspace(data: { name: string; description?: string }, creatorId: string): Promise<Workspace> {
    const res = await fetch(`${BASE_URL}/api/workspaces?creatorId=${encodeURIComponent(creatorId)}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: data.name,
        description: data.description || '',
      }),
    });

    if (!res.ok) {
      const errJson = await res.json().catch(() => ({}));
      throw new Error(errJson.message || errJson.error || `Create workspace failed with status ${res.status}`);
    }

    const created = await res.json();
    if (created && created.slug) {
      return created;
    }

    return backendStore.createWorkspace(data.name, data.description);
  },

  async getWorkspaceBySlug(workspaceSlug: string): Promise<Workspace> {
    const apiRes = await fetchJson<Workspace>(`/api/workspaces/slug/${encodeURIComponent(workspaceSlug)}`);
    if (apiRes) return apiRes;
    const fallback = backendStore.getWorkspaceBySlug(workspaceSlug);
    if (!fallback) throw new Error('not_found');
    return fallback;
  },

  // Projects
  async getProjects(workspaceSlug: string = 'main-workspace'): Promise<Project[]> {
    const apiRes = await fetchJson<Project[]>(`/api/workspaces/${workspaceSlug}/projects`);
    return apiRes || backendStore.getProjects();
  },

  async getProjectBySlug(workspaceSlug: string, projectSlug: string): Promise<Project> {
    const apiRes = await fetchJson<Project>(`/api/workspaces/${workspaceSlug}/projects/${projectSlug}`);
    if (apiRes) return apiRes;
    const fallback = backendStore.getProjectBySlug(workspaceSlug, projectSlug);
    if (!fallback) throw new Error('not_found');
    return fallback.project;
  },

  async createProject(workspaceSlugOrData: string | Partial<Project>, optionalData?: Partial<Project>): Promise<Project> {
    const workspaceSlug = typeof workspaceSlugOrData === 'string' ? workspaceSlugOrData : 'main-workspace';
    const projectData = typeof workspaceSlugOrData === 'string' ? (optionalData || {}) : workspaceSlugOrData;

    try {
      const res = await fetch(`/api/workspaces/${workspaceSlug}/projects`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(projectData),
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (e) {
      console.warn('API createProject failed, using backendStore:', e);
    }
    return backendStore.createProject(workspaceSlug, projectData);
  },

  // Boards
  async getBoards(workspaceSlugOrProjectId?: string, projectSlug?: string): Promise<Board[]> {
    if (workspaceSlugOrProjectId && projectSlug) {
      const apiRes = await fetchJson<Board[]>(`/api/workspaces/${workspaceSlugOrProjectId}/projects/${projectSlug}/boards`);
      if (apiRes) return apiRes;
      const projData = backendStore.getProjectBySlug(workspaceSlugOrProjectId, projectSlug);
      return projData ? backendStore.getBoards(projData.project.id) : backendStore.getBoards();
    }
    return backendStore.getBoards(workspaceSlugOrProjectId);
  },

  async getBoardBySlug(workspaceSlug: string, projectSlug: string, boardSlug: string): Promise<Board> {
    const apiRes = await fetchJson<Board>(`/api/workspaces/${workspaceSlug}/projects/${projectSlug}/boards/${boardSlug}`);
    if (apiRes) return apiRes;
    const fallback = backendStore.getBoardBySlug(workspaceSlug, projectSlug, boardSlug);
    if (!fallback) throw new Error('not_found');
    return fallback.board;
  },

  async createBoard(
    workspaceSlugOrBoardData: string | Partial<Board>,
    projectSlugOrOptionalBoardData?: string | Partial<Board>,
    optionalBoardData?: Partial<Board>
  ): Promise<Board> {
    let workspaceSlug = 'main-workspace';
    let projectSlug = 'brand-refresh';
    let boardData: Partial<Board> = {};

    if (typeof workspaceSlugOrBoardData === 'string' && typeof projectSlugOrOptionalBoardData === 'string') {
      workspaceSlug = workspaceSlugOrBoardData;
      projectSlug = projectSlugOrOptionalBoardData;
      boardData = optionalBoardData || {};
    } else if (typeof workspaceSlugOrBoardData === 'object') {
      boardData = workspaceSlugOrBoardData;
    } else if (typeof projectSlugOrOptionalBoardData === 'object') {
      boardData = projectSlugOrOptionalBoardData;
    }

    try {
      const res = await fetch(`/api/workspaces/${workspaceSlug}/projects/${projectSlug}/boards`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(boardData),
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (e) {
      console.warn('API createBoard failed, using backendStore:', e);
    }
    return backendStore.createBoard(workspaceSlug, projectSlug, boardData);
  },

  // Todos / Tasks
  async getTasks(workspaceSlugOrProjectId?: string, projectSlugOrBoardId?: string, boardSlug?: string): Promise<Task[]> {
    if (workspaceSlugOrProjectId && projectSlugOrBoardId && boardSlug) {
      const apiRes = await fetchJson<Task[]>(
        `/api/workspaces/${workspaceSlugOrProjectId}/projects/${projectSlugOrBoardId}/boards/${boardSlug}/todos`
      );
      if (apiRes) return apiRes;
      const boardData = backendStore.getBoardBySlug(workspaceSlugOrProjectId, projectSlugOrBoardId, boardSlug);
      return boardData ? backendStore.getTasks(boardData.project.id, boardData.board.id) : backendStore.getTasks();
    }
    return backendStore.getTasks(workspaceSlugOrProjectId, projectSlugOrBoardId);
  },

  async getTaskBySlug(workspaceSlug: string, projectSlug: string, boardSlug: string, todoSlug: string): Promise<Task> {
    const apiRes = await fetchJson<Task>(
      `/api/workspaces/${workspaceSlug}/projects/${projectSlug}/boards/${boardSlug}/todos/${todoSlug}`
    );
    if (apiRes) return apiRes;
    const fallback = backendStore.getTaskBySlug(workspaceSlug, projectSlug, boardSlug, todoSlug);
    if (!fallback) throw new Error('not_found');
    return fallback.task;
  },

  async getTaskByDisplayId(workspaceSlug: string, projectSlug: string, displayId: string): Promise<Task> {
    const apiRes = await fetchJson<Task>(
      `/api/workspaces/${workspaceSlug}/projects/${projectSlug}/todos/${displayId}`
    );
    if (apiRes) return apiRes;
    const fallback = backendStore.getTaskByDisplayId(workspaceSlug, projectSlug, displayId);
    if (!fallback) throw new Error('not_found');
    return fallback.task;
  },

  async createTask(
    workspaceSlugOrTaskData: string | Partial<Task>,
    projectSlugOrOptionalTaskData?: string | Partial<Task>,
    boardSlug?: string,
    optionalTaskData?: Partial<Task>
  ): Promise<Task> {
    let wsSlug = 'main-workspace';
    let projSlug = 'brand-refresh';
    let bSlug = 'visual-identity';
    let taskData: Partial<Task> = {};

    if (typeof workspaceSlugOrTaskData === 'string' && typeof projectSlugOrOptionalTaskData === 'string' && boardSlug) {
      wsSlug = workspaceSlugOrTaskData;
      projSlug = projectSlugOrOptionalTaskData;
      bSlug = boardSlug;
      taskData = optionalTaskData || {};
    } else if (typeof workspaceSlugOrTaskData === 'object') {
      taskData = workspaceSlugOrTaskData;
    }

    try {
      const res = await fetch(`/api/workspaces/${wsSlug}/projects/${projSlug}/boards/${bSlug}/todos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(taskData),
      });
      if (res.ok) {
        return await res.json();
      }
    } catch (e) {
      console.warn('API createTask failed, using backendStore:', e);
    }
    return backendStore.createTask(wsSlug, projSlug, bSlug, taskData);
  },

  // Columns & Drag-and-Drop
  async getColumns(boardId?: string): Promise<Column[]> {
    const apiRes = await fetchJson<Column[]>('/api/columns');
    return apiRes || backendStore.getColumns(boardId);
  },

  async moveCard(cardId: string, newColumnId: string | undefined, newPosition: number): Promise<Task> {
    try {
      const res = await fetch(`/api/cards/${cardId}/move`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newColumnId, newPosition }),
      });
      if (res.ok) return await res.json();
    } catch (e) {
      console.warn('API moveCard failed, using backendStore:', e);
    }
    return backendStore.moveCard(cardId, newColumnId, newPosition);
  },

  async moveColumn(columnId: string, newPosition: number): Promise<Column> {
    try {
      const res = await fetch(`/api/columns/${columnId}/move`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newPosition }),
      });
      if (res.ok) return await res.json();
    } catch (e) {
      console.warn('API moveColumn failed, using backendStore:', e);
    }
    return backendStore.moveColumn(columnId, newPosition);
  },

  async updateTask(taskId: string, updates: Partial<Task>): Promise<Task> {
    try {
      const res = await fetch(`/api/todos/${taskId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(updates),
      });
      if (res.ok) return await res.json();
    } catch (e) {
      console.warn('API updateTask failed:', e);
    }
    return backendStore.updateTask(taskId, updates);
  },

  async toggleSubtask(taskId: string, subtaskId: string): Promise<Task> {
    const tasks = backendStore.getTasks();
    const task = tasks.find((t) => t.id === taskId);
    if (!task) throw new Error('Task not found');
    const updatedSubtasks = task.subtasks.map((st) => (st.id === subtaskId ? { ...st, completed: !st.completed } : st));
    return this.updateTask(taskId, { subtasks: updatedSubtasks });
  },

  async addSubtask(taskId: string, title: string): Promise<Task> {
    const tasks = backendStore.getTasks();
    const task = tasks.find((t) => t.id === taskId);
    if (!task) throw new Error('Task not found');
    const newSt: Subtask = { id: `st-${Date.now()}`, title, completed: false };
    return this.updateTask(taskId, { subtasks: [...task.subtasks, newSt] });
  },

  async addComment(taskId: string, content: string, user: User): Promise<Task> {
    const tasks = backendStore.getTasks();
    const task = tasks.find((t) => t.id === taskId);
    if (!task) throw new Error('Task not found');
    const newComment: Comment = {
      id: `c-${Date.now()}`,
      authorName: user.name,
      authorAvatar: user.avatar,
      content,
      createdAt: 'Just now',
    };
    return this.updateTask(taskId, { comments: [...task.comments, newComment] });
  },
};
