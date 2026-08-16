export type TaskPriority = 'Low' | 'Medium' | 'High' | 'Urgent';

export type TaskStatus = 'To Do' | 'In Progress' | 'In Review' | 'Done' | 'Pending' | 'Overdue' | 'Completed';

export interface User {
  id: string;
  name: string;
  email: string;
  username?: string;
  avatar: string;
  role?: string;
  provider?: 'google' | 'github' | 'email';
  accessToken?: string;
  expiresAt?: number;
}

export interface SignupRequest {
  email: string;
  username: string;
  password: string;
  name: string;
}

export interface SignupResponse {
  message: string;
  email: string;
}

export interface LoginRequest {
  login: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  userId: string;
  email: string;
  username: string;
  role?: string;
}

export interface VerifyResponse {
  message: string;
  userId: string;
  email: string;
  username: string;
}

export interface Subtask {
  id: string;
  title: string;
  completed: boolean;
}

export interface Attachment {
  id: string;
  fileName: string;
  fileSize: string;
  fileType?: string;
  url: string;
  updatedAt: string;
}

export interface Comment {
  id: string;
  authorName: string;
  authorAvatar: string;
  content: string;
  createdAt: string;
}

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  description?: string;
  avatar?: string;
}

export interface Column {
  id: string;
  board_id?: string;
  name: string;
  position: number;
  is_locked?: boolean;
  colorHex?: string;
}

export interface Task {
  id: string;
  projectId: string;
  boardId?: string;
  sprintId?: string;
  column_id?: string;
  position?: number;
  storyPoints?: number;
  display_id?: string;
  sequence_number?: number;
  title: string;
  slug?: string;
  category: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string; // e.g., "2024-10-31" or "Oct 31, 2023"
  assignees: {
    id: string;
    name: string;
    avatar: string;
    initials?: string;
    role?: string;
    email?: string;
  }[];
  subtasks: Subtask[];
  attachments: Attachment[];
  comments: Comment[];
}

export type BoardType = 'Kanban' | 'List' | 'Calendar' | 'Sprint';

export interface Board {
  id: string;
  projectId: string;
  title: string;
  slug?: string;
  icon: string;
  type?: BoardType;
  sprintName?: string;
  startDate?: string;
  endDate?: string;
  goal?: string;
  status?: 'Active' | 'Completed' | 'Future';
  cardsCount: number;
  updatedAt: string;
  contributors: {
    id: string;
    avatar?: string;
    initials?: string;
  }[];
}

export interface Project {
  id: string;
  workspaceId?: string;
  name: string;
  prefix?: string;
  todo_counter?: number;
  slug?: string;
  category: string; // e.g. "DESIGN", "ENGINEERING", "OPERATIONS", "MARKETING"
  description: string;
  completedTasks: number;
  totalTasks: number;
  progressPercentage: number;
  contributors: {
    id: string;
    name: string;
    avatar?: string;
    initials?: string;
    role?: string;
    email?: string;
  }[];
  color: string;
  icon: string;
  template?: string;
  updatedAt: string;
}

export type ThemeColor = 'indigo' | 'emerald' | 'rose' | 'amber' | 'cyan' | 'violet';
export type ThemeMode = 'light' | 'dark';

export type ActiveView = 'workspace' | 'projects' | 'board' | 'list' | 'calendar' | 'sprint' | 'my-tasks' | 'recent' | 'settings' | 'login';
