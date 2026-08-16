import { Workspace, Project, Board, Task, Column } from '../types';
import { INITIAL_WORKSPACES, INITIAL_PROJECTS, INITIAL_BOARDS, INITIAL_TASKS, TABLE_TASKS } from '../data/mockData';
import { createUniqueSlug, generateBaseSlug, getRandomShortSuffix } from './slugService';

export function generateSuggestedPrefix(name: string): string {
  if (!name || !name.trim()) return 'PR';
  const clean = name.trim().replace(/[^a-zA-Z0-9\s]/g, '');
  const words = clean.split(/\s+/).filter(Boolean);
  let code = '';
  if (words.length >= 2) {
    code = words.map((w) => w[0]).join('').toUpperCase();
  } else if (words.length === 1) {
    const w = words[0].toUpperCase();
    code = w.length >= 2 ? w.slice(0, 2) : w + 'X';
  }
  code = code.replace(/[^A-Z0-9]/g, '');
  if (code.length < 2) code = (code + 'PR').slice(0, 2);
  return code.slice(0, 6);
}

// Backend memory/in-process data store initialized with default data with guaranteed unique slugs

let workspaces: Workspace[] = [...INITIAL_WORKSPACES];

let projects: Project[] = INITIAL_PROJECTS.map((p) => ({
  ...p,
  workspaceId: p.workspaceId || 'ws-1',
  prefix: p.prefix || generateSuggestedPrefix(p.name),
  todo_counter: p.todo_counter || 100,
  slug: p.slug || generateBaseSlug(p.name),
}));

let boards: Board[] = INITIAL_BOARDS.map((b) => ({
  ...b,
  slug: b.slug || generateBaseSlug(b.title),
}));

let columns: Column[] = [
  { id: 'col-todo', board_id: 'board-1', name: 'To Do', position: 1000, is_locked: true, colorHex: '#64748b' },
  { id: 'col-in-progress', board_id: 'board-1', name: 'In Progress', position: 2000, is_locked: false, colorHex: '#3b82f6' },
  { id: 'col-in-review', board_id: 'board-1', name: 'In Review', position: 3000, is_locked: false, colorHex: '#f59e0b' },
  { id: 'col-done', board_id: 'board-1', name: 'Done', position: 4000, is_locked: true, colorHex: '#10b981' },
];

let tasks: Task[] = [...INITIAL_TASKS, ...TABLE_TASKS].map((t, idx) => {
  let colId = 'col-todo';
  if (t.status === 'In Progress') colId = 'col-in-progress';
  else if (t.status === 'In Review') colId = 'col-in-review';
  else if (t.status === 'Done' || t.status === 'Completed') colId = 'col-done';

  const proj = projects.find((p) => p.id === t.projectId) || projects[0];
  const seq = t.sequence_number || (100 + idx + 1);
  const display_id = t.display_id || `${proj.prefix}-${seq}`;

  return {
    ...t,
    sequence_number: seq,
    display_id,
    slug: display_id,
    column_id: t.column_id || colId,
    position: t.position !== undefined ? t.position : (idx + 1) * 1000,
  };
});

