import express from 'express';
import path from 'path';
import { createServer as createViteServer } from 'vite';
import { backendStore } from './src/services/backendStore';

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(express.json());

  // --- API ROUTES ---

  // Health
  app.get('/api/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  // Workspaces
  app.get('/api/workspaces', (_req, res) => {
    res.json(backendStore.getWorkspaces());
  });

  app.get('/api/workspaces/:workspaceSlug', (req, res) => {
    const { workspaceSlug } = req.params;
    const workspace = backendStore.getWorkspaceBySlug(workspaceSlug);
    if (!workspace) {
      return res.status(404).json({ error: 'not_found', message: 'Workspace not found' });
    }
    res.json(workspace);
  });

  // Projects
  app.get('/api/workspaces/:workspaceSlug/projects', (req, res) => {
    const { workspaceSlug } = req.params;
    const workspace = backendStore.getWorkspaceBySlug(workspaceSlug);
    if (!workspace) {
      return res.status(404).json({ error: 'not_found', message: 'Workspace not found' });
    }
    res.json(backendStore.getProjects(workspace.id));
  });

  app.get('/api/workspaces/:workspaceSlug/projects/:projectSlug', (req, res) => {
    const { workspaceSlug, projectSlug } = req.params;
    const result = backendStore.getProjectBySlug(workspaceSlug, projectSlug);
    if (!result) {
      return res.status(404).json({ error: 'not_found', message: 'Project not found' });
    }
    res.json(result.project);
  });

  app.post('/api/workspaces/:workspaceSlug/projects', async (req, res) => {
    try {
      const { workspaceSlug } = req.params;
      const project = await backendStore.createProject(workspaceSlug, req.body);
      res.status(201).json(project);
    } catch (err: any) {
      res.status(400).json({ error: err.message });
    }
  });

  // Boards
  app.get('/api/workspaces/:workspaceSlug/projects/:projectSlug/boards', (req, res) => {
    const { workspaceSlug, projectSlug } = req.params;
    const projData = backendStore.getProjectBySlug(workspaceSlug, projectSlug);
    if (!projData) {
      return res.status(404).json({ error: 'not_found', message: 'Project not found' });
    }
    res.json(backendStore.getBoards(projData.project.id));
  });

  app.get('/api/workspaces/:workspaceSlug/projects/:projectSlug/boards/:boardSlug', (req, res) => {
    const { workspaceSlug, projectSlug, boardSlug } = req.params;
    const result = backendStore.getBoardBySlug(workspaceSlug, projectSlug, boardSlug);
    if (!result) {
      return res.status(404).json({ error: 'not_found', message: 'Board not found' });
    }
    res.json(result.board);
  });

  app.post('/api/workspaces/:workspaceSlug/projects/:projectSlug/boards', async (req, res) => {
    try {
      const { workspaceSlug, projectSlug } = req.params;
      const board = await backendStore.createBoard(workspaceSlug, projectSlug, req.body);
      res.status(201).json(board);
    } catch (err: any) {
      res.status(400).json({ error: err.message });
    }
  });

  // Validate Project Code Prefix
  app.get('/api/workspaces/:workspaceSlug/projects/check-prefix/:prefix', (req, res) => {
    const { workspaceSlug, prefix } = req.params;
    const available = backendStore.isPrefixAvailable(workspaceSlug, prefix);
    res.json({ available, prefix: prefix.toUpperCase().trim() });
  });

  // Todos / Tasks
  app.get('/api/workspaces/:workspaceSlug/projects/:projectSlug/todos/:displayId', (req, res) => {
    const { workspaceSlug, projectSlug, displayId } = req.params;
    const result = backendStore.getTaskByDisplayId(workspaceSlug, projectSlug, displayId);
    if (!result) {
      return res.status(404).json({ error: 'not_found', message: 'Todo not found' });
    }
    res.json(result.task);
  });

  app.get('/api/workspaces/:workspaceSlug/projects/:projectSlug/boards/:boardSlug/todos', (req, res) => {
    const { workspaceSlug, projectSlug, boardSlug } = req.params;
    const boardData = backendStore.getBoardBySlug(workspaceSlug, projectSlug, boardSlug);
    if (!boardData) {
      return res.status(404).json({ error: 'not_found', message: 'Board not found' });
    }
    res.json(backendStore.getTasks(boardData.project.id, boardData.board.id));
  });

  app.get('/api/workspaces/:workspaceSlug/projects/:projectSlug/boards/:boardSlug/todos/:todoSlug', (req, res) => {
    const { workspaceSlug, projectSlug, boardSlug, todoSlug } = req.params;
    const result = backendStore.getTaskBySlug(workspaceSlug, projectSlug, boardSlug, todoSlug);
    if (!result) {
      return res.status(404).json({ error: 'not_found', message: 'Todo not found' });
    }
    res.json(result.task);
  });

  app.post('/api/workspaces/:workspaceSlug/projects/:projectSlug/boards/:boardSlug/todos', async (req, res) => {
    try {
      const { workspaceSlug, projectSlug, boardSlug } = req.params;
      const todo = await backendStore.createTask(workspaceSlug, projectSlug, boardSlug, req.body);
      res.status(201).json(todo);
    } catch (err: any) {
      res.status(400).json({ error: err.message });
    }
  });

  app.patch('/api/todos/:todoId', (req, res) => {
    try {
      const { todoId } = req.params;
      const updated = backendStore.updateTask(todoId, req.body);
      res.json(updated);
    } catch (err: any) {
      res.status(404).json({ error: err.message });
    }
  });

  // Drag and Drop move endpoints
  app.patch('/api/cards/:id/move', (req, res) => {
    try {
      const { id } = req.params;
      const { newColumnId, newPosition } = req.body;
      const updated = backendStore.moveCard(id, newColumnId, newPosition);
      res.json(updated);
    } catch (err: any) {
      res.status(404).json({ error: err.message });
    }
  });

  app.patch('/api/columns/:id/move', (req, res) => {
    try {
      const { id } = req.params;
      const { newPosition } = req.body;
      const updated = backendStore.moveColumn(id, newPosition);
      res.json(updated);
    } catch (err: any) {
      res.status(404).json({ error: err.message });
    }
  });

  // Global / Fallback collections
  app.get('/api/columns', (_req, res) => {
    res.json(backendStore.getColumns());
  });
  app.get('/api/projects', (_req, res) => {
    res.json(backendStore.getProjects());
  });

  app.get('/api/boards', (_req, res) => {
    res.json(backendStore.getBoards());
  });

  app.get('/api/tasks', (_req, res) => {
    res.json(backendStore.getTasks());
  });

  // Vite Middleware in dev or static in prod
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Server running on http://localhost:${PORT}`);
  });
}

startServer();
