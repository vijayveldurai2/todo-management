import React, { useEffect } from 'react';
import { Provider } from 'react-redux';
import { createBrowserRouter, RouterProvider, Outlet } from 'react-router-dom';
import { store, useAppDispatch, useAppSelector } from './store';
import { fetchCurrentUser } from './store/authSlice';
import { fetchProjects } from './store/projectsSlice';
import { fetchTasks } from './store/tasksSlice';

import { WorkspacePicker } from './components/views/WorkspacePicker';
import { WorkspaceLayout } from './components/layouts/WorkspaceLayout';
import { WorkspaceView } from './components/views/WorkspaceView';
import { ProjectLayout } from './components/layouts/ProjectLayout';
import { ProjectDetailsView } from './components/views/ProjectDetailsView';
import { BoardView } from './components/views/BoardView';
import { TodoDetailPanel } from './components/modals/TodoDetailPanel';
import { NotFoundPage } from './components/views/NotFoundPage';
import { LoginView } from './components/views/LoginView';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { TaskListView } from './components/views/TaskListView';
import { KanbanBoardView } from './components/views/KanbanBoardView';
import { SprintBoardView } from './components/views/SprintBoardView';
import { CalendarView } from './components/views/CalendarView';

const TaskListRoute = () => (
  <div className="relative">
    <TaskListView />
    <Outlet />
  </div>
);

const KanbanRoute = () => (
  <div className="relative">
    <KanbanBoardView />
    <Outlet />
  </div>
);

const SprintRoute = () => (
  <div className="relative">
    <SprintBoardView />
    <Outlet />
  </div>
);

const CalendarRoute = () => (
  <div className="relative">
    <CalendarView />
    <Outlet />
  </div>
);

const router = createBrowserRouter([
  // Public Authentication Handles
  { path: '/login', element: <LoginView initialMode="login" /> },
  { path: '/signup', element: <LoginView initialMode="signup" /> },

  // Protected Application Handles
  {
    element: <ProtectedRoute />,
    children: [
      { path: '/', element: <WorkspacePicker /> },
      {
        path: '/workspace',
        element: <WorkspaceLayout />,
        children: [{ index: true, element: <WorkspaceView /> }],
      },
      {
        path: '/projects',
        element: <WorkspaceLayout />,
        children: [{ index: true, element: <WorkspaceView /> }],
      },
      {
        path: '/tasks',
        element: <WorkspaceLayout />,
        children: [
          {
            path: '',
            element: <TaskListRoute />,
            children: [{ path: 'todo/:todoSlug', element: <TodoDetailPanel /> }],
          },
        ],
      },
      {
        path: '/kanban',
        element: <WorkspaceLayout />,
        children: [
          {
            path: '',
            element: <KanbanRoute />,
            children: [{ path: 'todo/:todoSlug', element: <TodoDetailPanel /> }],
          },
        ],
      },
      {
        path: '/sprint',
        element: <WorkspaceLayout />,
        children: [
          {
            path: '',
            element: <SprintRoute />,
            children: [{ path: 'todo/:todoSlug', element: <TodoDetailPanel /> }],
          },
        ],
      },
      {
        path: '/calendar',
        element: <WorkspaceLayout />,
        children: [
          {
            path: '',
            element: <CalendarRoute />,
            children: [{ path: 'todo/:todoSlug', element: <TodoDetailPanel /> }],
          },
        ],
      },
      {
        path: '/:workspaceSlug',
        element: <WorkspaceLayout />,
        children: [
          { index: true, element: <WorkspaceView /> },
          {
            path: ':projectSlug',
            element: <ProjectLayout />,
            children: [
              { index: true, element: <ProjectDetailsView /> },
              // Standalone deep link — resolves via project + displayId alone, no board needed in path
              { path: 'todo/:todoDisplayId', element: <TodoDetailPanel /> },
              { path: 'todo/:todoSlug', element: <TodoDetailPanel /> },
              {
                path: ':boardSlug',
                element: <BoardView />,
                children: [
                  { path: 'todo/:todoDisplayId', element: <TodoDetailPanel /> },
                  { path: 'todo/:todoSlug', element: <TodoDetailPanel /> },
                ],
              },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);

const AppInitializer: React.FC = () => {
  const dispatch = useAppDispatch();
  const { mode, colorTheme } = useAppSelector((state) => state.theme);

  // Sync theme classes to HTML document root
  useEffect(() => {
    const root = document.documentElement;

    if (mode === 'dark') {
      root.classList.add('dark');
      root.classList.remove('light');
    } else {
      root.classList.add('light');
      root.classList.remove('dark');
    }

    const colorClasses = ['theme-indigo', 'theme-emerald', 'theme-rose', 'theme-amber', 'theme-cyan', 'theme-violet'];
    root.classList.remove(...colorClasses);
    root.classList.add(`theme-${colorTheme}`);
  }, [mode, colorTheme]);

  // Initial load
  useEffect(() => {
    dispatch(fetchCurrentUser());
    dispatch(fetchProjects());
    dispatch(fetchTasks(undefined));
  }, [dispatch]);

  return <RouterProvider router={router} />;
};

export function App() {
  return (
    <Provider store={store}>
      <AppInitializer />
    </Provider>
  );
}

export default App;