export const backendStore = {
  // WORKSPACES
  getWorkspaces(): Workspace[] {
    return workspaces;
  },

  getWorkspaceBySlug(slug: string): Workspace | undefined {
    return workspaces.find((w) => w.slug === slug);
  },

  async createWorkspace(name: string, description?: string): Promise<Workspace> {
    const slug = await createUniqueSlug(name, (s) =>
      workspaces.some((w) => w.slug === s)
    );
    const newWs: Workspace = {
      id: `ws-${Date.now()}`,
      name,
      slug,
      description: description || '',
    };
    workspaces.push(newWs);
    return newWs;
  },

  // PROJECTS
  getProjects(workspaceId?: string): Project[] {
    if (workspaceId) {
      return projects.filter((p) => p.workspaceId === workspaceId);
    }
    return projects;
  },

  getProjectBySlug(workspaceSlug: string, projectSlug: string): { workspace: Workspace; project: Project } | null {
    const workspace = this.getWorkspaceBySlug(workspaceSlug);
    if (!workspace) return null;
    const project = projects.find(
      (p) => (p.workspaceId === workspace.id || workspaceSlug === 'main-workspace') && p.slug === projectSlug
    );
    if (!project) return null;
    return { workspace, project };
  },

  async createProject(workspaceSlug: string, newProjectData: Partial<Project>): Promise<Project> {
    const workspace = this.getWorkspaceBySlug(workspaceSlug) || workspaces[0];
    const name = newProjectData.name || 'Untitled Project';

    const rawPrefix = newProjectData.prefix || generateSuggestedPrefix(name);
    const prefix = rawPrefix.toUpperCase().trim().replace(/[^A-Z0-9]/g, '');

    if (!prefix || prefix.length < 2 || prefix.length > 6) {
      throw new Error('Project Code must be between 2 and 6 uppercase alphanumeric characters.');
    }

    // Check prefix uniqueness per workspace
    const isPrefixTaken = projects.some(
      (p) => (p.workspaceId === workspace.id || workspaceSlug === 'main-workspace') && p.prefix?.toUpperCase() === prefix
    );
    if (isPrefixTaken) {
      throw new Error(`${prefix} is already used by another project`);
    }

    const slug = await createUniqueSlug(name, (s) =>
      projects.some((p) => p.workspaceId === workspace.id && p.slug === s)
    );

    const newProject: Project = {
      id: `proj-${Date.now()}`,
      workspaceId: workspace.id,
      name,
      prefix,
      todo_counter: 0,
      slug,
      category: newProjectData.category || 'GENERAL',
      description: newProjectData.description || '',
      completedTasks: 0,
      totalTasks: 0,
      progressPercentage: 0,
      color: newProjectData.color || '#3525cd',
      icon: newProjectData.icon || 'folder',
      template: newProjectData.template || 'Empty Project',
      updatedAt: 'Just now',
      contributors: newProjectData.contributors || [
        {
          id: 'u-1',
          name: 'Vijay Kumar',
          email: 'vijaykumar.veldurai2@gmail.com',
          role: 'PM',
          avatar: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDTOPP6Xfg9jOtaHJcbQZ0JqaIREWx-kV-vgYnDzdGK2hUfaE4vxqoAs79u0vJAyZYqn3eeQPCe-KxO9PLgoqoRYEtD7W2XOafyVmXh5mTTFqcHJ9FHruxk-Yij8tOx3xN9j1q7q-Fnuk4wNYczkIDad5kWKozaZW0g3_1wMDS7BcrmP2Q1Uy8vzX8XFRerFP_xJuE6E3tOGhmzhA3tSjMcHqVhc6gzvzzhpA-fUf-rnK2py5sk4y05uFBpyADjHnksJ-vzES0S5L0m',
        },
      ],
    };

    projects.unshift(newProject);
    return newProject;
  },

  isPrefixAvailable(workspaceSlug: string, prefix: string): boolean {
    const workspace = this.getWorkspaceBySlug(workspaceSlug) || workspaces[0];
    const clean = prefix.toUpperCase().trim();
    if (!clean || clean.length < 2 || clean.length > 6) return false;
    return !projects.some(
      (p) => (p.workspaceId === workspace.id || workspaceSlug === 'main-workspace') && p.prefix?.toUpperCase() === clean
    );
  },

  // BOARDS
  getBoards(projectId?: string): Board[] {
    if (projectId) {
      return boards.filter((b) => b.projectId === projectId);
    }
    return boards;
  },

  getBoardBySlug(workspaceSlug: string, projectSlug: string, boardSlug: string): { workspace: Workspace; project: Project; board: Board } | null {
    const projData = this.getProjectBySlug(workspaceSlug, projectSlug);
    if (!projData) return null;
    const { workspace, project } = projData;
    const board = boards.find((b) => b.projectId === project.id && b.slug === boardSlug);
    if (!board) return null;
    return { workspace, project, board };
  },

  async createBoard(workspaceSlug: string, projectSlug: string, boardData: Partial<Board>): Promise<Board> {
    const projData = this.getProjectBySlug(workspaceSlug, projectSlug);
    if (!projData) throw new Error('Project not found');
    const { project } = projData;

    const title = boardData.title || boardData.sprintName || 'New Board';
    const slug = await createUniqueSlug(
      title,
      (s) => boards.some((b) => b.projectId === project.id && b.slug === s)
    );

    const newBoard: Board = {
      id: `board-${Date.now()}`,
      projectId: project.id,
      title,
      slug,
      icon: boardData.icon || (boardData.type === 'Sprint' ? 'directions_run' : 'view_kanban'),
      type: boardData.type || 'Kanban',
      sprintName: boardData.sprintName,
      startDate: boardData.startDate,
      endDate: boardData.endDate,
      goal: boardData.goal,
      status: boardData.status || 'Active',
      cardsCount: 0,
      updatedAt: 'Just now',
      contributors: [{ id: 'u-1' }],
    };

    boards.push(newBoard);
    return newBoard;
  },

  // TODOS / TASKS
  getTasks(projectId?: string, boardId?: string): Task[] {
    let res = tasks;
    if (projectId) res = res.filter((t) => t.projectId === projectId);
    if (boardId) res = res.filter((t) => t.boardId === boardId || t.sprintId === boardId);
    return res;
  },

  getTaskByDisplayId(workspaceSlug: string, projectSlug: string, displayId: string): { workspace: Workspace; project: Project; board?: Board; task: Task } | null {
    const projData = this.getProjectBySlug(workspaceSlug, projectSlug);
    if (!projData) return null;
    const { workspace, project } = projData;

    const target = displayId.toLowerCase();
    const task = tasks.find(
      (t) => t.projectId === project.id && (
        t.display_id?.toLowerCase() === target ||
        t.slug?.toLowerCase() === target ||
        t.id.toLowerCase() === target
      )
    );
    if (!task) return null;

    const board = boards.find((b) => b.id === task.boardId || b.id === task.sprintId) || boards.find((b) => b.projectId === project.id);
    return { workspace, project, board, task };
  },

  getTaskBySlug(workspaceSlug: string, projectSlug: string, boardSlug: string, todoSlug: string): { workspace: Workspace; project: Project; board: Board; task: Task } | null {
    const boardData = this.getBoardBySlug(workspaceSlug, projectSlug, boardSlug);
    if (!boardData) return null;
    const { workspace, project, board } = boardData;
    const target = todoSlug.toLowerCase();
    const task = tasks.find(
      (t) => (t.boardId === board.id || t.sprintId === board.id) && (
        t.display_id?.toLowerCase() === target ||
        t.slug?.toLowerCase() === target ||
        t.id.toLowerCase() === target
      )
    );
    if (!task) return null;
    return { workspace, project, board, task };
  },

  async createTask(workspaceSlug: string, projectSlug: string, boardSlug: string, taskData: Partial<Task>): Promise<Task> {
    const boardData = this.getBoardBySlug(workspaceSlug, projectSlug, boardSlug);
    if (!boardData) throw new Error('Board not found');
    const { project, board } = boardData;

    // Atomic increment project's todo_counter
    project.todo_counter = (project.todo_counter || 0) + 1;
    const sequenceNumber = project.todo_counter;
    const prefix = project.prefix || generateSuggestedPrefix(project.name);
    const displayId = `${prefix}-${sequenceNumber}`;

    const title = taskData.title || 'New Task';

    const newTask: Task = {
      id: `task-${Date.now()}`,
      projectId: project.id,
      boardId: board.id,
      sprintId: board.type === 'Sprint' ? board.id : undefined,
      sequence_number: sequenceNumber,
      display_id: displayId,
      title,
      slug: displayId,
      category: taskData.category || 'General',
      description: taskData.description || '',
      status: taskData.status || 'To Do',
      priority: taskData.priority || 'Medium',
      dueDate: taskData.dueDate || 'Today',
      storyPoints: taskData.storyPoints,
      assignees: taskData.assignees || [{ id: 'u-1', name: 'Vijay Kumar', avatar: '' }],
      subtasks: taskData.subtasks || [],
      attachments: taskData.attachments || [],
      comments: taskData.comments || [],
    };

    tasks.unshift(newTask);
    return newTask;
  },

  // COLUMNS
  getColumns(boardId?: string): Column[] {
    let res = columns;
    if (boardId) {
      res = res.filter((c) => !c.board_id || c.board_id === boardId);
    }
    return [...res].sort((a, b) => a.position - b.position);
  },

  moveColumn(columnId: string, newPosition: number): Column {
    const col = columns.find((c) => c.id === columnId);
    if (!col) throw new Error('Column not found');
    col.position = newPosition;
    columns.sort((a, b) => a.position - b.position);
    return col;
  },

  moveCard(cardId: string, newColumnId?: string, newPosition?: number): Task {
    const taskIndex = tasks.findIndex((t) => t.id === cardId || t.slug === cardId);
    if (taskIndex === -1) throw new Error('Card not found');

    const task = tasks[taskIndex];
    if (newColumnId) {
      const targetCol = columns.find((c) => c.id === newColumnId || c.name === newColumnId);
      task.column_id = targetCol ? targetCol.id : newColumnId;
      if (targetCol) {
        task.status = targetCol.name as any;
      }
    }
    if (newPosition !== undefined) {
      task.position = newPosition;
    }

    return task;
  },

  updateTask(taskId: string, updates: Partial<Task>): Task {
    const index = tasks.findIndex((t) => t.id === taskId || t.slug === taskId);
    if (index !== -1) {
      tasks[index] = { ...tasks[index], ...updates };
      return tasks[index];
    }
    throw new Error('Task not found');
  },

  resetToDefaults(): void {
    workspaces = [...INITIAL_WORKSPACES];
    projects = INITIAL_PROJECTS.map((p) => ({
      ...p,
      workspaceId: p.workspaceId || 'ws-1',
      slug: p.slug || generateBaseSlug(p.name),
    }));
    boards = INITIAL_BOARDS.map((b) => ({
      ...b,
      slug: b.slug || generateBaseSlug(b.title),
    }));
    columns = [
      { id: 'col-todo', board_id: 'board-1', name: 'To Do', position: 1000, is_locked: true, colorHex: '#64748b' },
      { id: 'col-in-progress', board_id: 'board-1', name: 'In Progress', position: 2000, is_locked: false, colorHex: '#3b82f6' },
      { id: 'col-in-review', board_id: 'board-1', name: 'In Review', position: 3000, is_locked: false, colorHex: '#f59e0b' },
      { id: 'col-done', board_id: 'board-1', name: 'Done', position: 4000, is_locked: true, colorHex: '#10b981' },
    ];
    tasks = [...INITIAL_TASKS, ...TABLE_TASKS].map((t, idx) => {
      let colId = 'col-todo';
      if (t.status === 'In Progress') colId = 'col-in-progress';
      else if (t.status === 'In Review') colId = 'col-in-review';
      else if (t.status === 'Done' || t.status === 'Completed') colId = 'col-done';

      return {
        ...t,
        slug: t.slug || `${generateBaseSlug(t.title)}-${getRandomShortSuffix()}`,
        column_id: t.column_id || colId,
        position: t.position !== undefined ? t.position : (idx + 1) * 1000,
      };
    });
  },
};
